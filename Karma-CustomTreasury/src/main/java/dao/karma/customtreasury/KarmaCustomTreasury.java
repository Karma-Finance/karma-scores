/*
 * Copyright 2021 KarmaDAO
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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import dao.karma.interfaces.IIRC2;
import dao.karma.utils.AddressUtils;
import dao.karma.utils.JSONUtils;
import dao.karma.utils.MathUtils;
import dao.karma.utils.StringUtils;
import dao.karma.utils.types.Ownable;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.io.IOException;
import scorex.io.Reader;
import scorex.io.StringReader;

public class KarmaCustomTreasury extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaCustomTreasury";

    // Contract name
    private final String name;

    // The payout token contract address
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
        Context.require(!payoutToken.equals(AddressUtils.ZERO_ADDRESS));
        Context.require(!initialOwner.equals(AddressUtils.ZERO_ADDRESS));

        this.name = "Karma Custom Treasury";

        this.payoutToken = payoutToken;
        this.policy.set(initialOwner);
    }

    // --- Bond Contract Functions ---
    
    /**
     * Deposit principle token and recieve back payout token
     * 
     * Access: Everybody
     * 
     * @param principleTokenAddress
     * @param amountPrincipleToken
     * @param amountPayoutToken
     */
    // @External - this method is external through tokenFallback
    private void deposit (
        Address caller,
        Address principleTokenAddress, 
        BigInteger amountPrincipleToken,
        BigInteger amountPayoutToken
    ) {
        Context.require(bondContract.getOrDefault(caller, false), 
            "deposit: caller is not a bond contract");

        IIRC2.transfer(payoutToken, caller, amountPayoutToken, JSONUtils.method("deposit"));
    }
    
    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) {
        Reader reader = new StringReader(new String(_data));
        JsonValue input = null;
        try {
            input = Json.parse(reader);
        } catch (IOException e) {
            Context.revert("tokenFallback: Invalid JSON");
        }
        JsonObject root = input.asObject();
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

    @External(readonly = true)
    public Address payoutToken() {
        return this.payoutToken;
    }
    
    // ================================================
    // View Functions
    // ================================================
    /**
     * Returns payout token valuation of principle
     * 
     * @param principleTokenAddress
     * @param amount
     * @return value
     */
    @External(readonly = true)
    public BigInteger valueOfToken (
        Address principleTokenAddress,
        BigInteger amount
    ) {
        int payoutTokenDecimals = IIRC2.decimals(payoutToken);
        int principleTokenAddressDecimals = IIRC2.decimals(principleTokenAddress);

        return amount.multiply(MathUtils.pow10(payoutTokenDecimals)).divide(MathUtils.pow10(principleTokenAddressDecimals));
    }

}
