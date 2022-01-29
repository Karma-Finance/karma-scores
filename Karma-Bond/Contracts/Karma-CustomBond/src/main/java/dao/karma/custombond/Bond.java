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

// Info for bond holder
public class Bond {
    // payout token remaining to be paid
    public BigInteger payout;
    // Blocks left to vest
    public long vesting;
    // Last interaction
    public long lastBlock;
    // Price paid (principal tokens per payout token) in ten-millionths - 4000000 = 0.4
    public BigInteger truePricePaid;
    
    public Bond (
        BigInteger payout,
        long vesting,
        long lastBlock,
        BigInteger truePricePaid
    ) {
        this.payout = payout;
        this.vesting = vesting;
        this.lastBlock = lastBlock;
        this.truePricePaid = truePricePaid;
    }

    public static Bond readObject (ObjectReader r) {
        return new Bond(
            r.readBigInteger(),
            r.readLong(),
            r.readLong(),
            r.readBigInteger()
        );
    }

    public static void writeObject (ObjectWriter w, Bond obj) {
        w.write(obj.payout);
        w.write(obj.vesting);
        w.write(obj.lastBlock);
        w.write(obj.truePricePaid);
    }
}