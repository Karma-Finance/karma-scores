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

package dao.karma.token;

import java.math.BigInteger;

import dao.karma.standards.token.irc2.IRC2Basic;
import dao.karma.types.IKarmaAccessControlled;
import dao.karma.types.KarmaAccessControlled;
import score.Address;
import score.Context;
import score.annotation.EventLog;
import score.annotation.External;

public class KarmaToken extends IRC2Basic implements IKarmaAccessControlled {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaToken";
    private final static int decimals = 9;

    // Implements KarmaAccessControlled
    private final KarmaAccessControlled accessControlled;

    // ================================================
    // DB Variables
    // ================================================

    // ================================================
    // Event Logs
    // ================================================

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaToken (
        Address authority
    ) {
        super("Karma Token", "KARMA", decimals);
        this.accessControlled = new KarmaAccessControlled(NAME + "_accessControlled", authority);
    }

    /**
     * Mint new tokens
     * 
     * Access: Vault
     * 
     * @param account
     * @param amount
     */
    @External
    public void mint (Address account, BigInteger amount) {
        // Access control
        this.accessControlled.onlyVault();

        // OK
        this._mint(account, amount);
    }

    /**
     * Burn tokens from the caller account
     * 
     * Access: Everyone
     * 
     * @param amount
     */
    @External
    public void burn (BigInteger amount) {
        this._burn (Context.getCaller(), amount);
    }

    // ================================================
    // Checks
    // ================================================

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
