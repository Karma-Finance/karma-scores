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

package dao.karma.custombond;

import java.math.BigInteger;

import score.ObjectReader;
import score.ObjectWriter;

// Info for incremental adjustments to control variable
public class Adjust {
    // addition or subtraction
    public boolean add;
    // increment
    public BigInteger rate;
    // BCV when adjustment finished
    public BigInteger target;
    // minimum length (in blocks) between adjustments
    public Long buffer;
    // block when last adjustment made
    public Long lastBlock;

    public Adjust (
        boolean add,
        BigInteger rate,
        BigInteger target,
        Long buffer,
        Long lastBlock
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
}