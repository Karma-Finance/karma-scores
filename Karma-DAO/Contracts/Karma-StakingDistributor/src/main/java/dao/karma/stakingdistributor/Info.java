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

package dao.karma.stakingdistributor;

import java.math.BigInteger;

import dao.karma.utils.AddressUtils;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

public class Info {
  // in ten-thousandths (5000 = 0.5%)
  public BigInteger rate;
  public Address recipient;

  public Info (BigInteger rate, Address recipient) {
    this.rate = rate;
    this.recipient = recipient;
  }

  public static Info readObject (ObjectReader r) {
    return new Info (
        r.readBigInteger(),
        r.readAddress()
    );
  }

  public static void writeObject (ObjectWriter w, Info obj) {
      w.write(obj.rate);
      w.write(obj.recipient);
  }

  public static Info empty() {
    return new Info(BigInteger.ZERO, AddressUtils.ZERO_ADDRESS);
  }
}