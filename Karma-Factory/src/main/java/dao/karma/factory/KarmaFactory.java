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

import dao.karma.interfaces.IOlympusProFactoryStorage;
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
     *  @param karmaTreasury The Karma Treasury contract address
     *  @param karmaFactoryStorage The Karma Factory Storage contract address
     *  @param karmaSubsidyRouter The karma Subsidy Router contract address
     *  @param karmaDAO The Karma DAO contract address
     */
    public KarmaFactory (
        Address karmaTreasury,       // 0x31F8Cc382c9898b273eff4e0b7626a6987C846E8
        Address karmaFactoryStorage, // 0x6828D71014D797533C3b49B6990Ca1781656B71f
        Address karmaSubsidyRouter,  // 0x97Fac4EA361338EaB5c89792eE196DA8712C9a4a
        Address karmaDAO             // Proxy -> 0x34cfac646f301356faa8b21e94227e3583fe3f5f
    ) {
        this.name = "KarmaDAO Factory";

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
    @External
    public void setCustomTreasuryContractBytes (byte[] contract) {
        final Address caller = Context.getCaller();

        checkIsOwner(caller);

        // The Custom Treasury Contract bytes cannot be set more than once
        Context.require(this.customTreasuryContractBytes.get() == null,
            "setCustomTreasuryContractBytes: already set");
    }
    
    @External
    public void setCustomBondContractBytes (byte[] contract) {
        final Address caller = Context.getCaller();

        checkIsOwner(caller);

        // The Custom Bond Contract bytes cannot be set more than once
        Context.require(this.customBondContractBytes.get() == null,
            "setCustomBondContractBytes: already set");
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
        onlyPolicy();

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

        return IOlympusProFactoryStorage.pushBond (
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

        return IOlympusProFactoryStorage.pushBond (
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
}
