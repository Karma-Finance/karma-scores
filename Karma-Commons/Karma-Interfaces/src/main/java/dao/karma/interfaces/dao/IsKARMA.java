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

public abstract class IsKARMA {

  public static BigInteger toG (Address sKARMA, BigInteger payout) {
    return (BigInteger) Context.call (sKARMA, "toG", payout);
  }

  public static BigInteger fromG (Address sKARMA, BigInteger dues) {
    return (BigInteger) Context.call (sKARMA, "fromG", dues);
  }

  public static BigInteger gonsForBalance (Address sKARMA, BigInteger amount) {
    return (BigInteger) Context.call (sKARMA, "gonsForBalance", amount);
  }

  public static BigInteger balanceForGons (Address sKARMA, BigInteger gons) {
    return (BigInteger) Context.call (sKARMA, "balanceForGons", gons);
  }

  public static void rebase (Address sKARMA, BigInteger distribute, BigInteger number) {
    Context.call (sKARMA, "rebase", distribute, number);
  }

  public static BigInteger index (Address sKARMA) {
    return (BigInteger) Context.call (sKARMA, "index");
  }

  public static BigInteger circulatingSupply(Address sKARMA) {
    return (BigInteger) Context.call (sKARMA, "circulatingSupply");
  }
}
