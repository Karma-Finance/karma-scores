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

package dao.karma.factory;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;

import java.math.BigInteger;

import dao.karma.interfaces.bond.IKarmaFactoryStorage;
import dao.karma.structs.bond.TreasuryBond;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.types.Ownable;
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
     * Contract constructor
     * By default, the Policy Role is set to the caller address.
     * 
     * Access: Everybody
     * 
     * @param karmaTreasury The Karma Treasury contract address
     * @param karmaFactoryStorage The Karma Factory Storage contract address
     * @param karmaSubsidyRouter The karma Subsidy Router contract address
     * @param karmaDAO The Karma DAO contract address
     */
    public KarmaFactory (
        Address karmaTreasury,
        Address karmaFactoryStorage,
        Address karmaSubsidyRouter,
        Address karmaDAO
    ) {
        super(Context.getCaller());

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
     * Set the Custom Treasury Contract bytes. 
     * Can only be called once after deploying.
     * 
     * Access: SCORE Owner
     * 
     * @param contractBytes The treasury contract bytes
     */
    @External
    public void setCustomTreasuryContractBytes (
        byte[] contractBytes
    ) {
        final Address caller = Context.getCaller();

        // Access control
        checkIsOwner(caller);

        // The Custom Treasury Contract bytes cannot be set more than once
        checkCustomTreasuryContractBytesNotInitialized();

        // OK
        this.customTreasuryContractBytes.set(contractBytes);
    }

    /**
     * Write the Custom Bond Contract bytes. 
     * Can only be called once after deploying.
     * 
     * Access: SCORE Owner
     * 
     * @param contractBytes The bond contract bytes
     */
    @External
    public void setCustomBondContractBytes (
        byte[] contractBytes
    ) {
        final Address caller = Context.getCaller();

        // Access control
        checkIsOwner(caller);

        // The Custom Bond Contract bytes cannot be set more than once
        checkCustomBondContractBytesNotInitialized();

        // OK
        this.customBondContractBytes.set(contractBytes);
    }

    // --- Policy Role Functions --- 
    /**
     * Deploys custom treasury and custom bond contracts and returns address of both
     * 
     * Access: Policy
     * 
     * @param payoutToken The payout token address associated with the bond, token paid for principal
     * @param principalToken The principal inflow token address associated with the bond
     * @param initialOwner The initial owner of the bond
     * @param tierCeilings Array of ceilings of principal bonded till next tier
     * @param fees Array of fees tiers, in ten-thousandths (i.e. 33300 = 3.33%)
     * 
     * @return Addresses of newly deployed bond and treasury contracts
     */
    @External
    public TreasuryBond createBondAndTreasury (
        Address payoutToken, 
        Address principalToken, 
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
            principalToken,
            this.karmaTreasury,
            this.karmaSubsidyRouter,
            initialOwner,
            this.karmaDAO,
            tierCeilings,
            fees
        );

        BondDetails details = new BondDetails (
            payoutToken, 
            principalToken, 
            treasury, 
            bond,
            initialOwner,
            tierCeilings,
            fees
        );

        return IKarmaFactoryStorage.pushBond (this.karmaFactoryStorage, details);
 
 }

    /**
     * Deploys custom bond contract and returns address
     * 
     * Access: Policy
     * 
     * @param payoutToken The payout token
     * @param principleToken The principle token
     * @param customTreasury The Custom Treasury address associated with the bond
     * @param initialOwner The initial owner of the bond
     * @param tierCeilings 
     * @param fees 
     * @return The bond address 
     */
    @External
    public TreasuryBond createBond (
        Address payoutToken, 
        Address principalToken, 
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
            principalToken, 
            this.karmaTreasury, 
            this.karmaSubsidyRouter, 
            initialOwner, 
            this.karmaDAO, 
            tierCeilings, 
            fees
        );

        BondDetails details = new BondDetails (
            payoutToken, 
            principalToken, 
            customTreasury, 
            bond,
            initialOwner,
            tierCeilings,
            fees
        );

        return IKarmaFactoryStorage.pushBond (this.karmaFactoryStorage, details);
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
