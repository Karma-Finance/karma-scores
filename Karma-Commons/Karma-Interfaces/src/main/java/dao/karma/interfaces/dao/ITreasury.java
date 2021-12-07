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

import java.math.BigInteger;

import dao.karma.interfaces.irc2.IIRC2;
import dao.karma.utils.JSONUtils;
import score.Address;
import score.Context;

public abstract class ITreasury {
  public static BigInteger valueOfToken(Address treasury, Address principalToken, BigInteger amount) {
    return (BigInteger) Context.call(treasury, "valueOfToken", principalToken, amount);
  }

  public static void deposit(Address treasury, Address principalToken, BigInteger amount, BigInteger payout) {
    IIRC2.transfer(principalToken, treasury, amount, JSONUtils.method("deposit"));
  }

  public static BigInteger tokenValue(Address treasury, Address principal, BigInteger amount) {
    return (BigInteger) Context.call(treasury, "tokenValue", principal, amount);
  }

  public static void mint(Address treasury, Address to, BigInteger amount) {
    Context.call(treasury, "mint", to, amount);
  }
}
