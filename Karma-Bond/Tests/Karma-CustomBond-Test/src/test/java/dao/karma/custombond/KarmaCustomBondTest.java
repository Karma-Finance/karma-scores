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

package dao.karma.custombond;

import java.math.BigInteger;

import dao.karma.test.KarmaTest;
import dao.karma.test.ScoreSpy;
import score.Address;

public class KarmaCustomBondTest extends KarmaTest {

  ScoreSpy<KarmaCustomBond> bond;

  void setup_bond (
    Address customTreasury,
    Address payoutToken,
    Address principalToken,
    Address karmaTreasury,
    Address karmaOracle,
    Address subsidyRouter,
    Address initialOwner,
    Address karmaDAO,
    BigInteger[] tierCeilings,
    BigInteger[] fees
  ) throws Exception {
    bond = deploy (
      KarmaCustomBond.class,
      customTreasury,
      payoutToken,
      principalToken,
      karmaTreasury,
      karmaOracle,
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }
}