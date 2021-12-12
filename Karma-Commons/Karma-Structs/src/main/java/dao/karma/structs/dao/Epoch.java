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

public class Epoch {
  public Long length;
  public BigInteger number;
  public Long endBlock;
  public BigInteger distribute;

  public Epoch (
    Long length,
    BigInteger number,
    Long endBlock,
    BigInteger distribute
  ) {
    this.length = length;
    this.number = number;
    this.endBlock = endBlock;
    this.distribute = distribute;
  }

  public static Epoch readObject (ObjectReader r) {
      return new Epoch (
          r.readLong(),
          r.readBigInteger(),
          r.readLong(),
          r.readBigInteger()
      );
  }

  public static void writeObject (ObjectWriter w, Epoch obj) {
      w.write(obj.length);
      w.write(obj.number);
      w.write(obj.endBlock);
      w.write(obj.distribute);
  }
}
