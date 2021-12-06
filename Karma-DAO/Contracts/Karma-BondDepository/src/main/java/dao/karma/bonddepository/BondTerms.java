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

package dao.karma.bonddepository;

import java.math.BigInteger;

// Bond Terms
public class BondTerms  {
  public BigInteger controlVariable;
  public Long vestingTerm;
  public BigInteger minimumPrice;
  public BigInteger maxPayout;
  public BigInteger maxDebt;

  public BondTerms () {}
  public BondTerms (
    BigInteger controlVariable,
    Long vestingTerm,
    BigInteger minimumPrice,
    BigInteger maxPayout,
    BigInteger maxDebt
  ) {
    this.controlVariable = controlVariable;
    this.vestingTerm = vestingTerm;
    this.minimumPrice = minimumPrice;
    this.maxPayout = maxPayout;
    this.maxDebt = maxDebt;
  }
}