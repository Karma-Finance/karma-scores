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

import score.Address;
import score.Context;

public abstract class IgKARMA {

  public static BigInteger balanceTo (Address gKARMA, BigInteger amount) {
    return (BigInteger) Context.call (gKARMA, "balanceTo", amount);
  }

  public static BigInteger balanceFrom (Address gKARMA, BigInteger amount) {
    return (BigInteger) Context.call (gKARMA, "balanceFrom", amount);
  }

  public static void burn (Address gKARMA, BigInteger amount) {
    Context.call (gKARMA, "burn", amount);
  }

  public static void mint (Address gKARMA, Address to, BigInteger gBalance) {
    Context.call (gKARMA, "mint", to, gBalance);
  }
}
