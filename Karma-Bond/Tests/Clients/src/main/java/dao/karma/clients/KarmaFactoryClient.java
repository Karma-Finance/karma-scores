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

package dao.karma.clients;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import score.Address;

public class KarmaFactoryClient {

  public static void createBondAndTreasury(
    Score client, 
    Account from, 
    Address payoutToken, 
    Address principalToken,
    Address initialOwner,
    BigInteger[] tierCeilings,
    BigInteger[] fee
  ) {
    client.invoke(from, "createBondAndTreasury", payoutToken, principalToken, initialOwner, tierCeilings, fee);
  }

  public static void createBond (
    Score client, 
    Account from, 
    Address payoutToken, 
    Address principalToken, 
    Address customTreasury, 
    Address initialOwner, 
    BigInteger[] tierCeilings, 
    BigInteger[] fees
  ) {
    client.invoke (from, "createBond", payoutToken, principalToken, customTreasury, initialOwner, tierCeilings, fees);
  }
  
  public static void setCustomTreasuryContractBytes(Score client, Account from, byte[] content) {
    client.invoke(from, "setCustomTreasuryContractBytes", content);
  }

  public static void setCustomBondContractBytes(Score client, Account from, byte[] content) {
    client.invoke(from, "setCustomBondContractBytes", content);
  }
}