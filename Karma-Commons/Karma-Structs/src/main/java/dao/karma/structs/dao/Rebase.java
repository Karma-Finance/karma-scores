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

import java.math.BigInteger;

import score.ObjectReader;
import score.ObjectWriter;

public class Rebase {
  public BigInteger epoch;
  public BigInteger rebase; // 18 decimals
  public BigInteger totalStakedBefore;
  public BigInteger totalStakedAfter;
  public BigInteger amountRebased;
  public BigInteger index;
  public long blockNumberOccured;

  public Rebase (
    BigInteger epoch,
    BigInteger rebase,
    BigInteger totalStakedBefore,
    BigInteger totalStakedAfter,
    BigInteger amountRebased,
    BigInteger index,
    long blockNumberOccured
  ) {
    this.epoch = epoch;
    this.rebase = rebase;
    this.totalStakedBefore = totalStakedBefore;
    this.totalStakedAfter = totalStakedAfter;
    this.amountRebased = amountRebased;
    this.index = index;
    this.blockNumberOccured = blockNumberOccured;
  }

  public static Rebase readObject (ObjectReader r) {
      return new Rebase (
          r.readBigInteger(),
          r.readBigInteger(),
          r.readBigInteger(),
          r.readBigInteger(),
          r.readBigInteger(),
          r.readBigInteger(),
          r.readLong()
      );
  }

  public static void writeObject (ObjectWriter w, Rebase obj) {
      w.write(obj.epoch);
      w.write(obj.rebase);
      w.write(obj.totalStakedBefore);
      w.write(obj.totalStakedAfter);
      w.write(obj.amountRebased);
      w.write(obj.index);
      w.write(obj.blockNumberOccured);
  }
}
