/*
 * Copyright 2021 ICONLOOP Inc.
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

public class Array256<V> extends Map256<BigInteger, V> {

    public Array256(String id, Class<V> valueClass) {
        super(id, BigInteger.class, valueClass);
    }

    public void push (V value) {
        BigInteger index = this.size();
        this.set(index, value);
    }

    @SuppressWarnings("unchecked")
    public V[] toArray() {
        Context.require(this.size().compareTo(IntUtils.MAX_INT32) <= 0, 
            "Array256::toArray: max size reached");

        int size = this.size().intValue();
        Object[] result = new Object[size];

        for (int i = 0; i < size; i++) {
            result[i] = this.get(BigInteger.valueOf(i));
        }

        return (V[]) result;
    }

    public void delete () {
        this.size.set(BigInteger.ZERO);
    }
}