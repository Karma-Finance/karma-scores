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

package dao.karma.bondteller;

import java.math.BigInteger;

import score.Address;

// Info for bond holder
public class Bond {
    // token used to pay for bond
    public Address principal;
    // amount of principal inflow token paid for bond
    public BigInteger principalPaid;
    // sOHM remaining to be paid. agnostic balance
    public BigInteger payout;
    // Block when bond is vested
    public Long vested;
    // time bond was created
    public BigInteger created;
    // time bond was redeemed
    public BigInteger redeemed;

    public Bond (
        Address principal,
        BigInteger principalPaid,
        BigInteger payout,
        Long vested,
        BigInteger created,
        BigInteger redeemed
    ) {
        this.principal = principal;
        this.principalPaid = principalPaid;
        this.payout = payout;
        this.vested = vested;
        this.created = created;
        this.redeemed = redeemed;
    }
}