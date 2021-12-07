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

package dao.karma.custombond;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.JsonObject;
import dao.karma.interfaces.irc2.IIRC2;

import dao.karma.interfaces.dao.ITreasury;
import dao.karma.types.Ownable;
import dao.karma.utils.JSONUtils;
import dao.karma.utils.MathUtils;
import dao.karma.utils.StringUtils;
import dao.karma.utils.librairies.FixedPoint;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class KarmaCustomBond extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaCustomBond";

    // Contract name
    private final String name;

    private final Address payoutToken; // token paid for principal
    private final Address principalToken; // inflow token
    private final Address customTreasury; // pays for and receives principal
    private final Address karmaDAO; // The KarmaDAO contract address
    private final Address subsidyRouter; // pays subsidy in Karma to custom treasury

    // ================================================
    // DB Variables
    // ================================================
    // receives fee
    private final VarDB<Address> karmaTreasury = Context.newVarDB(NAME + "_karmaTreasury", Address.class);

    private final VarDB<BigInteger> totalPrincipalBonded = Context.newVarDB(NAME + "_totalPrincipalBonded", BigInteger.class);
    private final VarDB<BigInteger> totalPayoutGiven = Context.newVarDB(NAME + "_totalPayoutGiven", BigInteger.class);

    // stores terms for new bonds
    private final VarDB<Terms> terms = Context.newVarDB(NAME + "_terms", Terms.class);
    // stores adjustment to BCV data
    private final VarDB<Adjust> adjustment = Context.newVarDB(NAME + "_adjustment", Adjust.class);
    // stores fee tiers
    private final ArrayDB<FeeTiers> feeTiers = Context.newArrayDB(NAME + "_feeTiers", FeeTiers.class);

    // stores bond information for depositors
    private final DictDB<Address, Bond> bondInfo = Context.newDictDB(NAME + "_bondInfo", Bond.class);

    // total value of outstanding bonds; used for pricing
    private final VarDB<BigInteger> totalDebt = Context.newVarDB(NAME + "_totalDebt", BigInteger.class);
    // reference block for debt decay
    private final VarDB<Long> lastDecay = Context.newVarDB(NAME + "_lastDecay", Long.class);

    // principal accrued since subsidy paid
    private final VarDB<BigInteger> payoutSinceLastSubsidy = Context.newVarDB(NAME + "_payoutSinceLastSubsidy", BigInteger.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog
    public void BondCreated (
        BigInteger deposit,
        BigInteger payout,
        Long expires
    ) {}

    @EventLog
    public void BondRedeemed (
        Address recipient,
        BigInteger payout,
        BigInteger remaining
    ) {}

    @EventLog
    public void BondPriceChanged (
        BigInteger internalPrice,
        BigInteger debtRatio
    ) {}

    @EventLog
    public void ControlVariableAdjustment (
        BigInteger initialBCV,
        BigInteger newBCV,
        BigInteger adjustment,
        boolean addition
    ) {}

    // ================================================
    // Methods
    // ================================================
    /**
     * Contract constructor
     */
    public KarmaCustomBond (
        Address customTreasury,
        Address payoutToken,
        Address principalToken,
        Address karmaTreasury,
        Address subsidyRouter,
        Address initialOwner,
        Address karmaDAO,
        BigInteger[] tierCeilings,
        BigInteger[] fees
    ) {
        Context.require(!customTreasury.equals(ZERO_ADDRESS));
        Context.require(!payoutToken.equals(ZERO_ADDRESS));
        Context.require(!principalToken.equals(ZERO_ADDRESS));
        Context.require(!karmaTreasury.equals(ZERO_ADDRESS));
        Context.require(!subsidyRouter.equals(ZERO_ADDRESS));
        Context.require(!initialOwner.equals(ZERO_ADDRESS));
        Context.require(!karmaDAO.equals(ZERO_ADDRESS));

        this.name = "Karma Custom Bond";
        this.customTreasury = customTreasury;
        this.payoutToken = payoutToken;
        this.principalToken = principalToken;
        this.subsidyRouter = subsidyRouter;
        this.karmaDAO = karmaDAO;

        if (this.karmaTreasury.get() == null) {
            this.karmaTreasury.set(karmaTreasury);
        }

        if (this.owner.get() == null) {
            this.owner.set(initialOwner);
        }

        Context.require(tierCeilings.length == fees.length,
            "KarmaCustomBond: tier length and fee length not the same");
        
        for (int i = 0; i  < tierCeilings.length; i++) {
            feeTiers.add(new FeeTiers(tierCeilings[i], fees[i]));
        }
    }

    // --- Initialization ---
    /**
     * Initializes bond parameters
     * 
     * Access: Policy
     * 
     * @param controlVariable
     * @param vestingTerm
     * @param minimumPrice
     * @param maxPayout
     * @param maxDebt
     * @param initialDebt
     */
    public void initializeBond (
        BigInteger controlVariable,
        Long vestingTerm, // in blocks
        BigInteger minimumPrice,
        BigInteger maxPayout,
        BigInteger maxDebt,
        BigInteger initialDebt
    ) {
        // Access control
        onlyPolicy();

        Context.require(currentDebt().equals(ZERO), 
            "Debt must be 0 for initialization");

        this.terms.set (
            new Terms (
                controlVariable,
                vestingTerm,
                minimumPrice,
                maxPayout,
                maxDebt
            )
        );

        this.totalDebt.set(initialDebt);
        this.lastDecay.set(Context.getBlockHeight());
    }

    // --- Policy Functions ---
    // PARAMETER
    private final int VESTING = 0;
    private final int PAYOUT = 1;
    private final int DEBT = 2;

    /**
     * Set parameters for new bonds
     * 
     * Access: Policy
     * 
     * @param parameter
     * @param input
     */
    @External
    public void setBondTerms (
        int parameter,
        BigInteger input
    ) {
        // Access control
        onlyPolicy();

        var terms = this.terms.get();

        switch (parameter) {
            case VESTING: {
                int minHours = 36;
                int averageBlockTime = 2;
                int minVesting = minHours * 3600 / averageBlockTime;
                Context.require(input.compareTo(BigInteger.valueOf(minVesting)) >= 0, 
                    "setBondTerms: Vesting must be longer than 36 hours");
                terms.vestingTerm = input.longValue();
            } break;

            case PAYOUT: {
                Context.require(input.compareTo(BigInteger.valueOf(1000)) <= 0, 
                    "setBondTerms: Payout cannot be above 1 percent");
                terms.maxPayout = input;
            } break;

            case DEBT: {
                terms.maxDebt = input;
            } break;

            default:
                Context.revert("setBondTerms: invalid parameter");
        }
        
        this.terms.set(terms);
    }
    
    /**
     * @notice set control variable adjustment
     * 
     * Access: Policy
     * 
     * @param addition
     * @param increment
     * @param target
     * @param buffer
     */
    @External
    public void setBondTerms (
        boolean addition,
        BigInteger increment,
        BigInteger target,
        Long buffer
    ) {
        // Access control
        onlyPolicy();

        Context.require(increment.compareTo(terms.get().controlVariable.multiply(BigInteger.valueOf(30)).divide(BigInteger.valueOf(1000))) <= 0, 
            "setBondTerms: Increment too large");

        this.adjustment.set (
            new Adjust (
                addition,
                increment,
                target,
                buffer,
                Context.getBlockHeight()
            )
        );
    }
    
    // --- Custom Bond settings ---
    /**
     * Change address of Karma Treasury
     * 
     * Access: KarmaDAO
     * 
     * @param karmaTreasury
     */
    @External
    public void changeKarmaTreasury (Address karmaTreasury) {
        final Address caller = Context.getCaller();

        // Access control
        checkKarmaDao(caller);

        this.karmaTreasury.set(karmaTreasury);
    }

    /**
     * Subsidy controller checks payouts since last subsidy and resets counter
     * 
     * Access: Subsidy Controller
     */
    @External
    public BigInteger paySubsidy() {
        final Address caller = Context.getCaller();

        // Access control
        checkSubsidy(caller);

        BigInteger result = payoutSinceLastSubsidy.get();
        payoutSinceLastSubsidy.set(ZERO);

        return result;
    }

    // --- User functions ---
    /**
     * Deposit bond
     * @param amount
     * @param maxPrice
     * @param depositor
     */
    // @External - this method is external through tokenFallback
    private void deposit (
        Address caller,
        Address token, // only principalToken is accepted
        BigInteger amount, // amount of principal token received
        BigInteger maxPrice,
        Address depositor
    ) {
        Context.require(token.equals(this.principalToken),
            "deposit: Only principal token accepted for deposit");

        Context.require(!depositor.equals(ZERO_ADDRESS), 
            "deposit: invalid depositor");

        decayDebt();

        BigInteger totalDebt = this.totalDebt.get();
        var terms = this.terms.get();

        Context.require(totalDebt.compareTo(terms.maxDebt) <= 0,
            "deposit: Max capacity reached");
        
        BigInteger nativePrice = trueBondPrice();

        // slippage protection
        Context.require(maxPrice.compareTo(nativePrice) >= 0,
            "deposit: Slippage limit: more than max price"); 
        
        BigInteger value = ITreasury.valueOfToken(this.customTreasury, principalToken, amount);
        // payout to bonder is computed
        BigInteger payout = _payoutFor(value);
        
        // must be > 0.01 payout token ( underflow protection )
        Context.require(payout.compareTo(MathUtils.pow10(IIRC2.decimals(payoutToken)).divide(BigInteger.valueOf(100))) >= 0,
            "deposit: Bond too small");

        // size protection because there is no slippage
        Context.require(payout.compareTo(maxPayout()) <= 0, 
            "deposit: Bond too large");
            
        // profits are calculated
        BigInteger fee = payout.multiply(currentKarmaFee()).divide(MathUtils.pow10(6));
        
        // principal is transferred in, and 
        // deposited into the treasury, returning (amount - profit) payout token
        ITreasury.deposit(this.customTreasury, this.principalToken, amount, payout);

        // fee is transferred to dao
        if (!fee.equals(ZERO)) {
            IIRC2.transfer(payoutToken, karmaTreasury.get(), fee, JSONUtils.method("deposit"));
        }
        
        // total debt is increased
        this.totalDebt.set(totalDebt.add(value));
        
        // depositor info is stored
        this.bondInfo.set(depositor, new Bond(
            this.bondInfo.get(depositor).payout.add(payout.subtract(fee)),
            terms.vestingTerm,
            Context.getBlockHeight(),
            trueBondPrice()
        ));
        
        // indexed events are emitted
        this.BondCreated(amount, payout, Context.getBlockHeight() + terms.vestingTerm);
        this.BondPriceChanged(_bondPrice(), debtRatio());

        // total bonded increased
        this.totalPrincipalBonded.set(totalPrincipalBonded.get().add(amount));
        // total payout increased
        this.totalPayoutGiven.set(totalPayoutGiven.get().add(payout));
        // subsidy counter increased
        this.payoutSinceLastSubsidy.set(payoutSinceLastSubsidy.get().add( payout ));
        
        // control variable is adjusted
        adjust();
    }

    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) {
        JsonObject root = JSONUtils.parseData(_data);
        String method = root.get("method").asString();
        Address token = Context.getCaller();

        switch (method)
        {
            case "deposit": {
                JsonObject params = root.get("params").asObject();
                BigInteger maxPrice = StringUtils.toBigInt(params.get("maxPrice").asString());
                Address depositor = Address.fromString(params.get("depositor").asString());
                deposit(_from, token, _value, maxPrice, depositor);
                break;
            }

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
    }

    /**
     * Redeem bond for user
     * @param depositor destination address
     * @return Payout amount
     */
    @External
    public BigInteger redeem (Address depositor) {
        var info = this.bondInfo.get(depositor);
        Context.require(info != null, 
            "redeem: no bond registered for depositor");
        
        // (blocks since last interaction / vesting term remaining)
        BigInteger percentVested = percentVestedFor(depositor);
        BigInteger denominator = BigInteger.valueOf(10000);

        // if fully vested
        if (percentVested.compareTo(denominator) >= 0) {
            // delete user info
            this.bondInfo.set(depositor, null);
            // emit bond data
            this.BondRedeemed(depositor, info.payout, ZERO);
            IIRC2.transfer(payoutToken, depositor, info.payout, JSONUtils.method("redeem"));
            return info.payout;
        } else {
            // if unfinished
            // calculate payout vested
            BigInteger payout = info.payout.multiply(percentVested).divide(denominator);
            Long blockHeight = Context.getBlockHeight();

            // store updated deposit info
            BigInteger newPayout = info.payout.subtract(payout);
            bondInfo.set(depositor, new Bond(
                newPayout,
                info.vesting - (blockHeight - info.lastBlock),
                blockHeight,
                info.truePricePaid
            ));

            this.BondRedeemed(depositor, payout, newPayout);
            IIRC2.transfer(payoutToken, depositor, payout, JSONUtils.method("redeem"));
            return payout;
        }
    }

    // --- Internal help functions ---

    /**
     * Makes incremental adjustment to control variable
     */
    private void adjust() {
        var adjustment = this.adjustment.get();
        var terms = this.terms.get();
        Long blockHeight = Context.getBlockHeight();

        Long blockCanAdjust = adjustment.lastBlock + adjustment.buffer;

        if (!adjustment.rate.equals(ZERO) && blockHeight >= blockCanAdjust ) {
            BigInteger initial = terms.controlVariable;
            
            if (adjustment.add) {
                terms.controlVariable = terms.controlVariable.add(adjustment.rate);
                if (terms.controlVariable.compareTo(adjustment.target) >= 0) {
                    adjustment.rate = ZERO;
                }
            } else {
                terms.controlVariable = terms.controlVariable.subtract(adjustment.rate);
                if (terms.controlVariable.compareTo(adjustment.target) <= 0) {
                    adjustment.rate = ZERO;
                }
            }

            adjustment.lastBlock = blockHeight;

            this.terms.set(terms);
            this.adjustment.set(adjustment);

            this.ControlVariableAdjustment(initial, terms.controlVariable, adjustment.rate, adjustment.add );
        }
    }

    /**
     * Reduce total debt
     */
    private void decayDebt() {
        Long blockHeight = Context.getBlockHeight();
        this.totalDebt.set(this.totalDebt.get().subtract(debtDecay()));
        this.lastDecay.set(blockHeight);
    }

    /**
     * Calculate current bond price and remove floor if above
     * @return price
     */
    private BigInteger _bondPrice() {
        var terms = this.terms.get();

        BigInteger price = terms.controlVariable.multiply(debtRatio()).divide(MathUtils.pow10(IIRC2.decimals(payoutToken) - 5));

        if (price.compareTo(terms.minimumPrice) < 0) {
            price = terms.minimumPrice;
        } else if (!terms.minimumPrice.equals(ZERO)) {
            terms.minimumPrice = ZERO;
            this.terms.set(terms);
        }

        return price;
    }


    // ================================================
    // Checks
    // ================================================
    private void checkKarmaDao(Address caller) {
        Context.require(caller.equals(this.karmaDAO),
            "checkKarmaDao: only KarmaDAO can call this method");
    }

    private void checkSubsidy(Address caller) {
        Context.require(caller.equals(this.subsidyRouter),
            "checkKarmaDao: only Subsidy Router can call this method");
    }

    // ================================================
    // View functions
    // ================================================
    /**
     * Calculate current bond premium
     * @return price
     */
    @External(readonly = true)
    public BigInteger bondPrice() {
        var terms = this.terms.get();

        BigInteger price = terms.controlVariable.multiply(debtRatio()).divide(MathUtils.pow10(IIRC2.decimals(payoutToken) - 5));
        
        if (price.compareTo(terms.minimumPrice) < 0) {
            price = terms.minimumPrice;
        }

        return price;
    }

    /**
     * Calculate true bond price a user pays
     * @return price
     */
    @External(readonly = true)
    public BigInteger trueBondPrice() {
        return bondPrice().add(bondPrice().multiply(currentKarmaFee()).divide(MathUtils.pow10(6)));
    }

    /**
     * Determine maximum bond size
     */
    @External(readonly = true)
    public BigInteger maxPayout() {
        return IIRC2.totalSupply(this.payoutToken).multiply(this.terms.get().maxPayout).divide(BigInteger.valueOf(100000));
    }

    /**
     * Calculate total interest due for new bond
     * @param value
     */
    private BigInteger _payoutFor (BigInteger value) {
        return FixedPoint.fraction(value, bondPrice()).decode112with18().divide(MathUtils.pow10(11));
    }

    /**
     * Calculate user's interest due for new bond, accounting for Karma Fee
     * @param _value uint
     */
    @External(readonly = true)
    public BigInteger payoutFor (BigInteger value) {
        BigInteger total = FixedPoint.fraction(value, bondPrice()).decode112with18().divide(MathUtils.pow10(11));
        return total.subtract(total.multiply(currentKarmaFee()).divide(MathUtils.pow10(6)));
    }

    /**
     *  @notice calculate current ratio of debt to payout token supply
     *  @notice protocols using Karma Pro should be careful when quickly adding large %s to total supply
     */
    @External(readonly = true)
    public BigInteger debtRatio() {
        return FixedPoint.fraction (
            currentDebt().multiply(MathUtils.pow10(IIRC2.decimals(payoutToken))),
            IIRC2.totalSupply(payoutToken)
        ).decode112with18().divide(MathUtils.pow10(18));
    }

    /**
     * Calculate debt factoring in decay
     */
    @External(readonly = true)
    public BigInteger currentDebt() {
        return this.totalDebt.get().subtract(debtDecay());
    }

    /**
     * Amount to decay total debt by
     */
    @External(readonly = true)
    public BigInteger debtDecay()  {
        var totalDebt = this.totalDebt.get();
        Long blockHeight = Context.getBlockHeight();
        BigInteger blocksSinceLast = BigInteger.valueOf(blockHeight - lastDecay.get());
        BigInteger vestingTerm = BigInteger.valueOf(this.terms.get().vestingTerm);
        BigInteger decay = totalDebt.multiply(blocksSinceLast).divide(vestingTerm);

        if (decay.compareTo(totalDebt) > 0) {
            decay = totalDebt;
        }

        return decay;
    }

    /**
     * Calculate how far into vesting a depositor is
     * @param _depositor address
     */
    @External(readonly = true)
    public BigInteger percentVestedFor (Address depositor) {
        Bond bond = bondInfo.get(depositor);
        Long blockHeight = Context.getBlockHeight();
        Long blocksSinceLast = blockHeight - bond.lastBlock;
        Long vesting = bond.vesting;

        return vesting > 0 
            ? BigInteger.valueOf(blocksSinceLast).multiply(BigInteger.valueOf(10000)).divide(BigInteger.valueOf(vesting))
            : ZERO;
    }

    /**
     * Calculate amount of payout token available for claim by depositor
     * @param depositor address
     */
    @External(readonly = true)
    public BigInteger pendingPayoutFor (Address depositor) {
        BigInteger percentVested = percentVestedFor (depositor);
        BigInteger payout = bondInfo.get(depositor).payout;
        BigInteger vested = BigInteger.valueOf(10000);

        return (percentVested.compareTo(vested) >= 0)
            ? payout
            : payout.multiply(percentVested).divide(vested);
    }

    /**
     *  @notice current fee Karma takes of each bond
     *  @return currentFee_ uint
     */
    @External(readonly = true)
    public BigInteger currentKarmaFee() {
        int tierLength = feeTiers.size();
        var totalPrincipalBonded = this.totalPrincipalBonded.get();

        for (int i = 0; i < tierLength; i++) {
            var feeTier = feeTiers.get(i);

            if (totalPrincipalBonded.compareTo(feeTier.tierCeilings) < 0
            || i == (tierLength - 1)
            ) {
                return feeTier.fees;
            }
        }

        return ZERO;
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
}
