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

package dao.karma.custombondbalanced;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.Map;

import com.eclipsesource.json.JsonObject;

import dao.karma.interfaces.bond.IBalancedDEX;
import dao.karma.interfaces.bond.ICustomTreasuryBalanced;
import dao.karma.interfaces.bond.IToken;
import dao.karma.interfaces.dao.ITreasury;
import dao.karma.interfaces.oracle.IKarmaOracle;
import dao.karma.structs.bond.Adjust;
import dao.karma.structs.bond.Terms;
import dao.karma.types.Ownable;
import dao.karma.utils.JSONUtils;
import dao.karma.utils.MathUtils;
import dao.karma.utils.StringUtils;
import dao.karma.utils.librairies.FullMath;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class KarmaCustomBondBalanced extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaCustomBondBalanced";

    // Contract name
    private final String name;

    // token paid for principal
    private final Address payoutToken;
    // inflow token - should always be a Balanced LP token. In the current Balanced Protocol implementation, this is the DEX contract.
    private final Address principalToken;
    // The Balanced LP token Pool ID
    private final BigInteger principalPoolId;
    // pays for and receives principal
    private final Address customTreasury;
    // pays subsidy in Karma to custom treasury
    private final Address subsidyRouter;

    // --- Bond constants ---
    // When calling setBondTerms(VESTING), 
    // vesting must be longer than `BOND_TERMS_VESTING_MIN_SECONDS` hours
    public static final int BOND_TERMS_VESTING_MIN_SECONDS = 36 * 3600; // 36 hours

    // When calling setBondTerms(VESTING), 
    // maxPayout must be <= 1000 (= 1%)
    private static final BigInteger BOND_TERMS_PAYOUT_MAX_PERCENT = BigInteger.valueOf(1000);

    // Arbitrary decimals precision
    private static final int DECIMALS_PRECISION_EXPONENT = 5;
    private static final BigInteger DECIMALS_PRECISION = MathUtils.pow10(DECIMALS_PRECISION_EXPONENT);
    private static final BigInteger BOND_PRICE_PRECISION = MathUtils.pow10(7);
    private static final BigInteger TRUE_BOND_PRICE_PRECISION = MathUtils.pow10(6);
    // Arbitrary payout precision
    private static final BigInteger TOTAL_PAYOUT_PRECISION = MathUtils.pow10(11);
    private static final BigInteger PAYOUT_PRECISION = MathUtils.pow10(6);
    // Arbitrary vesting precision
    // 10000 = 100%
    private static final BigInteger FULLY_VESTED = BigInteger.valueOf(10000);

    // 10**18
    private static final BigInteger EXA = MathUtils.pow10(18);

    // ================================================
    // DB Variables
    // ================================================
    // receives fee
    private final VarDB<Address> karmaTreasury = Context.newVarDB(NAME + "_karmaTreasury", Address.class);
    private final VarDB<Address> karmaOracle = Context.newVarDB(NAME + "_karmaOracle", Address.class);
    private final VarDB<Address> karmaDAO = Context.newVarDB(NAME + "_karmaDAO", Address.class);

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
        long expires
    ) {}

    @EventLog
    public void PayoutUpdate (
        BigInteger oldPayout,
        BigInteger newPayout
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

    @EventLog
    public void DaoAddressChanged (
        Address oldAddress, 
        Address newAddress
    ) {}

    // ================================================
    // Methods
    // ================================================
    /**
     * Contract constructor
     * 
     * @param customTreasury The custom treasury associated with the bond
     * @param payoutToken The payout token address associated with the bond, token paid for principal
     * @param principalToken The inflow token (should always be a Balanced LP Token)
     * @param principalPoolId The inflow token pool ID
     * @param karmaTreasury The Karma treasury
     * @param subsidyRouter pays subsidy in Karma to custom treasury
     * @param initialOwner The initial policy role address
     * @param karmaDAO The KarmaDAO contract address
     * @param tierCeilings Array of ceilings of principal bonded till next tier
     * @param fees Array of fees tiers, in ten-thousandths (i.e. 33300 = 3.33%)
     */
    public KarmaCustomBondBalanced (
        Address customTreasury,
        Address payoutToken,
        Address principalToken,
        BigInteger principalPoolId,
        Address karmaTreasury,
        Address karmaOracle,
        Address subsidyRouter,
        Address initialOwner,
        Address karmaDAO,
        BigInteger[] tierCeilings,
        BigInteger[] fees
    ) {
        super(initialOwner);

        // Check inputs
        Context.require(!customTreasury.equals(ZERO_ADDRESS));
        Context.require(!payoutToken.equals(ZERO_ADDRESS));
        Context.require(!principalToken.equals(ZERO_ADDRESS));
        Context.require(!karmaTreasury.equals(ZERO_ADDRESS));
        Context.require(!karmaOracle.equals(ZERO_ADDRESS));
        Context.require(!subsidyRouter.equals(ZERO_ADDRESS));
        Context.require(!initialOwner.equals(ZERO_ADDRESS));
        Context.require(!karmaDAO.equals(ZERO_ADDRESS));

        Context.require(tierCeilings.length == fees.length,
            "KarmaCustomBondBalanced: tier length and fee length not the same");

        // OK
        this.name = "Karma Custom Bond (Balanced)";
        this.customTreasury = customTreasury;
        this.payoutToken = payoutToken;
        this.principalToken = principalToken;
        this.principalPoolId = principalPoolId;
        this.subsidyRouter = subsidyRouter;

        for (int i = 0; i  < tierCeilings.length; i++) {
            this.feeTiers.add(new FeeTiers(tierCeilings[i], fees[i]));
        }

        if (this.karmaTreasury.get() == null) {
            this.karmaTreasury.set(karmaTreasury);
        }

        if (this.karmaOracle.get() == null) {
            this.karmaOracle.set(karmaOracle);
        }

        if (this.karmaDAO.get() == null) {
            this.karmaDAO.set(karmaDAO);
        }

        // Default initialization
        if (this.lastDecay.get() == null) {
            this.lastDecay.set(0L);
        }

        if (this.totalDebt.get() == null) {
            this.totalDebt.set(ZERO);
        }

        if (this.payoutSinceLastSubsidy.get() == null) {
            this.payoutSinceLastSubsidy.set(ZERO);
        }

        if (this.totalPrincipalBonded.get() == null) {
            this.totalPrincipalBonded.set(ZERO);
        }

        if (this.totalPayoutGiven.get() == null) {
            this.totalPayoutGiven.set(ZERO);
        }

        if (this.terms.get() == null) {
            this.terms.set(Terms.empty());
        }

        if (this.adjustment.get() == null) {
            this.adjustment.set(Adjust.empty());
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
    @External
    public void initializeBond (
        BigInteger controlVariable,
        long vestingTerm, // in blocks
        BigInteger minimumPrice,
        BigInteger maxPayout,
        BigInteger maxDebt,
        BigInteger initialDebt,
        BigInteger maxDiscount
    ) {
        // Access control
        onlyPolicy();

        // Initialization control
        Context.require(currentDebt().equals(ZERO), 
            "initializeBond: Debt must be 0 for initialization");

        // OK
        this.terms.set (
            new Terms (
                controlVariable,
                vestingTerm,
                minimumPrice,
                maxPayout,
                maxDebt,
                maxDiscount
            )
        );

        this.totalDebt.set(initialDebt);
        this.lastDecay.set(Context.getBlockHeight());
    }

    // --- Policy Functions ---
    // PARAMETER
    public static final int VESTING = 0;
    public static final int PAYOUT = 1;
    public static final int DEBT = 2;

    /**
     * Change the parameters of a bond
     * 
     * Access: Policy
     * 
     * @param parameter The input type, its value is either 0 (VESTING), 1 (PAYOUT) or 2 (DEBT)
     * @param input The input value
     */
    @External
    public void setBondTerms (
        int parameter,
        BigInteger input
    ) {
        // Access control
        onlyPolicy();

        // OK
        var terms = this.terms.get();

        switch (parameter) {
            case VESTING: {
                int averageBlockTimeInSeconds = 2;
                int minVesting = BOND_TERMS_VESTING_MIN_SECONDS / averageBlockTimeInSeconds;
                Context.require(input.compareTo(BigInteger.valueOf(minVesting)) >= 0, 
                    "setBondTerms: Vesting must be longer than " + BOND_TERMS_VESTING_MIN_SECONDS + " seconds");
                terms.vestingTerm = input.longValueExact();
            } break;

            case PAYOUT: {
                Context.require(input.compareTo(BOND_TERMS_PAYOUT_MAX_PERCENT) <= 0, 
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
     * Set control variable adjustment
     * 
     * Access: Policy
     * 
     * Requirements:
     *  - The `increment` value must be inferior or equal to BCV*3/100
     * 
     * @param addition Addition (true) or subtraction (false) of BCV
     * @param increment The increment value of the `controlVariable` value
     * @param target BCV when adjustment finished
     * @param buffer Minimum length (in blocks) between adjustments
     */
    @External
    public void setAdjustment (
        boolean addition,
        BigInteger increment,
        BigInteger target,
        long buffer
    ) {
        // Access control
        onlyPolicy();

        // require(increment <= BCV*3/100)
        Context.require(increment.compareTo(terms.get().controlVariable.multiply(BigInteger.valueOf(3)).divide(BigInteger.valueOf(100))) <= 0, 
            "setAdjustment: Increment too large");

        // OK
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
    public void changeKarmaTreasury (
        Address karmaTreasury
    ) {
        final Address caller = Context.getCaller();

        // Access control
        checkKarmaDao(caller);

        // OK
        this.DaoAddressChanged(this.karmaTreasury.get(), karmaTreasury);
        this.karmaTreasury.set(karmaTreasury);
    }

    /**
     * Change address of Karma DAO
     * 
     * Access: KarmaDAO
     * 
     * @param karmaDAO
     */
    @External
    public void changeKarmaDAO (
        Address karmaDAO
    ) {
        final Address caller = Context.getCaller();

        // Access control
        checkKarmaDao(caller);

        // OK
        this.DaoAddressChanged(this.karmaDAO.get(), karmaDAO);
        this.karmaDAO.set(karmaDAO);
    }

    /**
     * Change address of Karma Oracle
     * 
     * Access: KarmaDAO
     * 
     * @param karmaOracle
     */
    @External
    public void changeKarmaOracle (
        Address karmaOracle
    ) {
        final Address caller = Context.getCaller();

        // Access control
        checkKarmaDao(caller);

        // OK
        this.DaoAddressChanged(this.karmaOracle.get(), karmaOracle);
        this.karmaOracle.set(karmaOracle);
    }

    /**
     * Subsidy controller checks payouts since last subsidy and resets counter
     * 
     * Access: Subsidy Router
     */
    @External
    public BigInteger paySubsidy() {
        final Address caller = Context.getCaller();

        // Access control
        checkSubsidyRouter(caller);

        // OK
        BigInteger result = payoutSinceLastSubsidy.get();
        this.payoutSinceLastSubsidy.set(ZERO);
        this.PayoutUpdate(result, ZERO);

        return result;
    }

    // --- User functions ---
    /**
     * Deposit bond
     * 
     * @param amount
     * @param maxPrice
     * @param depositor
     */
    // @External - this method is external through onIRC31Received
    private void deposit (
        Address caller, // the method caller. This field is handled by onIRC31Received
        Address token, // only principalToken is accepted. This field is handled by onIRC31Received
        BigInteger poolId, // The LP token Pool ID. This field is handled by onIRC31Received
        BigInteger amount, // amount of principal inflow token received. This field is handled by onIRC31Received
        BigInteger maxPrice,
        Address depositor
    ) {
        Context.require(token.equals(this.principalToken),
            "deposit: Only principal token accepted for deposit");

        Context.require(poolId.equals(this.principalPoolId),
            "deposit: Wrong LP token Pool ID");

        Context.require(!depositor.equals(ZERO_ADDRESS), 
            "deposit: invalid depositor");

        decayDebt();

        BigInteger totalDebt = this.totalDebt.get();
        var terms = this.terms.get();

        BigInteger nativePrice = trueBondPrice(null);

        // slippage protection
        Context.require(maxPrice.compareTo(nativePrice) >= 0,
            "deposit: Slippage limit: more than max price"); 

        BigInteger value = ICustomTreasuryBalanced.valueOfToken(this.customTreasury, this.principalToken, amount);
        // payout to bonder is computed
        BigInteger payout = _payoutFor(value);

        // Check if the deposit doesn't exceed the max debt
        Context.require(totalDebt.add(value).compareTo(terms.maxDebt) <= 0,
            "deposit: Max capacity reached");

        // must be > 0.01 payout token (underflow protection)
        // payout >= (10**payoutDecimals)/100
        Context.require(payout.compareTo(MathUtils.pow10(IToken.decimals(this.payoutToken)).divide(BigInteger.valueOf(100))) >= 0,
            "deposit: Bond too small");

        // size protection because there is no slippage
        Context.require(payout.compareTo(maxPayout()) <= 0, 
            "deposit: Bond too large");

        // profits are calculated
        BigInteger fee = payout.multiply(currentKarmaFee()).divide(PAYOUT_PRECISION);

        // principal is transferred in, and 
        // deposited into the treasury, returning (amount - profit) payout token
        ICustomTreasuryBalanced.deposit(this.customTreasury, this.principalToken, this.principalPoolId, amount, payout);

        // Fee is transferred to DAO treasury
        if (!fee.equals(ZERO)) {
            ITreasury.deposit(this.karmaTreasury.get(), this.payoutToken, fee);
        }

        // total debt is increased
        this.totalDebt.set(totalDebt.add(value));

        // depositor info is stored
        var depositorBondInfo = this.bondInfo.getOrDefault(depositor, Bond.empty());

        this.bondInfo.set(depositor, new Bond(
            depositorBondInfo.payout.add(payout.subtract(fee)),
            terms.vestingTerm,
            Context.getBlockHeight(),
            trueBondPrice(null)
        ));

        // indexed events are emitted
        this.BondCreated(amount, payout, Context.getBlockHeight() + terms.vestingTerm);
        this.BondPriceChanged(_bondPrice(), debtRatio());

        // total bonded increased
        this.totalPrincipalBonded.set(this.totalPrincipalBonded.get().add(amount));
        // total payout increased
        this.totalPayoutGiven.set(this.totalPayoutGiven.get().add(payout));
        // subsidy counter increased
        BigInteger oldPayout = this.payoutSinceLastSubsidy.get();
        BigInteger newPayout = oldPayout.add(payout);
        this.payoutSinceLastSubsidy.set(newPayout);
        this.PayoutUpdate(oldPayout, newPayout);

        // control variable is adjusted
        adjust();
    }

    /**
     * A method for handling a single token type transfer, which is called from the multi token contract.
     * It works by analogy with the fallback method of the normal transactions and returns nothing.
     * Throws if it rejects the transfer.
     * @param _operator: The address which initiated the transfer
     * @param _from: the address which previously owned the token
     * @param _id: the ID of the token being transferred
     * @param _value: the amount of tokens being transferred
     * @param _data: additional data with no specified format
     */
    @External
    public void onIRC31Received (Address _operator, Address _from, BigInteger _id, BigInteger _value, @Optional byte[] _data) {
        JsonObject root = JSONUtils.parseData(_data);
        String method = root.get("method").asString();
        Address token = Context.getCaller();

        switch (method)
        {
            case "deposit": {
                JsonObject params = root.get("params").asObject();
                BigInteger maxPrice = StringUtils.toBigInt(params.get("maxPrice").asString());
                Address depositor = Address.fromString(params.get("depositor").asString());
                deposit(_operator, token, _id, _value, maxPrice, depositor);
                break;
            }

            default:
                Context.revert("onIRC31Received: Unimplemented tokenFallback action");
        }
    }

    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) {
        JsonObject root = JSONUtils.parseData(_data);
        String method = root.get("method").asString();
        Address token = Context.getCaller();

        switch (method)
        {
            case "pay": {
                pay(token, _value);
                break;
            }

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
    }

    private void pay (Address token, BigInteger value) {
        Context.require(value.compareTo(BigInteger.ZERO) > 0,
            "pay: Nothing paid");
        // Only accept payoutToken as payment from the deposit
        Context.require(token.equals(this.payoutToken), 
            "pay: Only payout token is accepted as payment");
    }

    /**
     * Redeem bond for user
     * 
     * Access: Everyone
     * 
     * @param depositor destination address
     * @return Payout amount
     */
    @External
    public BigInteger redeem (
        Address depositor
    ) {
        var info = this.bondInfo.get(depositor);
        Context.require(info != null,
            "redeem: no bond registered for depositor");

        // (blocks since last interaction / vesting term remaining)
        BigInteger percentVested = percentVestedFor(depositor);

        // if fully vested
        if (percentVested.compareTo(FULLY_VESTED) >= 0) {
            // delete user info
            this.bondInfo.set(depositor, null);
            // emit bond data
            this.BondRedeemed(depositor, info.payout, ZERO);
            IToken.transfer(this.payoutToken, depositor, info.payout);
            return info.payout;
        } else {
            // if unfinished
            // calculate payout vested
            BigInteger fractionPayout = info.payout.multiply(percentVested).divide(FULLY_VESTED);
            long blockHeight = Context.getBlockHeight();

            // store updated deposit info
            BigInteger newPayout = info.payout.subtract(fractionPayout);
            bondInfo.set(depositor, new Bond(
                newPayout,
                info.vesting - (blockHeight - info.lastBlock),
                blockHeight,
                info.truePricePaid
            ));

            this.BondRedeemed(depositor, fractionPayout, newPayout);
            IToken.transfer(this.payoutToken, depositor, fractionPayout);
            return fractionPayout;
        }
    }

    // --- Internal help functions ---

    /**
     * Makes incremental adjustment to control variable
     */
    private void adjust() {
        var adjustment = this.adjustment.get();
        var terms = this.terms.get();
        long blockHeight = Context.getBlockHeight();

        long blockCanAdjust = adjustment.lastBlock + adjustment.buffer;

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

            this.ControlVariableAdjustment(initial, terms.controlVariable, adjustment.rate, adjustment.add);
        }
    }

    /**
     * Reduce total debt
     */
    private void decayDebt() {
        long blockHeight = Context.getBlockHeight();
        this.totalDebt.set(this.totalDebt.get().subtract(debtDecay()));
        this.lastDecay.set(blockHeight);
    }

    /**
     * Calculate current bond price and remove floor if above
     * @return price
     */
    private BigInteger _bondPrice() {
        var terms = this.terms.get();

        // price = BCV * debtRatio / (10**(IRC2(payoutToken).decimals()-DECIMALS_PRECISION))
        BigInteger price = terms.controlVariable.multiply(debtRatio()).divide(MathUtils.pow10(IToken.decimals(this.payoutToken) - DECIMALS_PRECISION_EXPONENT));

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
        Context.require(caller.equals(this.karmaDAO.get()),
            "checkKarmaDao: only KarmaDAO can call this method");
    }

    private void checkSubsidyRouter(Address caller) {
        Context.require(caller.equals(this.subsidyRouter),
            "checkSubsidy: only Subsidy Router can call this method");
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

        // price = BCV * debtRatio / (10**(IRC2(payoutToken).decimals()-DECIMALS_PRECISION))
        BigInteger price = terms.controlVariable.multiply(debtRatio()).divide(MathUtils.pow10(IToken.decimals(this.payoutToken) - DECIMALS_PRECISION_EXPONENT));
        BigInteger maxDiscount = terms.maxDiscount;

        if (price.compareTo(terms.minimumPrice) < 0) {
            price = terms.minimumPrice;
        }
        // check if max discount is greater than 0 and increase price to fit the capped discount
        // NOTE: if minimumPrice is set in the terms capped discount is not applied!
        else if (maxDiscount.compareTo(ZERO) > 0) {
            BigInteger bondDiscount = currentBondDiscount(price);

            // if bond discount is greater than max discount, increase bond price to fit the max discount
            if (bondDiscount.compareTo(maxDiscount) > 0) {
                BigInteger payoutTokenMarketPriceUSD = payoutTokenMarketPriceUSD();
                BigInteger newTrueBondPrice = payoutTokenMarketPriceUSD.multiply(MathUtils.pow10(3)).subtract(
                        maxDiscount.multiply(payoutTokenMarketPriceUSD)).multiply(MathUtils.pow10(4)).divide(this.lpMarketUsdPrice());
                BigInteger newBondPrice = newTrueBondPrice.multiply(TRUE_BOND_PRICE_PRECISION).divide(TRUE_BOND_PRICE_PRECISION.add(currentKarmaFee()));

                // only apply new bond price if it is higher than the old one, this should mitigate oracle risk
                // by defaulting to the un-capped bond price
                if (newBondPrice.compareTo(price) > 0) {
                    price = newBondPrice;
                }
            }
        }

        return price;
    }

    /**
     *  Calculate bond discount using market USD prices of principal and payout token
     *  @return BigInteger - Discount in percentages. Divide by 1e7 and multiply by 100% to convert in percentages
     *                       on client side.
     */
    @External(readonly = true)
    public BigInteger currentBondDiscount(
        @Optional BigInteger bondPrice
    ) {
        BigInteger bondPriceUSD = this.bondPriceUSD(bondPrice);
        BigInteger payoutTokenMarketPriceUSD = payoutTokenMarketPriceUSD();

        // discount = (payout token market price USD - bond price USD) / payout token market price USD
        // NOTE: result is in 1e7 decimal precision
        return (payoutTokenMarketPriceUSD.subtract(bondPriceUSD)).divide(payoutTokenMarketPriceUSD.divide(MathUtils.pow10(7)));
    }

    /**
     *  Payout token market USD price pulled from Karma Oracle
     */
    @External(readonly = true)
    public BigInteger payoutTokenMarketPriceUSD() {
        return IKarmaOracle.getUsdPrice(this.karmaOracle.get(), IToken.symbol(this.payoutToken));
    }

    /**
     *  Calculate bond price in USD
     */
    @External(readonly = true)
    public BigInteger bondPriceUSD(BigInteger bondPrice) {
        // NOTE: result is in 1e18 decimal precision
        return this.trueBondPrice(bondPrice).multiply(this.lpMarketUsdPrice()).divide(BOND_PRICE_PRECISION);
    }

    /**
     *  Calculate principal LP token market USD price, i.e. USD price of LP token
     */
    @External(readonly = true)
    public BigInteger lpMarketUsdPrice() {
        Map<String, ?> poolStats = IBalancedDEX.getPoolStats(this.principalToken, this.principalPoolId);

        // extract base, quote and total supply from pool stats
        BigInteger quoteDecimals = (BigInteger) poolStats.get("quote_decimals");
        BigInteger baseDecimals = (BigInteger) poolStats.get("base_decimals");
        int poolPrecision = baseDecimals.add(quoteDecimals).divide(BigInteger.TWO).intValue();
        BigInteger baseTokenReserveAmount = ((BigInteger) poolStats.get("base")).divide(MathUtils.pow10(baseDecimals.intValue()));
        BigInteger quoteTokenReserveAmount = ((BigInteger) poolStats.get("quote")).divide(MathUtils.pow10(quoteDecimals.intValue()));
        BigInteger poolTotalSupply = ((BigInteger) poolStats.get("total_supply")).divide(MathUtils.pow10(poolPrecision));
        Address baseToken = (Address) poolStats.get("base_token");
        Address quoteToken = (Address) poolStats.get("quote_token");

        Address karmaOracle = this.karmaOracle.get();
        BigInteger baseTokenMarketPrice = IKarmaOracle.getUsdPrice(karmaOracle, IToken.symbol(baseToken));
        BigInteger quoteTokenMarketPrice = IKarmaOracle.getUsdPrice(karmaOracle, IToken.symbol(quoteToken));

        BigInteger poolBaseTokenPriceUSD = this.poolTokenReservePrice(baseTokenReserveAmount, poolTotalSupply, baseTokenMarketPrice);
        BigInteger poolQuoteTokenPriceUSD = this.poolTokenReservePrice(quoteTokenReserveAmount, poolTotalSupply, quoteTokenMarketPrice);

        // USD price per Balanced LP token
        return poolBaseTokenPriceUSD.add(poolQuoteTokenPriceUSD);
    }


    /**
     * Calculate price of the token in pool
     */
    private BigInteger poolTokenReservePrice(BigInteger poolTokenReserveAmount, BigInteger poolTotalSupply,
                                             BigInteger tokenMarketPriceUSD) {
        return (poolTokenReserveAmount.divide(poolTotalSupply)).multiply(tokenMarketPriceUSD);
    }


    /**
     * Calculate true bond price a user pays
     */
    @External(readonly = true)
    public BigInteger trueBondPrice(@Optional BigInteger bondPrice) {
        if (bondPrice == null || bondPrice.equals(ZERO)) {
            // recursion only if no parameter given
            bondPrice = this.bondPrice();
        }
        // truePrice = `bondPrice()` + (`bondPrice()` * `currentKarmaFee()` / 10**TRUE_BOND_PRICE_PRECISION)
        return bondPrice.add(bondPrice.multiply(currentKarmaFee()).divide(TRUE_BOND_PRICE_PRECISION));
    }

    /**
     * Determine maximum bond size
     */
    @External(readonly = true)
    public BigInteger maxPayout() {
        // IRC2(payoutToken).totalSupply() * terms().maxPayout / 10**DECIMALS_PRECISION
        return IToken.totalSupply(this.payoutToken).multiply(this.terms.get().maxPayout).divide(DECIMALS_PRECISION);
    }

    /**
     * Calculate total interest due for new bond
     */
    private BigInteger _payoutFor (BigInteger value) {
        // total = value * 10**18 / bondPrice() / 10**TOTAL_PAYOUT_PRECISION
        return FullMath.mulDiv(value, EXA, bondPrice()).divide(TOTAL_PAYOUT_PRECISION);
    }

    /**
     * Calculate user's interest due for new bond, accounting for Karma Fee
     */
    @External(readonly = true)
    public BigInteger payoutFor (BigInteger value) {
        BigInteger total = _payoutFor(value);
        // payoutFor = total - (total * currentKarmaFee() / 10**PAYOUT_PRECISION)
        return total.subtract(total.multiply(currentKarmaFee()).divide(PAYOUT_PRECISION));
    }

    /**
     *  Calculate current ratio of debt to payout token supply
     *  Protocols using Karma Bond should be careful when quickly adding large %s to total supply
     */
    @External(readonly = true)
    public BigInteger debtRatio() {
        // debtRatio = currentDebt() * IRC2(payoutToken).decimals() / IRC2(payoutToken).totalSupply()
        return currentDebt().multiply(MathUtils.pow10(IToken.decimals(this.payoutToken))).divide(IToken.totalSupply(this.payoutToken));
    }

    /**
     * Calculate debt factoring in decay
     * Return value precision: amount of payout tokens / decimals(payout)
     */
    @External(readonly = true)
    public BigInteger currentDebt() {
        // currentDebt = totalDebt() - debtDecay()
        return this.totalDebt.get().subtract(debtDecay());
    }

    /**
     * Amount to decay total debt by
     * Return value precision: amount of payout tokens / decimals(payout)
     */
    @External(readonly = true)
    public BigInteger debtDecay()  {
        var terms = this.terms.get();
        Context.require(terms.vestingTerm != 0,
            "debtDecay: The vesting term must be initialized first");

        var totalDebt = this.totalDebt.get();
        long blockHeight = Context.getBlockHeight();
        BigInteger blocksSinceLast = BigInteger.valueOf(blockHeight - this.lastDecay.get());
        BigInteger vestingTerm = BigInteger.valueOf(terms.vestingTerm);
        // decay = totalDebt() * (blockHeight - lastDecay()) / (terms.vestingTerm)
        BigInteger decay = totalDebt.multiply(blocksSinceLast).divide(vestingTerm);

        if (decay.compareTo(totalDebt) > 0) {
            decay = totalDebt;
        }

        return decay;
    }

    /**
     * Calculate how far into vesting a depositor is
     * @param depositor The depositor address to calculate the vesting for
     */
    @External(readonly = true)
    public BigInteger percentVestedFor (
        Address depositor
    ) {
        Bond bond = bondInfo.get(depositor);
        Context.require(bond != null,
            "percentVestedFor: no bond registered for depositor");

        long blockHeight = Context.getBlockHeight();
        long blocksSinceLast = blockHeight - bond.lastBlock;
        long vesting = bond.vesting;

        return vesting > 0 
            ? BigInteger.valueOf(blocksSinceLast).multiply(FULLY_VESTED).divide(BigInteger.valueOf(vesting))
            : ZERO;
    }

    /**
     * Calculate amount of payout token available for claim by depositor
     * @param depositor The depositor address to calculate the payout token available for
     */
    @External(readonly = true)
    public BigInteger pendingPayoutFor (
        Address depositor
    ) {
        BigInteger percentVested = percentVestedFor (depositor);
        BigInteger payout = bondInfo.get(depositor).payout;

        return (percentVested.compareTo(FULLY_VESTED) >= 0)
            ? payout
            : payout.multiply(percentVested).divide(FULLY_VESTED);
    }

    /**
     * Get the current fee Karma takes of each bond
     */
    @External(readonly = true)
    public BigInteger currentKarmaFee() {
        int tierLength = this.feeTiers.size();
        var totalPrincipalBonded = this.totalPrincipalBonded.get();

        for (int i = 0; i < tierLength; i++) {
            var feeTier = this.feeTiers.get(i);

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

    @External(readonly = true)
    public Address karmaTreasury() {
        return this.karmaTreasury.get();
    }

    @External(readonly = true)
    public BigInteger totalPrincipalBonded() {
        return this.totalPrincipalBonded.get();
    }

    @External(readonly = true)
    public BigInteger totalPayoutGiven() {
        return this.totalPayoutGiven.get();
    }

    @External(readonly = true)
    public BigInteger totalDebt() {
        return this.totalDebt.get();
    }

    @External(readonly = true)
    public BigInteger payoutSinceLastSubsidy() {
        return this.payoutSinceLastSubsidy.get();
    }

    @External(readonly = true)
    public long lastDecay() {
        return this.lastDecay.get();
    }

    @External(readonly = true)
    public Terms terms() {
        return this.terms.get();
    }

    @External(readonly = true)
    public Adjust adjustment() {
        return this.adjustment.get();
    }

    @External(readonly = true)
    public FeeTiers feeTiers(int index) {
        return this.feeTiers.get(index);
    }

    @External(readonly = true)
    public Bond bondInfo(Address depositor) {
        return this.bondInfo.get(depositor);
    }

    @External(readonly = true)
    public Address principalToken () {
        return this.principalToken;
    }

    @External(readonly = true)
    public Address payoutToken () {
        return this.payoutToken;
    }

    @External(readonly = true)
    public BigInteger principalPoolId () {
        return this.principalPoolId;
    }

    @External(readonly = true)
    public Address customTreasury () {
        return this.customTreasury;
    }

    @External(readonly = true)
    public Address subsidyRouter () {
        return this.subsidyRouter;
    }
}
