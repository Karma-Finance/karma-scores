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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import dao.karma.standards.token.irc2.client.IRC2Client;
import dao.karma.structs.bond.Adjust;
import dao.karma.structs.bond.Terms;
import dao.karma.utils.JSONUtils;
import score.Address;

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

  public static void setAdjustment (
    Score client, 
    Account from, 
    boolean addition, 
    BigInteger increment,
    BigInteger target,
    long buffer
  ) {
    client.invoke(from, "setAdjustment", addition, increment, target, buffer);
  }

  public static Adjust adjustment(Score client) {
    return Adjust.fromMap(client.call("adjustment"));
  }

  public static void changeKarmaTreasury (
    Score client, 
    Account from, 
    Address karmaTreasury
  ) {
    client.invoke(from, "changeKarmaTreasury", karmaTreasury);
  }

  public static void paySubsidy (
    Score client, 
    Account from
  ) {
    client.invoke(from, "paySubsidy");
  }

  public static BigInteger payoutSinceLastSubsidy(Score client) {
    return (BigInteger) client.call("payoutSinceLastSubsidy");
  }

  public static void deposit(
    Score client, 
    Account from, 
    Score principalToken, 
    BigInteger amount, 
    BigInteger maxPrice,
    Address depositor
  ) {
    JsonObject params = Json.object()
      .add("maxPrice", maxPrice.toString())
      .add("depositor", depositor.toString());

    IRC2Client.transfer(principalToken, from, client.getAddress(), amount, JSONUtils.method("deposit", params));
  }

  public static void pay (
    Score client,
    Account from,
    Score payoutToken,
    BigInteger amount
  ) {
    IRC2Client.transfer(payoutToken, from, client.getAddress(), amount, JSONUtils.method("pay"));
  }

  public static void redeem (
    Score client, 
    Account from, 
    Address address
  ) {
    client.invoke(from, "redeem", address);
  }

  public static Terms terms(Score client) {
    return Terms.fromMap(client.call("terms"));
  }
}