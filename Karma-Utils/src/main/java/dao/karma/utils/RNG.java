/*
 * Copyright 2021 ICONation
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

package dao.karma.utils;

import java.math.BigInteger;

import score.Context;

public class RNG {
    /**
     * @return Get a 256-bits random byte array
     * We're limited to entropy available on chain from the transaction.
     * TODO: Use a VRF once it is implemented onchain.
     */
    private static byte[] getEntropy (byte[] userSeed) {
        return Context.hash("sha3-256",
            BytesUtils.concat(
                userSeed,
                Context.getTransactionHash(), // low
                Context.getTransactionNonce().toByteArray(), // low
                Context.getAddress().toByteArray(), // low
                Context.getCaller().toByteArray(), // low
                BigInteger.valueOf(Context.getTransactionIndex()).toByteArray(), // low
                BigInteger.valueOf(Context.getBlockTimestamp()).toByteArray() // medium
            )
        );
  }

  /**
   * @return Get a 256-bits random BigInteger
   */
  public static BigInteger randomInt (byte[] userSeed) {
      return new BigInteger(getEntropy(userSeed));
  }
}
