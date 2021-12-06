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

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import score.ObjectReader;
import score.ObjectWriter;

// Info for creating new bonds
public class Terms {
    // scaling variable for price
    public BigInteger controlVariable; 
    // fixed term or fixed expiration
    public boolean fixedTerm; 
    // term in blocks (fixed-term)
    public Long vestingTerm; 
    // block number bond matures (fixed-expiration)
    public Long expiration; 
    // block number bond no longer offered
    public Long conclusion; 
    // vs principal value
    public BigInteger minimumPrice; 
    // in thousandths of a %. i.e. 500 = 0.5%
    public BigInteger maxPayout; 
    // 9 decimal debt ratio, max % total supply created as debt
    public BigInteger maxDebt; 

    public Terms (
        BigInteger controlVariable,
        boolean fixedTerm,
        Long vestingTerm,
        Long expiration,
        Long conclusion,
        BigInteger minimumPrice,
        BigInteger maxPayout,
        BigInteger maxDebt
    ) {
        this.controlVariable = controlVariable;
        this.fixedTerm = fixedTerm;
        this.vestingTerm = vestingTerm;
        this.expiration = expiration;
        this.conclusion = conclusion;
        this.minimumPrice = minimumPrice;
        this.maxPayout = maxPayout;
        this.maxDebt = maxDebt;
    }
    
    public static Terms readObject (ObjectReader r) {
        return new Terms (
          r.readBigInteger(),
          r.readBoolean(),
          r.readLong(),
          r.readLong(),
          r.readLong(),
          r.readBigInteger(),
          r.readBigInteger(),
          r.readBigInteger()
        );
      }
  
      public static void writeObject (ObjectWriter w, Terms obj) {
        w.write(obj.controlVariable);
        w.write(obj.fixedTerm);
        w.write(obj.vestingTerm);
        w.write(obj.expiration);
        w.write(obj.conclusion);
        w.write(obj.minimumPrice);
        w.write(obj.maxPayout);
        w.write(obj.maxDebt);
      }

      public static Terms empty() {
        return new Terms(
          ZERO,
          false,
          0L,
          0L,
          0L,
          ZERO,
          ZERO,
          ZERO
        );
      }
}