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

package dao.karma.utils.librairies;

import static dao.karma.utils.IntUtils.uint144;
import static dao.karma.utils.IntUtils.uint224;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import score.Context;

public class FixedPoint {
  
  final static byte RESOLUTION = 112;
  final static private BigInteger Q112 = new BigInteger("10000000000000000000000000000", 16);

  public static uq112x112 fraction(BigInteger numerator, BigInteger denominator) {

    Context.require(denominator.compareTo(ZERO) > 0, 
      "FixedPoint::fraction: division by zero");

    if (numerator.equals(ZERO)) {
      return new uq112x112(ZERO);
    }

    BigInteger MINUS1 = BigInteger.valueOf(-1);

    BigInteger result = (numerator.compareTo(uint144(MINUS1)) <= 0) 
      ? numerator.shiftLeft(RESOLUTION).divide(denominator)
      : FullMath.mulDiv(numerator, Q112, denominator);

    Context.require(result.compareTo(uint224(MINUS1)) <= 0, 
      "FixedPoint::fraction: overflow");

    return new uq112x112(uint224(result));
  }
}
