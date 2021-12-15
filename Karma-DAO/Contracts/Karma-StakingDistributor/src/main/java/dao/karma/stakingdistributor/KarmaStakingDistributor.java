/*
 * Copyright 2021 Karma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dao.karma.stakingdistributor;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import dao.karma.interfaces.dao.ITreasury;
import dao.karma.interfaces.irc2.IIRC2;
import dao.karma.types.IKarmaAccessControlled;
import dao.karma.types.KarmaAccessControlled;
import dao.karma.utils.Array256;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;

public class KarmaStakingDistributor implements IKarmaAccessControlled {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaStakingDistributor";

    // Contract name
    private final String name;

    // Implements KarmaAccessControlled
    private final KarmaAccessControlled accessControlled;

    private final Address KARMA;
    private final Address treasury;
    private final Address staking;

    private final BigInteger rateDenominator = BigInteger.valueOf(1_000_000);

    // ================================================
    // DB Variables
    // ================================================
    public final DictDB<BigInteger, Adjust> adjustments = Context.newDictDB(NAME + "_adjustments", Adjust.class);
    public final Array256<Info> info = new Array256<>(NAME + "_info", Info.class);

    // ================================================
    // Event Logs
    // ================================================

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaStakingDistributor (
        Address treasury,
        Address KARMA,
        Address staking, 
        Address authority
    ) {
        Context.require(!KARMA.equals(ZERO_ADDRESS));
        Context.require(!staking.equals(ZERO_ADDRESS));
        Context.require(!authority.equals(ZERO_ADDRESS));

        this.accessControlled = new KarmaAccessControlled(NAME + "_accessControlled", authority);
        this.name = "Karma Staking Distributor";
        this.KARMA = KARMA;
        this.treasury = treasury;
        this.staking = staking;
    }

    // --- Public functions ---
    /**
     * Send epoch reward to staking contract
     * 
     * Access : Staking
     */
    @External
    public void distribute () {
        // Access control
        onlyStaking();

        // OK
        // distribute rewards to each recipient
        BigInteger length = info.size();
        for (BigInteger i = ZERO; i.compareTo(length) < 0; i = i.add(ONE)) {
            var info = this.info.get(i);
            if (info.rate.compareTo(ZERO) > 0) {
                // mint and send from treasury
                ITreasury.mint(treasury, info.recipient, nextRewardAt(info.rate));
                // check for adjustment
                adjust(i);
            }
        }
    }

    // --- Internal functions ---
    private void adjust (BigInteger index) {
        var adjustment = this.adjustments.get(index);
        var info = this.info.get(index);

        if (!adjustment.rate.equals(ZERO)) {

            // if rate should increase
            if (adjustment.add) {
                // raise rate
                info.rate = info.rate.add(adjustment.rate);

                // if target met
                if (info.rate.compareTo(adjustment.rate) >= 0) {
                    // turn off adjustment
                    adjustment.rate = ZERO;
                    // set to target
                    info.rate = adjustment.target;
                }
            } else {
                // if rate should decrease

                // protect from underflow
                if (info.rate.compareTo(adjustment.rate) > 0) {
                    // lower rate
                    info.rate = info.rate.subtract(adjustment.rate);
                } else {
                    info.rate = ZERO;
                }

                // if target met
                if (info.rate.compareTo(adjustment.target) <= 0) {
                    // turn off adjustment
                    adjustment.rate = ZERO;
                    // set to target
                    info.rate = adjustment.target;
                }
            }
        }

        this.info.set(index, info);
        this.adjustments.set(index, adjustment);
    }

    // --- View functions ---
    @External(readonly = true)
    public BigInteger nextRewardAt (BigInteger rate) {
        return IIRC2.totalSupply(this.KARMA).multiply(rate).divide(rateDenominator);
    }

    @External(readonly = true)
    public BigInteger nextRewardFor (Address recipient) {
        BigInteger reward = ZERO;
        
        BigInteger length = info.size();
        for (BigInteger i = ZERO; i.compareTo(length) < 0; i = i.add(ONE)) {
            var info = this.info.get(i);
            reward = reward.add(nextRewardAt(info.rate));
        }

        return reward;
    }

    // --- Policy Functions ---

    /**
     * Adds recipient for distributions
     * 
     * Access: Governor
     * 
     * @param recipient
     * @param rewardRate
     */
    @External
    public void addRecipient (Address recipient, BigInteger rewardRate) {
        // Access control
        this.accessControlled.onlyGovernor();

        Context.require(!recipient.equals(ZERO_ADDRESS));
        Context.require(rewardRate.compareTo(rateDenominator) <= 0,
            "addRecipient: Rate cannot exceed denominator");

        // OK
        info.push(new Info(rewardRate, recipient));
    }

    /**
     * Removes recipient for distributions
     * 
     * Access: Governor or Guardian
     * 
     * @param index
     */
    @External
    public void removeRecipient (BigInteger index) {
        // Access control
        this.accessControlled.onlyGovernorOrGuardian();
        var info = this.info.getOrDefault(index, Info.empty());

        Context.require(!info.recipient.equals(ZERO_ADDRESS), 
            "removeRecipient: Recipient does not exist");

        // OK
        // delete
        this.info.set(index, null);
    }

    /**
     * Set adjustment info for a collector's reward rate
     * 
     * Access: Governor or Guardian
     * 
     * @param index
     */
    @External
    public void setAdjustment (
        BigInteger index,
        boolean add,
        BigInteger rate,
        BigInteger target
    ) {
        // Access control
        this.accessControlled.onlyGovernorOrGuardian();
        var info = this.info.getOrDefault(index, Info.empty());
        
        Context.require(!info.recipient.equals(ZERO_ADDRESS), 
            "removeRecipient: Recipient does not exist");

        // OK
        final Address caller = Context.getCaller();
        final Address guardian = this.accessControlled.guardian();

        if (caller.equals(guardian)) {
            Context.require(rate.compareTo(info.rate.multiply(BigInteger.valueOf(25)).divide(BigInteger.valueOf(1000))) <= 0,
                "setAdjustment: cannot adjust by > 2.5%");
        }

        if (!add) {
            Context.require(rate.compareTo(info.rate) <= 0,
                "setAdjustment: Cannot decrease rate by more than it already is");
        }

        this.adjustments.set(index, new Adjust(add, rate, target));
    }

    // ================================================
    // Checks
    // ================================================
    private void onlyStaking () {
        final Address caller = Context.getCaller();
        Context.require(caller.equals(this.staking),
            "onlyStaking: Only Staking contract can call this method");
    }

    // ================================================
    // Public variable getters
    // ================================================
    /**
     * Get the contract name
     */
    @External(readonly = true)
    public String name() {
        return this.name;
    }

    @External(readonly = true)
    public Adjust adjustments (BigInteger index) {
        return this.adjustments.get(index);
    }

    @External(readonly = true)
    public Info info (BigInteger index) {
        return this.info.get(index);
    }

    // --- Implement IKarmaAccessControlled ---
    // ================================================
    // Event Logs
    // ================================================
    @Override
    @EventLog(indexed = 1)
    public void AuthorityUpdated(Address authority) {}

    // ================================================
    // Methods
    // ================================================
    @Override
    @External
    public void setAuthority(Address newAuthority) {
        this.accessControlled.setAuthority(newAuthority);
    }
}
