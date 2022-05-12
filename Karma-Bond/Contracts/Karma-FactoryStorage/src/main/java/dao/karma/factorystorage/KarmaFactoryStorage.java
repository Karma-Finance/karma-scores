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

package dao.karma.factorystorage;

import dao.karma.structs.bond.TreasuryBond;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.types.Ownable;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public class KarmaFactoryStorage extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaFactoryStorage";

    // Contract name
    private final String name;

    // ================================================
    // DB Variables
    // ================================================
    private final ArrayDB<BondDetails> bondDetails = Context.newArrayDB(NAME + "_bondDetails", BondDetails.class);
    private final VarDB<Address> karmaFactory = Context.newVarDB(NAME + "_karmaFactory", Address.class);
    private final DictDB<Address, Integer> indexOfBond = Context.newDictDB(NAME + "_indexOfBond", Integer.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog
    public void BondCreation(
        Address treasury, 
        Address bond, 
        Address initialOwner
    ) {}

    
    @EventLog
    public void FactoryChanged (
        Address factory
    ) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaFactoryStorage () {
        super(Context.getCaller());
        this.name = "Karma Factory Storage";
    }

    /**
     * Pushes bond details to array
     * 
     * Access: Karma Factory
     * 
     * @param bond The bond details
     * @return The Treasury and the Bond addresses
     * @return bond
     */
    @External
    public TreasuryBond pushBond (
        BondDetails bond
    ) {
        final Address caller = Context.getCaller();

        // Access control
        checkIsKarmaFactory(caller);

        // OK
        indexOfBond.set(bond.bondAddress, bondDetails.size());
        bondDetails.add(bond);

        this.BondCreation(bond.treasuryAddress, bond.bondAddress, bond.initialOwner);
        return new TreasuryBond(bond.treasuryAddress, bond.bondAddress);
    }

    /**
     * Changes the Karma Factory address
     * 
     * Access: Policy
     * 
     * @param factory The new factory address
     */
    @External
    public void setFactoryAddress (
        Address factory
    ) {
        // Access control
        onlyPolicy();

        // OK
        this.karmaFactory.set(factory);
        this.FactoryChanged(factory);
    }

    // ================================================
    // Checks
    // ================================================
    private void checkIsKarmaFactory(Address caller) {
        Context.require(caller.equals(this.karmaFactory.get()), 
            "checkIsKarmaFactory: Only Karma Factory can call this method");
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
    public BondDetails bondDetails (int index) {
        return this.bondDetails.get(index);
    }

    @External(readonly = true)
    public Address karmaFactory() {
        return this.karmaFactory.get();
    }

    @External(readonly = true)
    public int indexOfBond (Address bond) {
        return this.indexOfBond.get(bond);
    }
}
