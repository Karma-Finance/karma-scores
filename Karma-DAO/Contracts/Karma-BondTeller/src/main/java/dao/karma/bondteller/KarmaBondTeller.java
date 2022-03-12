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

package dao.karma.bondteller;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.JsonObject;

import dao.karma.interfaces.dao.ITreasury;
import dao.karma.interfaces.dao.IStaking;
import dao.karma.interfaces.dao.IsKARMA;
import dao.karma.interfaces.irc2.IIRC2;
import dao.karma.types.IKarmaAccessControlled;
import dao.karma.types.KarmaAccessControlled;
import dao.karma.utils.Array256;
import dao.karma.utils.JSONUtils;
import dao.karma.utils.MathUtils;
import dao.karma.utils.StringUtils;
import dao.karma.utils.TimeUtils;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class KarmaBondTeller implements IKarmaAccessControlled {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaBondTeller";

    // Contract name
    private final String name;

    // Implements KarmaAccessControlled
    private final KarmaAccessControlled accessControlled;

    // contract where users deposit bonds
    private final Address depository;
    // contract to stake payout
    private final Address staking;
    private final Address treasury;
    private final Address KARMA;
    // payment token
    private final Address sKARMA;

    // ================================================
    // DB Variables
    // ================================================
    // user data
    private Array256<Bond> bonderInfo (Address user) {
        return new Array256<>(NAME + "_bonderInfo_" + user, Bond.class);
    } 
    // user bond indexes
    private Array256<BigInteger> indexesFor (Address user) {
        return new Array256<>(NAME + "_indexesFor_" + user, BigInteger.class);
    } 
    // front end operator rewards
    private final DictDB<Address, BigInteger> FERs = Context.newDictDB(NAME + "_FERs", BigInteger.class);
    private final VarDB<BigInteger> feReward = Context.newVarDB(NAME + "_feReward", BigInteger.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog(indexed = 1)
    public void BondCreated(Address bonder, BigInteger payout, long expires) {}

    @EventLog(indexed = 1)
    public void Redeemed(Address bonder, BigInteger payout) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public KarmaBondTeller (
        Address depository,
        Address staking,
        Address treasury,
        Address KARMA,
        Address sKARMA,
        Address authority
    ) {
        Context.require(!depository.equals(ZERO_ADDRESS), "Zero address: Depository");
        Context.require(!staking.equals(ZERO_ADDRESS), "Zero address: Staking");
        Context.require(!treasury.equals(ZERO_ADDRESS), "Zero address: Treasury");
        Context.require(!KARMA.equals(ZERO_ADDRESS), "Zero address: KARMA");
        Context.require(!sKARMA.equals(ZERO_ADDRESS), "Zero address: sKARMA");

        this.accessControlled = new KarmaAccessControlled(NAME + "_accessControlled", authority);
        this.name = "Karma Bond Teller";

        this.depository = depository;
        this.staking = staking;
        this.treasury = treasury;
        this.KARMA = KARMA;
        this.sKARMA = sKARMA;
    }

    // --- Depository functions ---
    /**
     * Add new bond payout to user data
     * 
     * Access: Depository
     * 
     * @param bonder
     * @param principal
     * @param principalPaid
     * @param payout
     * @param expires
     */
    // @External - external through tokenFallback
    private BigInteger newBond (
        Address caller,
        Address token,
        Address bonder,
        Address principal,
        BigInteger principalPaid,
        BigInteger payout, // KARMA amount sent to newBond
        long expires,
        Address frontEndOperator
    ) {
        // Access control
        onlyDepository();
        // Only Karma token can be used for bond
        onlyKarma(token);

        // OK
        final Address thisAddress = Context.getAddress();

        BigInteger reward = payout.multiply(this.feReward.get()).divide(BigInteger.valueOf(10_000));
        ITreasury.mint(treasury, thisAddress, payout.add(reward));

        // Send KARMA to staking
        IStaking.stake(this.staking, this.KARMA, payout, true, true);

        // front end operator reward
        this.FERs.set(frontEndOperator, this.FERs.get(frontEndOperator).add(reward));

        var bonderInfo = this.bonderInfo(bonder);
        BigInteger index = bonderInfo.size();

        // store bond & stake payout
        bonderInfo.push(
            new Bond(
                principal, 
                principalPaid, 
                IsKARMA.toG(sKARMA, payout), 
                expires, 
                TimeUtils.now(), 
                BigInteger.ZERO
            )
        );

        return index;
    }

    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) {
        JsonObject root = JSONUtils.parseData(_data);
        String method = root.get("method").asString();
        Address token = Context.getCaller();

        switch (method)
        {
            /**
             * BigInteger newBond (
             *  Address caller,
             *  Address token,
             *  Address bonder,
             *  Address principal,
             *  BigInteger principalPaid,
             *  BigInteger payout, // KARMA amount sent to newBond
             *  long expires,
             *  Address frontEndOperator
             * ) 
             */
            case "newBond": {
                BigInteger payout = _value;
                JsonObject params = root.get("params").asObject();

                Address bonder = Address.fromString(params.get("bonder").asString());
                Address principal = Address.fromString(params.get("principal").asString());
                BigInteger principalPaid = StringUtils.toBigInt(params.get("principalPaid").asString());
                long expires = params.get("expires").asLong();
                Address frontEndOperator = Address.fromString(params.get("frontEndOperator").asString());

                newBond(_from, token, bonder, principal, principalPaid, payout, expires, frontEndOperator);
                break;
            }

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
    }

    // --- Interactable functions ---
    /**
     * Redeems all redeemable bonds
     * 
     * Access: Everyone
     * 
     * @param bonder
     */
    @External
    public BigInteger redeemAll (Address bonder) {
        updateIndexesFor(bonder);
        return redeem(bonder, this.indexesFor(bonder).toArray());
    }

    /**
     * Redeem bond for user
     * 
     * Access: Everyone
     * 
     * @param bonder
     * @param indexes
     */
    @External
    public BigInteger redeem (Address bonder, BigInteger[] indexes) {
        BigInteger dues = ZERO;
        var bonderInfo = this.bonderInfo(bonder);

        for (int i = 0; i < indexes.length; i++) {
            BigInteger index = indexes[i];
            Bond info = bonderInfo.get(index);

            if (!pendingFor(bonder, index).equals(ZERO)) {
                // mark as redeemed
                info.redeemed = TimeUtils.now(); 
                bonderInfo.set(index, info);
                dues = dues.add(info.payout);
            }
        }

        dues = IsKARMA.fromG(sKARMA, dues);

        this.Redeemed(bonder, dues);
        pay(bonder, dues);
        return dues;
    }

    /**
     * Pay reward to front end operator
     */
    @External
    public void getReward() {
        final Address caller = Context.getCaller();
        BigInteger reward = this.FERs.get(caller);

        Context.require(reward.compareTo(ZERO) > 0,
            "getReward: No reward available");

        this.FERs.set(caller, ZERO);
        IIRC2.transfer(KARMA, caller, reward, JSONUtils.method("feoReward"));
    }

    // --- Ownable functions ---
    /**
     * Send payout
     * @param amount Amount of sKARMA to send
     */
    private void pay (Address bonder, BigInteger amount) {
        IIRC2.transfer(sKARMA, bonder, amount, JSONUtils.method("pay"));
    }

    /**
     * Returns indexes of live bonds
     * 
     * @param bonder
     */
    @External()
    public void updateIndexesFor (Address bonder) {
        var info = this.bonderInfo(bonder);
        var indexesFor = indexesFor(bonder);
        indexesFor.delete();
        BigInteger infoSize = info.size();

        for (BigInteger i = ZERO; i.compareTo(infoSize) < 0; i = i.add(ONE)) {
            if (info.get(i).redeemed.equals(ZERO)) {
                indexesFor.push(i);
            }
        }
    }

    // --- Payout ---
    /**
     * Calculate amount of OHM available for claim for single bond
     * 
     * @param bonder
     * @param index
     */
    @External(readonly = true)
    public BigInteger pendingFor (Address bonder, BigInteger index) {
        var bonderInfo = this.bonderInfo(bonder);
        long blockHeight = Context.getBlockHeight();
        var info = bonderInfo.get(index);

        if (info.redeemed.equals(ZERO) && info.vested <= blockHeight) {
            return info.payout;
        }

        return ZERO;
    }

    /**
     * Calculate amount of OHM available for claim for array of bonds
     * 
     * @param bonder
     * @param indexes
     */
    @External(readonly = true)
    public BigInteger pendingForIndexes (Address bonder, BigInteger[] indexes) {
        BigInteger pending = ZERO;

        for (int i = 0; i < indexes.length; i++) {
            pending = pending.add(pendingFor(bonder, BigInteger.valueOf(i)));
        }

        return IsKARMA.fromG(this.sKARMA, pending);
    }

    /**
     * Total pending on all bonds for bonder
     * 
     * @param bonder address
     */
    @External(readonly = true)
    public BigInteger totalPendingFor (Address bonder) {
        var info = this.bonderInfo(bonder);

        BigInteger infoSize = info.size();
        BigInteger pending = ZERO;

        for (BigInteger i = ZERO; i.compareTo(infoSize) < 0; i = i.add(ONE)) {
            pending = pending.add(pendingFor(bonder, i));
        }

        return IsKARMA.fromG(this.sKARMA, pending);
    }

    // --- Vesting ---
    /**
     * Calculate how far into vesting a depositor is
     * 
     * @param bonder
     * @param index
     */
    @External(readonly = true)
    public BigInteger percentVestedFor(Address bonder, BigInteger index) {
        var bond = this.bonderInfo(bonder).get(index);

        BigInteger timeSince = TimeUtils.now().subtract(bond.created);
        BigInteger term = BigInteger.valueOf(bond.vested).subtract(bond.created);

        return timeSince.multiply(MathUtils.pow10(9)).divide(term);
    }

    // ================================================
    // Checks
    // ================================================
    private void onlyDepository () {
        final Address caller = Context.getCaller();
        Context.require(caller.equals(this.depository),
            "onlyDepository: Only Depository can call this method");
    }

    private void onlyKarma (Address token) {
        Context.require(token.equals(this.KARMA),
            "onlyKarma: Only Karma token is accepted");
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
