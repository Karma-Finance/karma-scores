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

package dao.karma.custombondbalanced;

import java.math.BigInteger;

import score.ObjectReader;
import score.ObjectWriter;

public class FeeTiers {
    // principal bonded till next tier
    public BigInteger tierCeilings;
    // in ten-thousandths (i.e. 33300 = 3.33%)
    public BigInteger fees;

    public FeeTiers (
        BigInteger tierCeilings,
        BigInteger fees
    ) {
        this.tierCeilings = tierCeilings;
        this.fees = fees;
    }

    public static FeeTiers readObject (ObjectReader r) {
        return new FeeTiers(
            r.readBigInteger(),
            r.readBigInteger()
        );
    }

    public static void writeObject (ObjectWriter w, FeeTiers obj) {
        w.write(obj.tierCeilings);
        w.write(obj.fees);
    }
}