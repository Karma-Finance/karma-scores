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

package dao.karma.factorystorage;

import java.math.BigInteger;

import dao.karma.utils.classes.TreasuryBond;
import dao.karma.utils.types.Ownable;
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

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaFactoryStorage (
    ) {
        this.name = "Karma Factory Storage";
    }

    /**
     * Pushes bond details to array
     * 
     * Access: Karma Factory
     * 
     * @param payoutToken Address
     * @param principleToken Address
     * @param customTreasury Address
     * @param customBond Address
     * @param initialOwner Address
     * @param tierCeilings BigInteger[]
     * @param fees BigInteger[]
     * @return treasury Address
     * @return bond Address
     */
    @External
    public TreasuryBond pushBond (
        Address payoutToken, 
        Address principleToken, 
        Address customTreasury, 
        Address customBond, 
        Address initialOwner, 
        BigInteger[] tierCeilings, 
        BigInteger[] fees
    ) {
        final Address caller = Context.getCaller();

        // Access control
        checkIsKarmaFactory(caller);

        // OK
        indexOfBond.set(customBond, bondDetails.size());
        
        bondDetails.add (
            new BondDetails (
                payoutToken,
                principleToken,
                customTreasury,
                customBond,
                initialOwner,
                tierCeilings,
                fees
            )
        );

        this.BondCreation(customTreasury, customBond, initialOwner);
        return new TreasuryBond(customTreasury, customBond);
    }

    /**
     * Changes Karma Factory address
     * 
     * Access: Policy
     * 
     * @param factory address
     */
    @External
    public void setFactoryAddress (Address factory) {
        // Access control
        onlyPolicy();

        // OK
        this.karmaFactory.set(factory);
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
