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

package dao.karma.bonddepository;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import dao.karma.interfaces.dao.ICalculator;
import dao.karma.interfaces.dao.ITeller;
import dao.karma.interfaces.dao.ITreasury;
import dao.karma.interfaces.irc2.IIRC2;
import dao.karma.types.IKarmaAccessControlled;
import dao.karma.types.KarmaAccessControlled;
import dao.karma.utils.JSONUtils;
import dao.karma.utils.MathUtils;
import dao.karma.utils.librairies.FixedPoint;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public class KarmaBondDepository implements IKarmaAccessControlled {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaBondDepository";

    // Contract name
    private final String name;

    // Implements KarmaAccessControlled
    private final KarmaAccessControlled accessControlled;

    private final Address treasury;
    private final Address KARMA;

    // ================================================
    // DB Variables
    // ================================================
    private final DictDB<BigInteger, Bond> bonds = Context.newDictDB(NAME + "_bonds", Bond.class);
    // bond IDs
    private final ArrayDB<Address> IDs = Context.newArrayDB(NAME + "_IDs", Address.class);
    // Handles payment
    private final VarDB<Address> teller = Context.newVarDB(NAME + "_teller", Address.class); 

    // ================================================
    // Event Logs
    // ================================================
    @EventLog
    public void BeforeBond(BigInteger index, BigInteger price, BigInteger internalPrice, BigInteger debtRatio) {}
    @EventLog
    public void CreateBond(BigInteger index, BigInteger amount, BigInteger payout, Long expires) {}
    @EventLog
    public void AfterBond(BigInteger index, BigInteger price, BigInteger internalPrice, BigInteger debtRatio) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public KarmaBondDepository (
        Address KARMA,
        Address treasury,
        Address authority
    ) {
        Context.require(!KARMA.equals(ZERO_ADDRESS));
        Context.require(!treasury.equals(ZERO_ADDRESS));

        this.accessControlled = new KarmaAccessControlled(NAME + "_accessControlled", authority);
        this.name = "Karma Bond Depository";

        this.KARMA = KARMA;
        this.treasury = treasury;
    }

    // -- Policy Functions ---
    /**
     * Creates a new bond type
     * 
     * Access: Guardian
     * 
     * @param principal
     * @param calculator
     * @param capacity
     * @param capacityIsPayout
     */
    @External
    public BigInteger addBond (
        Address principal,
        Address calculator,
        BigInteger capacity,
        boolean capacityIsPayout
    ) {
        // Access control
        accessControlled.onlyGuardian();

        // OK
        Terms terms = Terms.empty();

        BigInteger IDsLength = BigInteger.valueOf(IDs.size());

        bonds.set(IDsLength, 
            new Bond(
                principal, 
                calculator, 
                terms, 
                false, 
                capacity, 
                capacityIsPayout, 
                ZERO, 
                Context.getBlockHeight()
            )
        );

        IDs.add(principal);

        return IDsLength;
    }

    /**
     * Set minimum price for new bond
     * 
     * Access: Guardian
     * 
     * @param id
     * @param controlVariable
     * @param fixedTerm
     * @param vestingTerm
     * @param expiration
     * @param conclusion
     * @param minimumPrice
     * @param maxPayout
     * @param maxDebt
     * @param initialDebt
     */
    @External
    public void setTerms (
        BigInteger id,
        BigInteger controlVariable,
        boolean fixedTerm,
        Long vestingTerm,
        Long expiration,
        Long conclusion,
        BigInteger minimumPrice,
        BigInteger maxPayout,
        BigInteger maxDebt,
        BigInteger initialDebt
    ) {
        // Access control
        accessControlled.onlyGuardian();

        var bond = this.bonds.get(id);
        // Check if already exists
        Context.require(bond != null, 
            "setTerms: bond doesn't exist");
        // Check if already set
        Context.require(!bond.termsSet,
            "setTerms: Already set");

        // OK
        Terms terms = new Terms(controlVariable, fixedTerm, vestingTerm, expiration, conclusion, minimumPrice, maxPayout, maxDebt);

        bond.terms = terms;
        bond.totalDebt = initialDebt;
        bond.termsSet = true;
        this.bonds.set(id, bond);
    }

    /**
     * Disable existing bond
     * 
     * Access: Guardian
     * 
     * @param _id
     */
    @External
    public void deprecateBond (BigInteger id) {
        // Access control
        accessControlled.onlyGuardian();

        // OK
        var bond = this.bonds.get(id);
        bond.capacity = ZERO;
        this.bonds.set(id, bond);
    }

    /**
     * Set teller contract
     * 
     * Access: Governor
     * 
     * @param teller address
     */
    @External
    public void setTeller (Address teller) {
        // Access control
        accessControlled.onlyGovernor();

        // OK
        Context.require(this.teller.get() == null,
            "setTeller: Existing Teller has already been set");

        Context.require(!teller.equals(ZERO_ADDRESS),
            "setTeller: Given Teller can't be zero address");

        this.teller.set(teller);
    }

    // --- Mutable functions ---
    /**
     * Deposit bond
     * 
     * Access: Everyone
     * 
     * @param amount
     * @param maxPrice
     * @param depositor
     * @param bondId
     * @param frontEndOperator
     */
    @External
    public DepositResult deposit (
        BigInteger amount,
        BigInteger maxPrice,
        Address depositor,
        BigInteger bondId,
        Address frontEndOperator
    ) {
        Context.require(!depositor.equals(ZERO_ADDRESS), 
            "deposit: Invalid address");

        var info = this.bonds.get(bondId);

        Context.require(info.termsSet, 
            "deposit: Bond not initialized");
        
        long blockHeight = Context.getBlockHeight();
        Context.require(blockHeight < info.terms.conclusion,
            "deposit: Bond concluded");
        
        this.BeforeBond(bondId, bondPriceInUsd(bondId), bondPrice(bondId), debtRatio(bondId));
        
        decayDebt(bondId);

        Context.require(info.totalDebt.compareTo(info.terms.maxDebt) <= 0,
            "deposit: Max debt exceeded");
        
        // slippage protection            
        Context.require(maxPrice.compareTo(_bondPrice(bondId)) >= 0,
            "deposit: Slippage limit: more than max price");

        BigInteger value = ITreasury.tokenValue(treasury, info.principal, amount);
        // payout to bonder is computed
        BigInteger payout = payoutFor(value, bondId);

        // ensure there is remaining capacity for bond
        if (info.capacityIsPayout) {
            // capacity in payout terms
            Context.require(info.capacity.compareTo(payout) >= 0,
                "deposit: Bond concluded");
            info.capacity = info.capacity.subtract(payout);
        } else {
            // capacity in principal terms
            Context.require(info.capacity.compareTo(amount) >= 0,
                "deposit: Bond concluded");
            info.capacity = info.capacity.subtract(amount);
        }

        int karmaDecimals = IIRC2.decimals(this.KARMA);

        // must be > 0.01 KARMA (underflow protection)
        BigInteger minimalPayout = MathUtils.pow10(karmaDecimals).divide(BigInteger.valueOf(100));

        Context.require(payout.compareTo(minimalPayout) >= 0,
            "deposit: Bond too small");
        
        // size protection because there is no slippage
        Context.require(payout.compareTo(maxPayout(bondId)) <= 0,
            "deposit: Bond too large");

        // send payout to treasury
        IIRC2.transfer(info.principal, treasury, amount, JSONUtils.method("payout"));

        // increase total debt
        var bondStorage = this.bonds.get(bondId);
        bondStorage.totalDebt = info.totalDebt.add(value);
        this.bonds.set(bondId, bondStorage);

        Long expiration = info.terms.vestingTerm + blockHeight;
        if (!info.terms.fixedTerm) {
            expiration = info.terms.expiration;
        }

        // user info stored with teller
        BigInteger index = ITeller.newBond(this.teller.get(), depositor, info.principal, amount, payout, expiration, frontEndOperator);

        this.CreateBond(bondId, amount, payout, expiration);

        return new DepositResult(payout, index);
    }

    // --- Internal functions ---
    /**
     * Reduce total debt
     * 
     * @param bondId
     */
    private void decayDebt (BigInteger bondId) {
        var bond = this.bonds.get(bondId);
        bond.totalDebt = bond.totalDebt.subtract(debtDecay(bondId));
        bond.lastDecay = Context.getBlockHeight();
        this.bonds.set(bondId, bond);
    }

    // --- View functions ---
    /**
     * Returns data about a bond type
     * 
     * @param bondId
     */
    @External(readonly = true)
    public BondInfo bondInfo (BigInteger bondId) {
        var info = this.bonds.get(bondId);
        return new BondInfo (
            info.principal, 
            info.calculator, 
            info.totalDebt, 
            info.lastDecay
        );
    }

    /**
     * Returns terms for a bond type
     * 
     * @param bondId
     */
    @External(readonly = true)
    public BondTerms bondTerms (BigInteger bondId) {
        var terms = this.bonds.get(bondId).terms;
        return new BondTerms(
            terms.controlVariable, 
            terms.vestingTerm, 
            terms.minimumPrice, 
            terms.maxPayout, 
            terms.maxDebt
        );
    }

    // --- Payout ---
    /**
     * Determine maximum bond size
     * 
     * @param bondId
     */
    @External(readonly = true)
    public BigInteger maxPayout (BigInteger bondId) {
        BigInteger totalSupply = IIRC2.totalSupply(KARMA);
        return totalSupply.multiply(this.bonds.get(bondId).terms.maxPayout).divide(BigInteger.valueOf(100000));
    }

    /**
     * Payout due for amount of treasury value
     * 
     * @param value uint
     * @param bondId uint
     */
    @External(readonly = true)
    public BigInteger payoutFor (BigInteger value, BigInteger bondId) {
        return FixedPoint.fraction(value, bondPrice(bondId)).decode112with18().divide(MathUtils.pow10(16));
    }

    @External(readonly = true)
    public BigInteger payoutForAmount (BigInteger amount, BigInteger bondId) {
        Address principal = this.bonds.get(bondId).principal;
        return payoutFor (ITreasury.tokenValue(this.treasury, principal, amount), bondId);
    }

    // --- Bond Price ---
    /**
     * Calculate current bond premium
     */
    @External(readonly = true)
    public BigInteger bondPrice (BigInteger bondId) {
        var bond = this.bonds.get(bondId);
        BigInteger price = bond.terms.controlVariable.multiply(debtRatio(bondId)).add(BigInteger.valueOf(1000000000)).divide(MathUtils.pow10(7));

        if (price.compareTo(bond.terms.minimumPrice) < 0) {
            price = bond.terms.minimumPrice;
        }

        return price;
    }

    /**
     * Calculate current bond price and remove floor if above
     */
    @External(readonly = true)
    public BigInteger _bondPrice (BigInteger bondId) {
        var info = this.bonds.get(bondId);
        BigInteger price = info.terms.controlVariable.multiply(debtRatio(bondId)).add(BigInteger.valueOf(1000000000)).divide(MathUtils.pow10(7));

        if (price.compareTo(info.terms.minimumPrice) < 0) {
            price = info.terms.minimumPrice;
        } else if (!info.terms.minimumPrice.equals(ZERO)) {
            info.terms.minimumPrice = ZERO;
            this.bonds.set(bondId, info);
        }

        return price;
    }

    /**
     * Converts bond price to USD value
     * 
     * @param bondId
     */
    @External(readonly = true)
    public BigInteger bondPriceInUsd (BigInteger bondId) {
        var bond = this.bonds.get(bondId);
        return (!bond.calculator.equals(ZERO_ADDRESS)) 
        ? bondPrice(bondId).multiply(ICalculator.markdown(bond.calculator, bond.principal)).divide(BigInteger.valueOf(100))
        : bondPrice(bondId).multiply(MathUtils.pow10(IIRC2.decimals(bond.principal))).divide(BigInteger.valueOf(100));
    }

    // --- Debt --- 
    /**
     * Calculate current ratio of debt to KARMA supply
     * 
     * @param bondId
     */
    @External(readonly = true)
    public BigInteger debtRatio (BigInteger bondId) {
        BigInteger totalSupply = IIRC2.totalSupply(this.KARMA);
        return FixedPoint.fraction(currentDebt(bondId).multiply(MathUtils.pow10(9)), totalSupply).decode112with18().divide(MathUtils.pow10(18));
    }

    /**
     * Debt ratio in same terms for reserve or liquidity bonds
     * 
     * @param bondId
     */
    public BigInteger standardizedDebtRatio (BigInteger bondId) {
        var bond = this.bonds.get(bondId);
        if (!bond.calculator.equals(ZERO_ADDRESS)) {
            return debtRatio(bondId).multiply(ICalculator.markdown(bond.calculator, bond.principal)).divide(MathUtils.pow10(9));
        } else {
            return debtRatio(bondId);
        }
    }

    /**
     * Calculate debt factoring in decay
     * 
     * @param bondId
     * @return
     */
    public BigInteger currentDebt (BigInteger bondId) {
        return this.bonds.get(bondId).totalDebt.subtract(debtDecay(bondId));
    }

    /**
     * Amount to decay total debt by
     * 
     * @param bondId
     */
    public BigInteger debtDecay (BigInteger bondId) {
        var bond = this.bonds.get(bondId);
        Long blocksSinceLast = Context.getBlockHeight() - bond.lastDecay;

        BigInteger decay = bond.totalDebt.multiply(BigInteger.valueOf(blocksSinceLast)).divide(BigInteger.valueOf(bond.terms.vestingTerm));

        if (decay.compareTo(bond.totalDebt) > 0) {
            decay = bond.totalDebt;
        }

        return decay;
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
    public Address IDs(int index) {
        return this.IDs.get(index);
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
