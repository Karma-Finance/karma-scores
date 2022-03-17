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

package dao.karma.customtreasurybalanced;

import java.math.BigInteger;

import com.eclipsesource.json.JsonObject;

import dao.karma.interfaces.bond.IBalancedDEX;
import dao.karma.interfaces.bond.IToken;

import dao.karma.utils.AddressUtils;
import dao.karma.utils.JSONUtils;
import dao.karma.utils.MathUtils;
import dao.karma.utils.StringUtils;
import dao.karma.types.Ownable;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class KarmaCustomTreasuryBalanced extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaCustomTreasuryBalanced";

    // Contract name
    private final String name;

    // The payout token contract address, token paid for principal
    private final Address payoutToken;

    // Bond contracts
    private final DictDB<Address, Boolean> bondContract = Context.newDictDB(NAME + "_bondContract", Boolean.class);

    // Principal token Pool ID
    private final BigInteger poolIdPrincipalToken;

    // ================================================
    // DB Variables
    // ================================================

    // ================================================
    // Event Logs
    // ================================================
    @EventLog
    public void BondContractToggled (
        Address bondContract, 
        boolean approved
    ) {}

    @EventLog
    public void Withdraw (
        Address token, 
        Address destination, 
        BigInteger amount
    ) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public KarmaCustomTreasuryBalanced (
        Address payoutToken, 
        Address initialOwner,
        BigInteger poolIdPrincipalToken
    ) {
        super(initialOwner);

        Context.require(!payoutToken.equals(AddressUtils.ZERO_ADDRESS));
        Context.require(!initialOwner.equals(AddressUtils.ZERO_ADDRESS));

        this.name = "Karma Custom Treasury";

        this.payoutToken = payoutToken;
        this.poolIdPrincipalToken = poolIdPrincipalToken;
    }

    // --- Bond Contract Functions ---

    /**
     * Deposit principal token and receive back payout token
     * 
     * Access: Everybody
     * 
     * @param principalTokenAddress
     * @param amountPrincipalToken
     * @param amountPayoutToken
     */
    // @External - this method is external through tokenFallback
    private void deposit (
        Address caller,
        Address principalTokenAddress, 
        BigInteger poolIdPrincipalToken,
        BigInteger amountPrincipalToken,
        BigInteger amountPayoutToken
    ) {
        Context.require(bondContract.getOrDefault(caller, false), 
            "deposit: caller is not a bond contract");

        Context.require(poolIdPrincipalToken.equals(this.poolIdPrincipalToken),
            "deposit: Wrong principal token pool ID");

        IToken.transfer(this.payoutToken, caller, amountPayoutToken, "pay");
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
                BigInteger amountPayoutToken = StringUtils.toBigInt(params.get("amountPayoutToken").asString());
                deposit(_operator, token, _id, _value, amountPayoutToken);
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
        // Address token = Context.getCaller();

        switch (method)
        {
            case "funding": {
                // accept funds from any address
                break;
            }

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
    }

    // --- Policy Functions ---
    /**
     * Policy can withdraw IRC2 token to desired address
     *
     * Access: Policy
     * 
     * @param token The token to withdraw
     * @param destination The destination address for the withdraw
     * @param amount The amount of tokens
     */
    @External
    public void withdraw (
        Address token,
        Address destination,
        BigInteger amount
    ) {
        // Access control
        onlyPolicy();

        IToken.transfer(token, destination, amount);
        this.Withdraw(token, destination, amount);
    }

    /**
     * Toggle a bond contract
     * 
     * Access: Policy
     * 
     * @param bondContract The bond contract to toggle
     */
    @External
    public void toggleBondContract (
        Address bondContract
    ) {
        // Access control
        onlyPolicy();

        // OK
        boolean state = this.bondContract.getOrDefault(bondContract, false);

        // toggle
        this.bondContract.set(bondContract, !state);
    }

    // ================================================
    // Checks
    // ================================================

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

    /**
     * Return the payout token address
     */
    @External(readonly = true)
    public Address payoutToken() {
        return this.payoutToken;
    }

    /**
     * Return the status of a given address in the bond contract whitelist
     * @param address Any address
     */
    @External(readonly = true)
    public boolean bondContract (Address address) {
        return this.bondContract.getOrDefault(address, false);
    }

    // ================================================
    // View Functions
    // ================================================
    private int getPrincipalDecimals (Address principalTokenAddress) {
        // The DEX address is the same than the token address
        var stats = IBalancedDEX.poolStats(principalTokenAddress, this.poolIdPrincipalToken);
        BigInteger base_decimals = (BigInteger) stats.get("base_decimals");
        BigInteger quote_decimals = (BigInteger) stats.get("quote_decimals");
        // The result is rounded down on purpose
        return base_decimals.add(quote_decimals).divide(BigInteger.TWO).intValue();
    }

    /**
     * Returns payout token valuation of principal token
     * 
     * @param principalTokenAddress The principal token address
     * @param amount An amount of principal token
     */
    @External(readonly = true)
    public BigInteger valueOfToken (
        Address principalTokenAddress,
        BigInteger amount
    ) {
        // Convert amount to match payout token decimals
        int payoutDecimals = IToken.decimals(payoutToken);

        // Get the principal token decimals
        int principalDecimals = getPrincipalDecimals(principalTokenAddress);

        return amount.multiply(MathUtils.pow10(payoutDecimals)).divide(MathUtils.pow10(principalDecimals));
    }

    /**
     * Returns the registered pool ID for the principal token
     */
    @External(readonly = true)
    public BigInteger poolIdPrincipalToken () {
        return this.poolIdPrincipalToken;
    }
}
