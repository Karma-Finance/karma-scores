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

package dao.karma.subsidyrouter;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;

import java.math.BigInteger;

import dao.karma.interfaces.bond.IBond;
import dao.karma.types.Ownable;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;

// Immutable contract routes between Karma bonds and subsidy controllers
// Allows for subsidies on bonds offered through bond contracts
public class KarmaSubsidyRouter extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaSubsidyRouter";

    // Contract name
    private final String name;

    // ================================================
    // DB Variables
    // ================================================
    // maps bond contract managed by subsidy controller
    private final DictDB<Address, Address> bondForController = Context.newDictDB(NAME + "_bondForController", Address.class);

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaSubsidyRouter (
    ) {
        this.name = "Karma Subsidy Router";
    }
    
    /**
     * Subsidy controller fetches and resets payout counter
     * 
     * Access: Everyone
     */
    @External
    public BigInteger getSubsidyInfo() {
        final Address caller = Context.getCaller();
        Address bond = this.bondForController.get(caller);

        Context.require (bond != null,
            "getSubsidyInfo: Address not mapped");

        return IBond.paySubsidy(bond);
    }

    /**
     * Add new subsidy controller for bond contract
     * 
     * Access: Policy
     * 
     * @param bond
     * @param subsidyController
     */
    @External
    public void addSubsidyController (Address bond, Address subsidyController) {
        // Access control
        onlyPolicy();

        Context.require (!bond.equals(ZERO_ADDRESS));
        Context.require (!subsidyController.equals(ZERO_ADDRESS));

        this.bondForController.set(subsidyController, bond);
    }
    
    /**
     * Remove subsidy controller for bond contract
     * 
     * Access: Policy
     * 
     * @param subsidyController address
     */
    @External
    public void removeSubsidyController (Address subsidyController) {
        // Access control
        onlyPolicy();

        this.bondForController.set(subsidyController, null);
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

    /**
     * Get the bond associated with a given controller
     * @param controller The controller
     * @return The bond
     */
    @External(readonly = true)
    public Address bondForController (Address controller) {
        return this.bondForController.get(controller);
    }
}
