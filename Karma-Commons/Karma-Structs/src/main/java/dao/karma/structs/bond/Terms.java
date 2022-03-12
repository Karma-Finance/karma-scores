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

package dao.karma.structs.bond;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.Map;

import score.ObjectReader;
import score.ObjectWriter;

// Info for creating new bonds
public class Terms {
    // scaling variable for price
    public BigInteger controlVariable;
    // in blocks
    public long vestingTerm;
    // vs principal value
    public BigInteger minimumPrice;
    // in thousandths of a %. i.e. 500 = 0.5%
    public BigInteger maxPayout;
    // payout token decimal debt ratio, max % total supply created as debt
    public BigInteger maxDebt;

    public Terms (
        BigInteger controlVariable,
        long vestingTerm,
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

    public static Terms readObject (ObjectReader r) {
        return new Terms(
            r.readBigInteger(),
            r.readLong(),
            r.readBigInteger(),
            r.readBigInteger(),
            r.readBigInteger()
        );
    }

    public static void writeObject (ObjectWriter w, Terms obj) {
        w.write(obj.controlVariable);
        w.write(obj.vestingTerm);
        w.write(obj.minimumPrice);
        w.write(obj.maxPayout);
        w.write(obj.maxDebt);
    }

    public static Terms empty() {
        return new Terms(ZERO, 0L, ZERO, ZERO, ZERO);
    }

    public static Terms fromMap(Object call) {
        @SuppressWarnings("unchecked")
        Map<String,Object> map = (Map<String,Object>) call;
        return new Terms (
            (BigInteger) map.get("controlVariable"),
            ((BigInteger) map.get("vestingTerm")).longValue(),
            (BigInteger) map.get("minimumPrice"),
            (BigInteger) map.get("maxPayout"),
            (BigInteger) map.get("maxDebt")
        );
    }
}