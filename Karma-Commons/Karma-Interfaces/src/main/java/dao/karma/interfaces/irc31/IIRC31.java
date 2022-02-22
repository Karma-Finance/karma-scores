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

package dao.karma.interfaces.irc31;

import java.math.BigInteger;

import score.Address;
import score.Context;

public class IIRC31 {

  public static void transfer (
    Address irc31,
    Address to,
    BigInteger amount,
    BigInteger id,
    byte[] data
  ) {
    Context.call(irc31, "transfer", to, amount, id, data);
  }

  public static BigInteger totalSupply (Address irc31, BigInteger id) {
    return (BigInteger) Context.call(irc31, "totalSupply", id);
  }

  public static BigInteger balanceOf(Address irc31, Address address, BigInteger id) {
    return (BigInteger) Context.call(irc31, "balanceOf", address, id);
  }
}
