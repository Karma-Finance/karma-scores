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

package dao.karma.custombond.client;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

public class KarmaCustomBondClient {

  public static void initializeBond (
    Score client, 
    Account from,
    BigInteger controlVariable,
    long vestingTerm,
    BigInteger minimumPrice,
    BigInteger maxPayout,
    BigInteger maxDebt,
    BigInteger initialDebt
  ) {
    client.invoke(from, "initializeBond", controlVariable, vestingTerm, minimumPrice, maxPayout, maxDebt, initialDebt);
  }

  public static void setBondTerms (
    Score client,
    Account from,
    int parameter,
    BigInteger input
  ) {
    client.invoke(from, "setBondTerms", parameter, input);
  }
}