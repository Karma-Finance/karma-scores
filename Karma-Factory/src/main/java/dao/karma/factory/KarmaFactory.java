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

package dao.karma.factory;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;

import java.math.BigInteger;

import dao.karma.interfaces.IKarmaFactoryStorage;
import dao.karma.utils.classes.TreasuryBond;
import dao.karma.utils.types.Ownable;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

public class KarmaFactory extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaFactory";

    // Contract name
    private final String name;

    // State variables
    private final Address karmaTreasury;
    private final Address karmaFactoryStorage;
    private final Address karmaSubsidyRouter;
    private final Address karmaDAO;

    // ================================================
    // DB Variables
    // ================================================
    // Custom Treasury Contract bytes to deploy by the factory
    VarDB<byte[]> customTreasuryContractBytes = Context.newVarDB(NAME + "_customTreasuryContractBytes", byte[].class);
    // Custom Bond Contract bytes to deploy by the factory
    VarDB<byte[]> customBondContractBytes = Context.newVarDB(NAME + "_customBondContractBytes", byte[].class);

    // ================================================
    // Event Logs
    // ================================================

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     *  Access: Everybody
     * 
     *  @param karmaTreasury The Karma Treasury contract address
     *  @param karmaFactoryStorage The Karma Factory Storage contract address
     *  @param karmaSubsidyRouter The karma Subsidy Router contract address
     *  @param karmaDAO The Karma DAO contract address
     */
    public KarmaFactory (
        Address karmaTreasury,
        Address karmaFactoryStorage,
        Address karmaSubsidyRouter,
        Address karmaDAO
    ) {
        this.name = "Karma Factory";

        Context.require(!karmaTreasury.equals(ZERO_ADDRESS),
            "KarmaFactory: karmaTreasury cannot be zero address");
        Context.require(!karmaFactoryStorage.equals(ZERO_ADDRESS),
            "KarmaFactory: karmaFactoryStorage cannot be zero address");
        Context.require(!karmaSubsidyRouter.equals(ZERO_ADDRESS),
            "KarmaFactory: karmaSubsidyRouter cannot be zero address");
        Context.require(!karmaDAO.equals(ZERO_ADDRESS),
            "KarmaFactory: karmaDAO cannot be zero address");

        this.karmaTreasury = karmaTreasury;
        this.karmaFactoryStorage = karmaFactoryStorage;
        this.karmaSubsidyRouter = karmaSubsidyRouter;
        this.karmaDAO = karmaDAO;
    }

    // ================================================
    // Contract Initialization
    // ================================================
    /**
     * Write the Custom Treasury Contract bytes. 
     * Can only be called once after deploying.
     * 
     * Access: SCORE Owner
     * 
     * @param contractBytes The contract bytes
     */
    @External
    public void setCustomTreasuryContractBytes (byte[] contractBytes) {
        final Address caller = Context.getCaller();

        // Access control
        checkIsOwner(caller);

        // The Custom Treasury Contract bytes cannot be set more than once
        checkCustomTreasuryContractBytesNotInitialized();
        
        // OK
        this.customTreasuryContractBytes.set(contractBytes);
    }

    /**
     * Write the Custom Treasury Contract bytes. 
     * Can only be called once after deploying.
     * 
     * Access: SCORE Owner
     * 
     * @param contractBytes The contract bytes
     */
    @External
    public void setCustomBondContractBytes (byte[] contractBytes) {
        final Address caller = Context.getCaller();

        // Access control
        checkIsOwner(caller);

        // The Custom Bond Contract bytes cannot be set more than once
        checkCustomBondContractBytesNotInitialized();

        // OK
        this.customBondContractBytes.set(contractBytes);
    }

    // --- Policy Functions --- 
    /**
     * Deploys custom treasury and custom bond contracts and returns address of both
     * 
     * @param payoutToken Address
     * @param principleToken Address
     * @param initialOwner Address
     * @return treasury Address
     * @return bond Address
     */
    @External
    public TreasuryBond createBondAndTreasury (
        Address payoutToken, 
        Address principleToken, 
        Address initialOwner, 
        BigInteger[] tierCeilings, 
        BigInteger[] fees
    ) {
        // Access control
        onlyPolicy();

        // Check contracts bytes initialized
        checkCustomTreasuryContractBytesInitialized();
        checkCustomBondContractBytesInitialized();

        // OK
        Address treasury = Context.deploy (
            customTreasuryContractBytes.get(), 
            payoutToken, 
            initialOwner
        );

        Address bond = Context.deploy (
            customBondContractBytes.get(), 
            treasury, 
            payoutToken, 
            principleToken, 
            this.karmaTreasury, 
            this.karmaSubsidyRouter, 
            initialOwner, 
            this.karmaDAO, 
            tierCeilings, 
            fees
        );

        return IKarmaFactoryStorage.pushBond (
            this.karmaFactoryStorage,
            payoutToken, 
            principleToken, 
            treasury, 
            bond,
            initialOwner,
            tierCeilings,
            fees
        );
    }
    
    /**
     * Deploys custom bond contract and returns address
     * 
     * @param payoutToken address
     * @param principleToken address
     * @param initialOwner address
     * @return treasury address
     * @return bond address
     */
    @External
    public TreasuryBond createBond (
        Address payoutToken, 
        Address principleToken, 
        Address customTreasury, 
        Address initialOwner, 
        BigInteger[] tierCeilings, 
        BigInteger[] fees
    ) {
        onlyPolicy();

        Address bond = Context.deploy (
            customBondContractBytes.get(), 
            customTreasury, 
            payoutToken, 
            principleToken, 
            this.karmaTreasury, 
            this.karmaSubsidyRouter, 
            initialOwner, 
            this.karmaDAO, 
            tierCeilings, 
            fees
        );

        return IKarmaFactoryStorage.pushBond (
            this.karmaFactoryStorage,
            payoutToken, 
            principleToken, 
            customTreasury, 
            bond,
            initialOwner,
            tierCeilings,
            fees
        );
    }

    // ================================================
    // Checks
    // ================================================
    private void checkIsOwner (Address user) {
        Context.require(user.equals(Context.getOwner()), 
            "checkIsOwner: Only owner can call this method");
    }

    /**
     * Check if the custom treasury contract bytes have been initialized
     */
    private void checkCustomTreasuryContractBytesInitialized() {
        Context.require(customTreasuryContractBytes.get() != null,
            "checkCustomTreasuryContractBytesInitialized: not set");
    }

    /**
     * Check if the custom treasury contract bytes have been initialized
     */
    private void checkCustomBondContractBytesInitialized() {
        Context.require(customBondContractBytes.get() != null,
            "checkCustomBondContractBytesInitialized: not set");
    }

    /**
     * Check if the custom treasury contract bytes haven't been initialized
     */
    private void checkCustomTreasuryContractBytesNotInitialized() {
        Context.require(this.customTreasuryContractBytes.get() == null,
            "checkCustomTreasuryContractBytesNotInitialized: already set");
    }

    /**
     * Check if the custom treasury contract bytes haven't been initialized
     */
    private void checkCustomBondContractBytesNotInitialized() {
        Context.require(this.customBondContractBytes.get() == null,
            "checkCustomBondContractBytesNotInitialized: already set");
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
        return this.karmaTreasury;
    }
    
    @External(readonly = true)
    public Address karmaFactoryStorage() {
        return this.karmaFactoryStorage;
    }
    
    @External(readonly = true)
    public Address karmaSubsidyRouter() {
        return this.karmaSubsidyRouter;
    }
    
    @External(readonly = true)
    public Address karmaDAO() {
        return this.karmaDAO;
    }
}
