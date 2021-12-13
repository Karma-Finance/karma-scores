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

package dao.karma.staking;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.JsonObject;

import dao.karma.interfaces.dao.IDistributor;
import dao.karma.interfaces.dao.IgKARMA;
import dao.karma.interfaces.dao.IsKARMA;
import dao.karma.interfaces.irc2.IIRC2;
import dao.karma.structs.dao.Claim;
import dao.karma.structs.dao.Epoch;
import dao.karma.types.IKarmaAccessControlled;
import dao.karma.types.KarmaAccessControlled;
import dao.karma.utils.JSONUtils;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class KarmaStaking implements IKarmaAccessControlled {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaStaking";

    // Contract name
    private final String name;

    // ================================================
    // DB Variables
    // ================================================
    public final Address KARMA;
    public final Address sKARMA;
    public final Address gKARMA;
    public final Epoch epoch;

    public final VarDB<Address> distributor = Context.newVarDB(NAME + "_distributor", Address.class);
    public final DictDB<Address, Claim> warmupInfo = Context.newDictDB(NAME + "_warmupInfo", Claim.class);

    public final VarDB<BigInteger> warmupPeriod = Context.newVarDB(NAME + "_warmupPeriod", BigInteger.class);
    private final VarDB<BigInteger> gonsInWarmup = Context.newVarDB(NAME + "_gonsInWarmup", BigInteger.class);

    // Implements KarmaAccessControlled
    private final KarmaAccessControlled accessControlled;

    // ================================================
    // Event Logs
    // ================================================
    @EventLog
    public void DistributorSet (Address distributor) {}

    @EventLog
    public void WarmupSet (BigInteger warmup) {}

    @EventLog
    private void Wrapped (Address to, BigInteger gBalance) {}

    @EventLog
    private void Unwrapped (Address to, BigInteger sBalance) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaStaking (
        Address KARMA,
        Address sKARMA,
        Address gKARMA,
        Long epochLength,
        BigInteger firstEpochNumber,
        Long firstEpochBlock,
        Address authority
    ) {
        Context.require(!KARMA.equals(ZERO_ADDRESS));
        Context.require(!sKARMA.equals(ZERO_ADDRESS));
        Context.require(!gKARMA.equals(ZERO_ADDRESS));

        this.accessControlled = new KarmaAccessControlled(NAME + "_accessControlled", authority);

        this.name = "Karma Staking";
        this.KARMA = KARMA;
        this.sKARMA = sKARMA;
        this.gKARMA = gKARMA;

        this.epoch = new Epoch(
            epochLength, 
            firstEpochNumber, 
            firstEpochBlock, 
            BigInteger.ZERO
        );
    }

    // --- Mutative functions ---
    /**
     * Stake KARMA to enter warmup
     * 
     * Access: Everyone
     * 
     * @param to
     * @param amount
     * @param rebasing
     * @param claim
     * @return
     */
    // @External - through tokenFallback
    private BigInteger stake (
        Address caller,
        Address token,
        Address to,
        BigInteger amount, // amount of token sent
        boolean rebasing,
        boolean claim
    ) {
        onlyKarma(token);
        
        Context.require(token.equals(this.KARMA),
            "stake: only KARMA can be staked");

        rebase();

        var warmupPeriod = this.warmupPeriod.get();

        if (claim && warmupPeriod.equals(ZERO)) {
            return _send(to, amount, rebasing);
        } else {
            var info = warmupInfo.getOrDefault(to, Claim.empty());

            if (!info.lock) {
                Context.require(to.equals(caller), 
                    "stake: External deposits for account are locked");
            }

            this.warmupInfo.set(to,
                new Claim (
                    info.deposit.add(amount),
                    info.gons.add(IsKARMA.gonsForBalance(this.sKARMA, amount)),
                    epoch.number.add(warmupPeriod),
                    info.lock
                )
            );

            this.gonsInWarmup.set(this.gonsInWarmup.get().add(IsKARMA.gonsForBalance(this.sKARMA, amount)));

            return amount;
        }
    }

    /**
     * Retrieve stake from warmup
     * 
     * Access: Everyone
     * 
     * @param to
     * @param rebasing
     */
    @External
    public BigInteger claim (Address to, boolean rebasing) {
        var info = warmupInfo.getOrDefault(to, Claim.empty());
        final Address caller = Context.getCaller();

        if (!info.lock) {
            Context.require(to.equals(caller));
        }

        if (epoch.number.compareTo(info.expiry) >= 0 && !info.expiry.equals(ZERO)) {
            this.warmupInfo.set(to, null);

            this.gonsInWarmup.set(this.gonsInWarmup.get().subtract(info.gons));

            return _send(to, IsKARMA.balanceForGons(this.sKARMA, info.gons), rebasing);
        }

        return ZERO;
    }

    /**
     * Forfeit stake and retrieve OHM
     * 
     * Access: Everyone
     */
    @External
    public BigInteger forfeit () {
        final Address caller = Context.getCaller();
        var info = this.warmupInfo.getOrDefault(caller, Claim.empty());
        this.warmupInfo.set(caller, null); // delete

        this.gonsInWarmup.set(this.gonsInWarmup.get().subtract(info.gons));

        IIRC2.transfer(this.KARMA, caller, info.deposit, "forfeit".getBytes());

        return info.deposit;
    }

    /**
     * Prevent new deposits or claims from external address (protection from malicious activity)
     */
    @External
    public void toggleLock () {
        final Address caller = Context.getCaller();
        var info = this.warmupInfo.getOrDefault(caller, Claim.empty());
        info.lock = !info.lock;
        this.warmupInfo.set(caller, info);
    }

    /**
     * Redeem sKARMA or gKARMA for KARMA
     */
    // @External - through tokenFallback
    private void unstake (
        Address caller,
        Address token, // should be sKARMA or gKARMA
        Address to,
        BigInteger amount, // amount of sKARMA sent
        boolean trigger,
        boolean rebasing // true: sKARMA, false: gKARMA
    ) {
        if (rebasing) {
            onlyStakedKarma(token);
        } else {
            onlyGovernanceKarma(token);
        }

        if (trigger) {
            rebase();
        }

        BigInteger result = amount;

        if (rebasing) {
            // Nothing to do, sKARMA already received
        } else {
            // amount was given in gKARMA terms
            IgKARMA.burn(this.gKARMA, amount);

            // convert amount to KARMA terms
            result = IgKARMA.balanceFrom(this.gKARMA, amount);
        }

        // Send KARMA back
        IIRC2.transfer(this.KARMA, to, result, "unstake".getBytes());
    }

    /**
     * Convert {@code amount} sKARMA into {@code gBalance} gKARMA
     * 
     * @param caller caller address
     * @param token sKARMA address
     * @param to destination address
     * @param amount amount of sKARMA to convert to gKARMA
     */
    // @External - through tokenFallback
    private void wrap (
        Address caller,
        Address token, // should be sKARMA
        Address to,
        BigInteger amount
    ) {
        // Check
        onlyStakedKarma(token);

        // OK
        BigInteger gBalance = IgKARMA.balanceTo(this.gKARMA, amount);

        // Convert to gKARMA
        IgKARMA.mint(this.gKARMA, to, gBalance);

        this.Wrapped(to, gBalance);
    }

    /**
     * Convert {@code amount} gKARMA into {@code sBalance} sKARMA
     * 
     * @param caller caller address
     * @param token sKARMA address
     * @param to destination address
     * @param amount amount of sKARMA to convert to gKARMA
     */
    // @External - through tokenFallback
    private void unwrap (
        Address caller,
        Address token, // should be gKARMA
        Address to,
        BigInteger amount
    ) {
        // Check
        onlyGovernanceKarma(token);

        // OK
        IgKARMA.burn(this.gKARMA, amount);

        BigInteger sBalance = IgKARMA.balanceFrom(this.gKARMA, amount);

        // Convert to sKARMA
        IIRC2.transfer(this.sKARMA, to, sBalance, "unwrap".getBytes());

        this.Unwrapped(to, sBalance);
    }

    /**
     * Trigger rebase if epoch over
     */
    @External
    public void rebase () {
        long blockHeight = Context.getBlockHeight();
        var distributor = this.distributor.get();

        if (epoch.endBlock <= blockHeight) {
            IsKARMA.rebase(this.sKARMA, epoch.distribute, epoch.number);

            epoch.endBlock = epoch.endBlock + epoch.length;
            epoch.number = epoch.number.add(ONE);

            if (distributor != null) {
                IDistributor.distribute(distributor);
            }

            var contractBalance = this.contractBalance();
            var totalStaked = this.totalStaked();

            if (contractBalance.compareTo(totalStaked) <= 0) {
                epoch.distribute = ZERO;
            } else {
                epoch.distribute = contractBalance.subtract(totalStaked);
            }
        }
    }
    
    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) {
        JsonObject root = JSONUtils.parseData(_data);
        String method = root.get("method").asString();
        final Address token = Context.getCaller();
        final Address caller = _from;

        switch (method)
        {
            case "stake": {
                /**
                 * BigInteger stake (
                 *     Address caller,
                 *     Address token,
                 *     Address to,
                 *     BigInteger amount, // amount of token sent
                 *     boolean rebasing,
                 *     boolean claim
                 * )
                 */
                JsonObject params = root.get("params").asObject();
                Address to = Address.fromString(params.get("to").asString());
                BigInteger amount = _value;
                boolean rebasing = params.get("rebasing").asBoolean();
                boolean claim = params.get("claim").asBoolean();
                stake(caller, token, to, amount, rebasing, claim);
            } break;

            case "unstake": {
                /**
                 * void unstake (
                 *    Address caller,
                 *    Address token, // should be sKARMA or gKARMA
                 *    Address to,
                 *    BigInteger amount, // amount of sKARMA sent
                 *    boolean trigger,
                 *    boolean rebasing // true: sKARMA, false: gKARMA
                 * )
                 */
                JsonObject params = root.get("params").asObject();
                Address to = Address.fromString(params.get("to").asString());
                BigInteger amount = _value;
                boolean trigger = params.get("claim").asBoolean();
                boolean rebasing = params.get("rebasing").asBoolean();
                unstake(caller, token, to, amount, trigger, rebasing);
            } break;

            case "wrap": {
                /**
                 * private void wrap (
                 *     Address caller,
                 *     Address token, // should be sKARMA
                 *     Address to,
                 *     BigInteger amount
                 * )
                 */
                JsonObject params = root.get("params").asObject();
                Address to = Address.fromString(params.get("to").asString());
                BigInteger amount = _value;
                wrap(caller, token, to, amount);
            } break;

            case "unwrap": {
                /**
                 * private void unwrap (
                 *     Address caller,
                 *     Address token, // should be gKARMA
                 *     Address to,
                 *     BigInteger amount
                 * ) {
                 */
                JsonObject params = root.get("params").asObject();
                Address to = Address.fromString(params.get("to").asString());
                BigInteger amount = _value;
                unwrap(caller, token, to, amount);
            } break;

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
    }

    // --- Internal functions ---
    private BigInteger _send (
        Address to,
        BigInteger amount,
        boolean rebasing
    ) {
        if (rebasing) {
            // send as sKARMA (equal unit as KARMA)
            IIRC2.transfer(this.sKARMA, to, amount, JSONUtils.method("deposit"));
            return amount;
        } else {
            // send as gKARMA (convert units from KARMA)
            BigInteger gBalance = IgKARMA.balanceTo(gKARMA, amount);
            IgKARMA.mint(this.gKARMA, to, gBalance);
            return gBalance;
        }
    }

    // --- View functions ---
    /**
     * @return The sKARMA index, which tracks rebase growth
     */
    @External(readonly = true)
    public BigInteger index () {
        return IsKARMA.index(this.sKARMA);
    }

    /**
     * @return Contract KARMA holdings, including bonuses provided
     */
    @External(readonly = true)
    public BigInteger contractBalance () {
        return IIRC2.balanceOf(this.KARMA, Context.getAddress());
    }

    /**
     * @return Total supply staked
     */
    @External(readonly = true)
    public BigInteger totalStaked () {
        return IsKARMA.circulatingSupply(this.sKARMA);
    }
    
    /**
     * @return Total supply staked
     */
    @External(readonly = true)
    public BigInteger supplyInWarmup () {
        return IsKARMA.balanceForGons(this.sKARMA, this.gonsInWarmup.get());
    }

    // -- Admin functions ---
    /**
     * Sets the contract address for LP staking
     * 
     * Access: Governor
     * 
     * @param distributor
     */
    @External
    public void setDistributor (Address distributor) {
        accessControlled.onlyGovernor();

        this.distributor.set(distributor);
        this.DistributorSet(distributor);
    }

    /**
     * Set warmup period for new stakers
     * 
     * Access: Governor
     * 
     * @param warmupPeriod
     */
    @External
    public void setWarmupLength (BigInteger warmupPeriod) {
        accessControlled.onlyGovernor();

        this.warmupPeriod.set(warmupPeriod);
        this.WarmupSet(warmupPeriod);
    }

    // ================================================
    // Checks
    // ================================================
    private void onlyKarma (Address token) {
        Context.require(token.equals(this.KARMA),
            "onlyKarma: Only Karma token is accepted");
    }

    private void onlyStakedKarma (Address token) {
        Context.require(token.equals(this.sKARMA),
            "onlyStakedKarma: Only staked Karma token is accepted");
    }

    private void onlyGovernanceKarma (Address token) {
        Context.require(token.equals(this.gKARMA),
            "onlyGovernanceKarma: Only governance Karma token is accepted");
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
    public Address KARMA () {
        return this.KARMA;
    }

    @External(readonly = true)
    public Address sKARMA () {
        return this.sKARMA;
    }

    @External(readonly = true)
    public Address gKARMA () {
        return this.gKARMA;
    }

    @External(readonly = true)
    public Epoch epoch () {
        return this.epoch;
    }

    @External(readonly = true)
    public Address distributor () {
        return this.distributor.get();
    }

    @External(readonly = true)
    public Claim warmupInfo (Address user) {
        return this.warmupInfo.get(user);
    }

    @External(readonly = true)
    public BigInteger warmupPeriod () {
        return this.warmupPeriod.get();
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
