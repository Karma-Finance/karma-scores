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

package dao.karma.staked;

import static dao.karma.utils.IntUtils.MAX_UINT256;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import dao.karma.interfaces.dao.IStaking;
import dao.karma.interfaces.dao.IgKARMA;
import dao.karma.interfaces.irc2.IIRC2;
import dao.karma.standards.token.irc2.IRC2Basic;
import dao.karma.structs.dao.Rebase;
import dao.karma.utils.IntUtils;
import dao.karma.utils.MathUtils;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class KarmaTokenStaked extends IRC2Basic {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaTokenStaked";
    public static final int decimals = 9;

    // Decimals 9
    private final BigInteger INITIAL_FRAGMENTS_SUPPLY = BigInteger.valueOf(5_000_000).multiply(MathUtils.pow10(9));

    // TOTAL_GONS is a multiple of INITIAL_FRAGMENTS_SUPPLY so that _gonsPerFragment is an integer.
    // Use the highest value that fits in a uint256 for max granularity.
    private final BigInteger TOTAL_GONS = MAX_UINT256.subtract(MAX_UINT256.mod(INITIAL_FRAGMENTS_SUPPLY));

    // MAX_SUPPLY = maximum integer < (sqrt(4*TOTAL_GONS + 1) - 1) / 2
    private final BigInteger MAX_SUPPLY = IntUtils.MAX_UINT128;

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<Address> initializer = Context.newVarDB(NAME + "_initializer", Address.class);

    // Index Gons - tracks rebase growth
    private final VarDB<BigInteger> INDEX = Context.newVarDB(NAME + "_INDEX", BigInteger.class);

    // balance used to calc rebase
    public final VarDB<Address> stakingContract = Context.newVarDB(NAME + "_stakingContract", Address.class);
    // additional staked supply (governance token)
    public final VarDB<Address> gKARMA = Context.newVarDB(NAME + "_gOHM", Address.class);

    // past rebase data
    public final ArrayDB<Rebase> rebases = Context.newArrayDB(NAME + "_rebases", Rebase.class);

    private final VarDB<BigInteger> _gonsPerFragment = Context.newVarDB(NAME + "_gonsPerFragment", BigInteger.class);
    private final DictDB<Address, BigInteger> _gonBalances = Context.newDictDB(NAME + "_gonBalances", BigInteger.class);

    public final VarDB<Address> treasury = Context.newVarDB(NAME + "_treasury", Address.class);
    public final DictDB<Address, BigInteger> debtBalances = Context.newDictDB(NAME + "_debtBalances", BigInteger.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog(indexed = 1)
    public void LogSupply (BigInteger epoch, BigInteger totalSupply) {}

    @EventLog(indexed = 1)
    public void LogRebase (BigInteger epoch, BigInteger rebase, BigInteger index) {}

    @EventLog
    public void LogStakingContractUpdated (Address stakingContract) {}


    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public KarmaTokenStaked (
    ) {
        super("Karma Token Staked", "sKARMA", decimals, BigInteger.ZERO);
        final Address caller = Context.getCaller();

        if (this.initializer.get() == null) {
            this.initializer.set(caller);
        }

        if (this.totalSupply.get() == null) {
            this.totalSupply.set(INITIAL_FRAGMENTS_SUPPLY);
        }

        if (this._gonsPerFragment.get() == null) {
            this._gonsPerFragment.set(TOTAL_GONS.divide(INITIAL_FRAGMENTS_SUPPLY));
        }
    }

    // -- Initialization ---
    /**
     * Access: Initializer
     * 
     * @param index
     */
    @External
    public void setIndex (BigInteger index) {
        // Access control
        onlyInitializer();

        Context.require(this.INDEX.get() == null,
            "setIndex: INDEX already initialized");

        // OK
        this.INDEX.set(gonsForBalance(index));
    }

    /**
     * Access: Initializer
     * 
     * @param gKARMA
     */
    @External
    public void setgKARMA (Address gKARMA) {
        // Access control
        onlyInitializer();

        Context.require(this.gKARMA.get() == null,
            "setgKARMA: gKARMA already initialized");

        Context.require(!gKARMA.equals(ZERO_ADDRESS) && gKARMA.isContract(),
            "setgKARMA: gKARMA is not a valid contract");

        // OK
        this.gKARMA.set(gKARMA);
    }

    /**
     * Access: Initializer
     * 
     * dev: Do this last
     * 
     * @param stakingContract
     * @param treasury
     */
    @External
    public void initialize (Address stakingContract, Address treasury) {
        // Access control
        onlyInitializer();

        Context.require(!stakingContract.equals(ZERO_ADDRESS) && stakingContract.isContract(),
            "initialize: stakingContract is not a valid contract");

        Context.require(!treasury.equals(ZERO_ADDRESS) && treasury.isContract(),
            "initialize: treasury is not a valid contract");

        // OK
        this.stakingContract.set(stakingContract);
        this._gonBalances.set(stakingContract, TOTAL_GONS);
        this.treasury.set(treasury);

        this.Transfer(ZERO_ADDRESS, stakingContract, this.totalSupply(), "initialize".getBytes());
        this.LogStakingContractUpdated(stakingContract);

        // Prevent initializing again
        this.initializer.set(null);
    }

    // --- Rebase ---
    /**
     * Increases sKARMA supply to increase staking balances relative to {@code profit}
     * 
     * Access : Staking Contract
     * 
     * @param profit
     * @param epoch
     */
    @External
    public BigInteger rebase (BigInteger profit, BigInteger epoch) {
        // Access control
        onlyStakingContract();

        // OK
        BigInteger rebaseAmount = ZERO;
        BigInteger circulatingSupply = this.circulatingSupply();
        var totalSupply = this.totalSupply();

        if (profit.equals(ZERO)) {
            this.LogSupply(epoch, totalSupply);
            this.LogRebase(epoch, ZERO, index());
            return totalSupply;
        } else if (circulatingSupply.compareTo(ZERO) > 0) {
            rebaseAmount = profit.multiply(totalSupply).divide(circulatingSupply);
        } else {
            rebaseAmount = profit;
        }

        totalSupply = totalSupply.add(rebaseAmount);

        if (totalSupply.compareTo(MAX_SUPPLY) > 0) {
            totalSupply = MAX_SUPPLY;
        }

        this.totalSupply.set(totalSupply);

        this._gonsPerFragment.set(TOTAL_GONS.divide(totalSupply));

        _storeRebase(circulatingSupply, profit, epoch);

        return totalSupply;
    }

    /**
     * Emits event with data about rebase
     * 
     * @param previousCirculating
     * @param profit
     * @param epoch
     */
    private void _storeRebase(
        BigInteger previousCirculating, 
        BigInteger profit, 
        BigInteger epoch
    ) {
        BigInteger rebasePercent = profit.multiply(MathUtils.pow10(18)).divide(previousCirculating);
        BigInteger index = index();

        rebases.add(
            new Rebase(
                epoch, 
                rebasePercent, 
                previousCirculating, 
                circulatingSupply(),
                profit,
                index,
                Context.getBlockHeight()
            )
        );

        this.LogSupply(epoch, this.totalSupply());
        this.LogRebase(epoch, rebasePercent, index);
    }

    // --- Mutative functions ---
    @Override
    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        final Address caller = Context.getCaller();
        BigInteger gonValue = _value.multiply(_gonsPerFragment.get());

        // check some basic requirements
        Context.require(gonValue.compareTo(BigInteger.ZERO) >= 0, 
            "gonValue needs to be positive");
        Context.require(safeGetBalance(caller).compareTo(gonValue) >= 0, 
            "Insufficient balance");

        // adjust the balances
        safeSetBalance(caller, safeGetBalance(caller).subtract(gonValue));
        safeSetBalance(_to, safeGetBalance(_to).add(gonValue));

        var balance = balanceOf(caller);
        var debt = debtBalances.getOrDefault(caller, ZERO);
        Context.require(balance.compareTo(debt) >= 0, "Debt: cannot transfer amount");

        // if the recipient is SCORE, call 'tokenFallback' to handle further operation
        byte[] dataBytes = (_data == null) ? new byte[0] : _data;
        if (_to.isContract()) {
            Context.call(_to, "tokenFallback", caller, _value, dataBytes);
        }

        // emit Transfer event
        Transfer(caller, _to, _value, dataBytes);
    }

    protected BigInteger safeGetBalance(Address owner) {
        return this._gonBalances.getOrDefault(owner, BigInteger.ZERO);
    }

    protected void safeSetBalance(Address owner, BigInteger amount) {
        this._gonBalances.set(owner, amount);
    }

    /**
     * Update the debt of a given address
     * 
     * Access : Treasury
     */
    @External
    public void changeDbt (
        BigInteger amount,
        Address debtor,
        boolean add
    ) {
        // Access control
        onlyTreasury();

        // OK
        var debt = debtBalances.getOrDefault(debtor, ZERO);

        if (add) {
            debtBalances.set(debtor, debt.add(amount));
        } else {
            debtBalances.set(debtor, debt.subtract(amount));
        }
    }

    // --- View functions ---
    @Override
    @External(readonly = true)
    public BigInteger balanceOf (Address who) {
        return _gonBalances.getOrDefault(who, ZERO).divide(_gonsPerFragment.get());
    }

    @External(readonly = true)
    public BigInteger gonsForBalance (BigInteger amount) {
        return amount.multiply(_gonsPerFragment.get());
    }

    @External(readonly = true)
    public BigInteger balanceForGons (BigInteger gons) {
        return gons.divide(_gonsPerFragment.get());
    }

    @External(readonly = true)
    public BigInteger toG (BigInteger amount) {
        return IgKARMA.balanceTo(this.gKARMA.get(), amount);
    }

    @External(readonly = true)
    public BigInteger fromG (BigInteger amount) {
        return IgKARMA.balanceFrom(this.gKARMA.get(), amount);
    }

    /**
     * Staking contract holds excess sKARMA
     * @return
     */
    @External(readonly = true)
    public BigInteger circulatingSupply () {
        var totalSupply = this.totalSupply();
        var stakingContract = this.stakingContract.get();
        var balanceStakingContract = balanceOf(stakingContract);
        var gKarmaSupply = IIRC2.totalSupply(this.gKARMA.get());
        var balanceFrom = IgKARMA.balanceFrom(this.gKARMA.get(), gKarmaSupply);
        var supplyWarmup = IStaking.supplyInWarmup(stakingContract);
        return totalSupply.subtract(balanceStakingContract).add(balanceFrom).add(supplyWarmup);
    }

    @External(readonly = true)
    public BigInteger index () {
        return balanceForGons(this.INDEX.get());
    }

    // ================================================
    // Checks
    // ================================================
    private void onlyStakingContract() {
        final Address caller = Context.getCaller();
        Context.require(caller.equals(this.stakingContract.get()), 
            "onlyStakingContract: only the staking contract can call this method");
    }

    private void onlyInitializer() {
        final Address caller = Context.getCaller();
        Context.require(caller.equals(this.initializer.get()),
            "onlyInitializer: only initializer can call this method");
    }

    private void onlyTreasury() {
        final Address caller = Context.getCaller();
        Context.require(caller.equals(this.treasury.get()),
            "onlyTreasury: only treasury can call this method");
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public Address stakingContract () {
        return this.stakingContract.get();
    }

    @External(readonly = true)
    public Address gKARMA () {
        return this.gKARMA.get();
    }

    @External(readonly = true)
    public Rebase rebases (int index) {
        return this.rebases.get(index);
    }

    @External(readonly = true)
    public Address treasury () {
        return this.treasury.get();
    }

    @External(readonly = true)
    public BigInteger debtBalances (Address address) {
        return this.debtBalances.get(address);
    }
}
