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

package dao.karma.structs.factorystorage;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

public class BondDetails {
    public Address payoutToken;
    public Address principalToken;
    public Address treasuryAddress;
    public Address bondAddress;
    public Address initialOwner;
    public BigInteger[] tierCeilings;
    public BigInteger[] fees;

    public BondDetails (
        Address payoutToken, 
        Address principalToken, 
        Address treasuryAddress, 
        Address bondAddress,
        Address initialOwner,
        BigInteger[] tierCeilings,
        BigInteger[] fees
    ) {
        this.payoutToken = payoutToken;
        this.principalToken = principalToken;
        this.treasuryAddress = treasuryAddress;
        this.bondAddress = bondAddress;
        this.initialOwner = initialOwner;
        this.tierCeilings = tierCeilings;
        this.fees = fees;
    }
    
    public static BondDetails readObject (ObjectReader r) {
        Address payoutToken = r.readAddress();
        Address principalToken = r.readAddress();
        Address treasuryAddress = r.readAddress();
        Address bondAddress = r.readAddress();
        Address initialOwner = r.readAddress();

        ArrayList<BigInteger> tierCeilings = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            tierCeilings.add(r.readBigInteger());
        }
        r.end();

        ArrayList<BigInteger> fees = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            fees.add(r.readBigInteger());
        }
        r.end();

        return new BondDetails (
            payoutToken,
            principalToken,
            treasuryAddress,
            bondAddress,
            initialOwner,
            Arrays.copyOfRange(tierCeilings.toArray(), 0, tierCeilings.size(), BigInteger[].class),
            Arrays.copyOfRange(fees.toArray(), 0, fees.size(), BigInteger[].class)
        );
    }

    public static void writeObject (ObjectWriter w, BondDetails obj) {
        w.write(obj.payoutToken);
        w.write(obj.principalToken);
        w.write(obj.treasuryAddress);
        w.write(obj.bondAddress);
        w.write(obj.initialOwner);
        w.beginList(obj.tierCeilings.length);
        for (int i = 0; i < obj.tierCeilings.length; i++) {
            w.write(obj.tierCeilings[i]);
        }
        w.end();
        w.beginList(obj.fees.length);
        for (int i = 0; i < obj.fees.length; i++) {
            w.write(obj.fees[i]);
        }
        w.end();
    }

    public static BondDetails fromMap(Object call) {
        @SuppressWarnings("unchecked")
        Map<String,Object> map = (Map<String,Object>) call;
        return new BondDetails(
            (Address) map.get("payoutToken"), 
            (Address) map.get("principalToken"), 
            (Address) map.get("treasuryAddress"), 
            (Address) map.get("bondAddress"), 
            (Address) map.get("initialOwner"),
            (BigInteger[]) map.get("tierCeilings"),
            (BigInteger[]) map.get("fees")
        );
    }
}