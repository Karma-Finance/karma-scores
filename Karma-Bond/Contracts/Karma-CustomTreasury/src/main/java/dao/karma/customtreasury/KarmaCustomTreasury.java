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

package dao.karma.customtreasury;

import java.math.BigInteger;

import com.eclipsesource.json.JsonObject;

import dao.karma.interfaces.bond.IToken;

import dao.karma.utils.AddressUtils;
import dao.karma.utils.ICX;
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
import score.annotation.Payable;

public class KarmaCustomTreasury extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaCustomTreasury";

    // Contract name
    private final String name;

    // The payout token contract address, token paid for principal
    private final Address payoutToken;

    // Bond contracts
    private final DictDB<Address, Boolean> bondContract = Context.newDictDB(NAME + "_bondContract", Boolean.class);

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
    public KarmaCustomTreasury (
        Address payoutToken, 
        Address initialOwner
    ) {
        super(initialOwner);

        Context.require(!payoutToken.equals(AddressUtils.ZERO_ADDRESS));
        Context.require(!initialOwner.equals(AddressUtils.ZERO_ADDRESS));

        this.name = "Karma Custom Treasury";

        this.payoutToken = payoutToken;
    }

    // --- Bond Contract Functions ---

    /**
     * Deposit principal token and receive back payout token
     * 
     * Access: Everybody
     * 
     * @param principalTokenAddress
     * @param amountPrincipalToken
     * @param amountPayoutToken Amount of payout token expected
     */
    // @External - this method is external through tokenFallback
    private void deposit (
        Address caller,
        Address principalTokenAddress, 
        BigInteger amountPrincipalToken,
        BigInteger amountPayoutToken
    ) {
        Context.require(bondContract.getOrDefault(caller, false), 
            "deposit: caller is not a bond contract");

        IToken.transfer(this.payoutToken, caller, amountPayoutToken, "pay");
    }

    /**
     * Deposit principal ICX token and receive back payout token
     * 
     * Access: Everybody
     * 
     * @param amountPayoutToken Amount of payout token expected
     */
    @External
    @Payable
    public void depositIcx (BigInteger amountPayoutToken) {
        final BigInteger value = Context.getValue();
        final Address token = ICX.TOKEN_ADDRESS;
        final Address caller = Context.getCaller();
        deposit(caller, token, value, amountPayoutToken);
    }

    /**
     * Funding of principal tokens, do *not* receive anything back
     * 
     * Access: Everybody
     */
    private void funding (
        Address caller,
        Address principalTokenAddress,
        BigInteger amountPrincipalToken
    ) {
        // accept funds from any address, nothing to do
    }

    /**
     * Funding of principal ICX tokens, do *not* receive anything back
     * 
     * Access: Everybody
     */
    @External
    @Payable
    public void fundingIcx () {
        final BigInteger value = Context.getValue();
        final Address token = ICX.TOKEN_ADDRESS;
        final Address caller = Context.getCaller();
        funding(caller, token, value);
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
                BigInteger amountPayoutToken = StringUtils.toBigInt(params.get("amountPayoutToken").asString());
                deposit(_from, token, _value, amountPayoutToken);
                break;
            }

            case "funding": {
                funding(_from, token, _value);
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
    /**
     * Returns payout token valuation of principal token
     * i.e. convert amount denominated in principal decimals to be denominated in payout decimals
     * 
     * @param principalTokenAddress The principal token address
     * @param amount An amount of principal token
     */
    @External(readonly = true)
    public BigInteger valueOfToken (
        Address principalTokenAddress,
        BigInteger amount
    ) {
        // convert amount to match payout token decimals
        int payoutDecimals = IToken.decimals(payoutToken);
        int principalDecimals = IToken.decimals(principalTokenAddress);

        return amount.multiply(MathUtils.pow10(payoutDecimals)).divide(MathUtils.pow10(principalDecimals));
    }
}
