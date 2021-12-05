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

public abstract class KarmaAccessControlled {
  
    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaAccessControlled";

    // ================================================
    // Event Logs
    // ================================================
    @EventLog(indexed = 1)
    public void AuthorityUpdated (Address authority) {}

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<Address> authority = Context.newVarDB(NAME + "_authority", Address.class);

    public KarmaAccessControlled (Address address) {
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
      Context.require(caller.equals(IKarmaAuthority.governor(this.authority.get())));
    }
}
