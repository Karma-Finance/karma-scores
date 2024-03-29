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

public class MathUtils {
    public static final BigInteger EXA = MathUtils.pow10(18);

    public static BigInteger pow (BigInteger base, int exponent) {
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base);
        }
        return result;
    }

    public static BigInteger pow10 (int exponent) {
        return MathUtils.pow(BigInteger.TEN, exponent);
    }

    public static BigInteger min (BigInteger a, BigInteger b) {
        return a.compareTo(b) > 0 ? b : a;
    }
    public static BigInteger max (BigInteger a, BigInteger b) {
        return a.compareTo(b) > 0 ? a : b;
    }

    public static BigInteger sum (BigInteger[] array) {
        BigInteger result = BigInteger.ZERO;
        for (var cur : array) {
            result = result.add(cur);
        }
        return result;
    }

    public static BigInteger convertToExa (BigInteger amount, int decimals) {
      if (decimals == 18) {
        return amount;
      }

      if (decimals >= 0) {
        return amount.multiply(EXA).divide(MathUtils.pow10(decimals));
      }
      
      return null;
    }

    public static BigInteger exaMul(BigInteger a, BigInteger b) {
        BigInteger halfEXA = EXA.divide(BigInteger.TWO);
        return halfEXA.add(a.multiply(b)).divide(EXA);
    }
}
