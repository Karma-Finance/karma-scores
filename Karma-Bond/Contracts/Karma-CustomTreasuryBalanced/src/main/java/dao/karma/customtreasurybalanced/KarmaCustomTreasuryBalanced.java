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
import dao.karma.interfaces.irc2.IIRC2;

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
     * @param amountPayoutToken
     */
    // @External - this method is external through tokenFallback
    private void deposit (
        Address caller,
        Address principalTokenAddress, 
        BigInteger amountPrincipalToken,
        BigInteger poolIdPrincipalToken,
        BigInteger amountPayoutToken
    ) {
        Context.require(bondContract.getOrDefault(caller, false), 
            "deposit: caller is not a bond contract");

        IIRC2.transfer(this.payoutToken, caller, amountPayoutToken, JSONUtils.method("pay"));
    }

    @External
    public void onIRC31Received (Address _from, BigInteger _value, BigInteger _id, @Optional byte[] _data) {
        JsonObject root = JSONUtils.parseData(_data);
        String method = root.get("method").asString();
        Address token = Context.getCaller();

        switch (method)
        {
            case "deposit": {
                JsonObject params = root.get("params").asObject();
                BigInteger amountPayoutToken = StringUtils.toBigInt(params.get("amountPayoutToken").asString());
                deposit(_from, token, _value, _id, amountPayoutToken);
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

        IIRC2.transfer(token, destination, amount, JSONUtils.method("withdraw"));
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
        int payoutDecimals = IIRC2.decimals(payoutToken);
        // Principal token is a Balanced LP token, assume it is 18 decimals
        int principalDecimals = 18; // IIRC2.decimals(principalTokenAddress);

        return amount.multiply(MathUtils.pow10(payoutDecimals)).divide(MathUtils.pow10(principalDecimals));
    }
}
