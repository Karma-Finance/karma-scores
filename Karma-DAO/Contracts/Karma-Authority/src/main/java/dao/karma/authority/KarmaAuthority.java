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

package dao.karma.authority;

import dao.karma.types.IKarmaAccessControlled;
import dao.karma.types.KarmaAccessControlled;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public class KarmaAuthority implements IKarmaAccessControlled {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaAuthority";

    // Contract name
    private final String name;

    // Implements KarmaAccessControlled
    private final KarmaAccessControlled accessControlled;

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<Address> governor = Context.newVarDB(NAME + "_governor", Address.class);
    private final VarDB<Address> guardian = Context.newVarDB(NAME + "_guardian", Address.class);
    private final VarDB<Address> policy = Context.newVarDB(NAME + "_policy", Address.class);
    private final VarDB<Address> vault = Context.newVarDB(NAME + "_vault", Address.class);
    private final VarDB<Address> newGovernor = Context.newVarDB(NAME + "_newGovernor", Address.class);
    private final VarDB<Address> newGuardian = Context.newVarDB(NAME + "_newGuardian", Address.class);
    private final VarDB<Address> newPolicy = Context.newVarDB(NAME + "_newPolicy", Address.class);
    private final VarDB<Address> newVault = Context.newVarDB(NAME + "_newVault", Address.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog(indexed = 2)
    public void GovernorPushed(Address from, Address to, boolean effectiveImmediately) {}
    @EventLog(indexed = 2)
    public void GuardianPushed(Address from, Address to, boolean effectiveImmediately) {}
    @EventLog(indexed = 2)
    public void PolicyPushed(Address from, Address to, boolean effectiveImmediately) {}
    @EventLog(indexed = 2)
    public void VaultPushed(Address from, Address to, boolean effectiveImmediately) {}

    @EventLog(indexed = 2)
    public void GovernorPulled(Address from, Address to) {}
    @EventLog(indexed = 2)
    public void GuardianPulled(Address from, Address to) {}
    @EventLog(indexed = 2)
    public void PolicyPulled(Address from, Address to) {}
    @EventLog(indexed = 2)
    public void VaultPulled(Address from, Address to) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaAuthority (
        Address governor,
        Address guardian,
        Address policy,
        Address vault
    ) {
        final Address thisAddress = Context.getAddress();
        this.name = "Karma Authority";

        this.accessControlled = new KarmaAccessControlled(NAME + "_accessControlled", thisAddress);

        if (this.governor.get() == null) {
            this.governor.set(governor);
            this.GovernorPushed(thisAddress, governor, true);
        }
        if (this.guardian.get() == null) {
            this.guardian.set(guardian);
            this.GuardianPushed(thisAddress, guardian, true);
        }
        if (this.policy.get() == null) {
            this.policy.set(policy);
            this.PolicyPushed(thisAddress, policy, true);
        }
        if (this.vault.get() == null) {
            this.vault.set(vault);
            this.VaultPushed(thisAddress, vault, true);
        }
    }

    // --- Gov only ---
    /**
     * Push a new governor
     * 
     * Access: Governor
     * 
     * @param newGovernor
     * @param effectiveImmediately
     */
    @External
    public void pushGovernor (
        Address newGovernor, 
        boolean effectiveImmediately
    ) {
        // Access control
        this.accessControlled.onlyGovernor();

        // OK
        if (effectiveImmediately) {
            this.governor.set(newGovernor);
        } else {
            this.newGovernor.set(newGovernor);
            this.GovernorPushed(this.governor.get(), newGovernor, effectiveImmediately);
        }
    }

    /**
     * Push a new guardian
     * 
     * Access: Governor
     * 
     * @param newGuardian
     * @param effectiveImmediately
     */
    @External
    public void pushGuardian (
        Address newGuardian, 
        boolean effectiveImmediately
    ) {
        // Access control
        this.accessControlled.onlyGovernor();

        // OK
        if (effectiveImmediately) {
            this.guardian.set(newGuardian);
        } else {
            this.newGuardian.set(newGuardian);
            this.GuardianPushed(this.guardian.get(), newGuardian, effectiveImmediately);
        }
    }

    /**
     * Push a new policy
     * 
     * Access: Governor
     * 
     * @param newPolicy
     * @param effectiveImmediately
     */
    @External
    public void pushPolicy (
        Address newPolicy, 
        boolean effectiveImmediately
    ) {
        // Access control
        this.accessControlled.onlyGovernor();

        // OK
        if (effectiveImmediately) {
            this.policy.set(newPolicy);
        } else {
            this.newPolicy.set(newPolicy);
            this.PolicyPushed(this.policy.get(), newPolicy, effectiveImmediately);
        }
    }

    /**
     * Push a new vault
     * 
     * Access: Governor
     * 
     * @param newVault
     * @param effectiveImmediately
     */
    @External
    public void pushVault (
        Address newVault, 
        boolean effectiveImmediately
    ) {
        // Access control
        this.accessControlled.onlyGovernor();

        // OK
        if (effectiveImmediately) {
            this.vault.set(newVault);
        } else {
            this.newVault.set(newVault);
            this.VaultPushed(this.vault.get(), newVault, effectiveImmediately);
        }
    }

    // --- Pending Role Only ---
    /**
     * Pull a pending governor
     * 
     * Access : New Governor
     */
    @External
    public void pullGovernor () {
        final Address caller = Context.getCaller();

        // Access control
        Context.require(caller.equals(this.newGovernor.get()),
            "pullGovernor: Only new governor can call this method");

        // OK
        this.GovernorPulled(this.governor(), this.newGovernor.get());
        this.governor.set(this.newGovernor.get());
        this.newGovernor.set(null);
    }

    /**
     * Pull a pending guardian
     * 
     * Access : New Guardian
     */
    @External
    public void pullGuardian () {
        final Address caller = Context.getCaller();

        // Access control
        Context.require(caller.equals(this.newGuardian.get()),
            "pullGuardian: Only new guardian can call this method");

        // OK
        this.GuardianPulled(this.guardian(), this.newGuardian.get());
        this.guardian.set(this.newGuardian.get());
        this.newGuardian.set(null);
    }

    /**
     * Pull a pending policy
     * 
     * Access : New Policy
     */
    @External
    public void pullPolicy () {
        final Address caller = Context.getCaller();

        // Access control
        Context.require(caller.equals(this.newPolicy.get()),
            "pullPolicy: Only new policy can call this method");

        // OK
        this.PolicyPulled(this.policy(), this.newPolicy.get());
        this.policy.set(this.newPolicy.get());
        this.newPolicy.set(null);
    }

    /**
     * Pull a pending vault
     * 
     * Access : New Vault
     */
    @External
    public void pullVault () {
        final Address caller = Context.getCaller();

        // Access control
        Context.require(caller.equals(this.newVault.get()),
            "pullVault: Only new vault can call this method");

        // OK
        this.VaultPulled(this.vault(), this.newVault.get());
        this.vault.set(this.newVault.get());
        this.newVault.set(null);
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
    public Address governor() {
        return this.governor.get();
    }

    @External(readonly = true)
    public Address guardian() {
        return this.guardian.get();
    }

    @External(readonly = true)
    public Address policy() {
        return this.policy.get();
    }

    @External(readonly = true)
    public Address vault() {
        return this.vault.get();
    }

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
