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

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

// Info about each type of bond
public class Bond {
    // token to accept as payment
    public Address principal;
    // contract to value principal
    public Address calculator;
    // terms of bond
    public Terms terms;
    // have terms been set
    public boolean termsSet;
    // capacity remaining
    public BigInteger capacity;
    // capacity limit is for payout vs principal
    public boolean capacityIsPayout;
    // total debt from bond
    public BigInteger totalDebt;
    // last block when debt was decayed
    public long lastDecay;

    public Bond (
      Address principal,
      Address calculator,
      Terms terms,
      boolean termsSet,
      BigInteger capacity,
      boolean capacityIsPayout,
      BigInteger totalDebt,
      long lastDecay
    ) {
      this.principal = principal;
      this.calculator = calculator;
      this.terms = terms;
      this.termsSet = termsSet;
      this.capacity = capacity;
      this.capacityIsPayout = capacityIsPayout;
      this.totalDebt = totalDebt;
      this.lastDecay = lastDecay;
    }

    public static Bond readObject (ObjectReader r) {
      return new Bond (
        r.readAddress(),
        r.readAddress(),
        r.read(Terms.class),
        r.readBoolean(),
        r.readBigInteger(),
        r.readBoolean(),
        r.readBigInteger(),
        r.readLong()
      );
    }

    public static void writeObject (ObjectWriter w, Bond obj) {
      w.write(obj.principal);
      w.write(obj.calculator);
      w.write(obj.terms);
      w.write(obj.termsSet);
      w.write(obj.capacity);
      w.write(obj.capacityIsPayout);
      w.write(obj.totalDebt);
      w.write(obj.lastDecay);
    }

    public static Bond empty() {
      return new Bond (
        ZERO_ADDRESS, 
        ZERO_ADDRESS, 
        Terms.empty(), 
        false, 
        ZERO, 
        false, 
        ZERO, 
        0L
      );
    }
}