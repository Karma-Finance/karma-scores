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

package dao.karma.subsidyrouter.client;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import score.Address;

public class KarmaSubsidyRouterClient {

  public static void addSubsidyController(Score client, Account from, Address subsidyController, Address bond) {
    client.invoke(from, "addSubsidyController", subsidyController, bond);
  }
  
  public static void removeSubsidyController(Score client, Account from, Address subsidyController) {
    client.invoke(from, "removeSubsidyController", subsidyController);
  }

  public static Address bondForController(Score client, Address subsidyController) {
    return (Address) client.call("bondForController", subsidyController);
  }

  public static void getSubsidyInfo (Score client, Account from) {
    client.invoke(from, "getSubsidyInfo");
  }
}