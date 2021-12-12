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

package dao.karma.structs.dao;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import score.ObjectReader;
import score.ObjectWriter;

public class Claim {
  public BigInteger deposit;
  public BigInteger gons;
  public BigInteger expiry;
  // prevents malicious delays for claim
  public Boolean lock;

  public Claim (
    BigInteger deposit,
    BigInteger gons,
    BigInteger expiry,
    Boolean lock
  ) {
    this.deposit = deposit;
    this.gons = gons;
    this.expiry = expiry;
    this.lock = lock;
  }

  public static Claim readObject (ObjectReader r) {
      return new Claim (
          r.readBigInteger(),
          r.readBigInteger(),
          r.readBigInteger(),
          r.readBoolean()
      );
  }

  public static void writeObject (ObjectWriter w, Claim obj) {
      w.write(obj.deposit);
      w.write(obj.gons);
      w.write(obj.expiry);
      w.write(obj.lock);
  }

  public static Claim empty() {
    return new Claim (ZERO, ZERO, ZERO, false);
  }
}
