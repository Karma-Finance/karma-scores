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

package dao.karma.interfaces.dao;

import score.Address;
import score.Context;

public abstract class IKarmaAuthority {
  public static Address governor(Address authority) {
    return (Address) Context.call(authority, "governor");
  }

  public static Address guardian(Address authority) {
    return (Address) Context.call(authority, "guardian");
  }

  public static Address policy(Address authority) {
    return (Address) Context.call(authority, "policy");
  }

  public static Address vault(Address authority) {
    return (Address) Context.call(authority, "vault");
  }
}
