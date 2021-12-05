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

package dao.karma.types;

import dao.karma.interfaces.dao.IKarmaAuthority;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;

public class KarmaAccessControlled {
  
    // ================================================
    // Consts
    // ================================================

    // ================================================
    // Event Logs
    // ================================================
    @EventLog(indexed = 1)
    public void AuthorityUpdated (Address authority) {}

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<Address> authority;

    public KarmaAccessControlled (String id, Address address) {
      this.authority = Context.newVarDB(id + "_authority", Address.class);

      if (this.authority.get() == null) {
        this.authority.set(address);
        this.AuthorityUpdated(address);
      }
    }

    // ================================================
    // Modifiers
    // ================================================
    public void onlyGovernor () {
      final Address caller = Context.getCaller();
      final Address governor = IKarmaAuthority.governor(this.authority.get());
      Context.require(caller.equals(governor),
        "onlyGovernor: Only governor can call this method");
    }

    public void onlyGuardian () {
      final Address caller = Context.getCaller();
      final Address guardian = IKarmaAuthority.guardian(this.authority.get());
      Context.require(caller.equals(guardian),
        "onlyGuardian: Only guardian can call this method");
    }

    public void onlyPolicy () {
      final Address caller = Context.getCaller();
      final Address policy = IKarmaAuthority.policy(this.authority.get());
      Context.require(caller.equals(policy),
        "onlyPolicy: Only policy can call this method");
    }

    public void onlyVault () {
      final Address caller = Context.getCaller();
      final Address vault = IKarmaAuthority.vault(this.authority.get());
      Context.require(caller.equals(vault),
        "onlyVault: Only vault can call this method");
    }

    // ================================================
    // Gov only
    // ================================================
    /**
     * Update the authority address
     * 
     * Access : Governor
     * 
     * @param newAuthority
     */
    public void setAuthority (Address newAuthority) {
      // Access control
      onlyGovernor();

      // OK
      this.authority.set(newAuthority);
      this.AuthorityUpdated(newAuthority);
    }
}
