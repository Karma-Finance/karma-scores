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

import dao.karma.structs.factorystorage.BondDetails;
import score.Address;

public class KarmaFactoryStorageClient {

  public static void pushBond (
    Score client, 
    Account from, 
    BondDetails details
  ) {
    client.invoke(from, "pushBond", details);
  }

  public static void setFactoryAddress (
    Score client, 
    Account from, 
    Address address
  ) {
    client.invoke(from, "setFactoryAddress", address);
  }

  public static int indexOfBond (
    Score client, 
    Address bondAddress
  ) {
    return ((BigInteger) client.call("indexOfBond", bondAddress)).intValue();
  }

  public static BondDetails bondDetails(Score score, int index) {
    return BondDetails.fromMap(score.call("bondDetails", index));
  }
}