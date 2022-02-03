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

// Info for incremental adjustments to control variable
public class Adjust {
    // Addition (true) or subtraction (false) of BCV
    public boolean add;
    // The increment value of the `controlVariable` value
    public BigInteger rate;
    // BCV when adjustment finished
    public BigInteger target;
    // minimum length (in blocks) between adjustments
    public long buffer;
    // block when last adjustment made
    public long lastBlock;

    public Adjust (
        boolean add,
        BigInteger rate,
        BigInteger target,
        long buffer,
        long lastBlock
    ) {
        this.add = add;
        this.rate = rate;
        this.target = target;
        this.buffer = buffer;
        this.lastBlock = lastBlock;
    }

    public static Adjust readObject (ObjectReader r) {
        return new Adjust (
            r.readBoolean(),
            r.readBigInteger(),
            r.readBigInteger(),
            r.readLong(),
            r.readLong()
        );
    }

    public static void writeObject (ObjectWriter w, Adjust obj) {
        w.write(obj.add);
        w.write(obj.rate);
        w.write(obj.target);
        w.write(obj.buffer);
        w.write(obj.lastBlock);
    }

    public static Adjust fromMap (Object call) {
        @SuppressWarnings("unchecked")
        Map<String,Object> map = (Map<String,Object>) call;
        return new Adjust (
            (boolean) map.get("add"),
            (BigInteger) map.get("rate"),
            (BigInteger) map.get("target"),
            ((BigInteger) map.get("buffer")).longValue(),
            ((BigInteger) map.get("lastBlock")).longValue()
        );
    }

    public static Adjust empty() {
      return new Adjust(false, ZERO, ZERO, 0L, 0L);
    }
}