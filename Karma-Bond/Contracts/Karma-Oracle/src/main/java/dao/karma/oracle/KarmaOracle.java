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

package dao.karma.oracle;

import static dao.karma.utils.MathUtils.EXA;
import static java.math.BigInteger.ZERO;
import java.math.BigInteger;
import java.util.Map;
import dao.karma.types.Ownable;
import dao.karma.utils.ArrayUtils;
import dao.karma.utils.MathUtils;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public class KarmaOracle extends Ownable {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaOracle";

    // Contract name
    private final String name;

    // List of stablecoins - the price for these tokens will always be evaluated at 1$ whatever happens
    private static final String[] STABLE_TOKENS = {
        "USDS", 
        "USDB", 
        "bnUSD", 
        "IUSDC", 
        "IUSDT"
    };

    private static final OmmToken[] OMM_TOKENS = {
        new OmmToken("USDS", "USDS"),
        new OmmToken("sICX", "ICX"),
        new OmmToken("IUSDC", "USDC"),
    };

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<Address> balancedDex = Context.newVarDB(NAME + "_balancedDex", Address.class);
    private final VarDB<Address> bandOracle = Context.newVarDB(NAME + "_bandOracle", Address.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog
    public void AddressChanged(Address newAddress) {}

    // ================================================
    // Methods
    // ================================================
    /**
     * Contract constructor
     * 
     */
    public KarmaOracle (
        Address initialOwner,
        Address balancedDex,
        Address bandOracle
    ) {
        super(initialOwner);

        // OK
        this.name = "Karma Price Oracle";

        // Default initialization
        if (this.balancedDex.get() == null) {
            this.balancedDex.set(balancedDex);
        }

        if (this.bandOracle.get() == null) {
            this.bandOracle.set(bandOracle);
        }
    }

    // --- Only Policy methods ---
    @External
    public void setBalancedDex (Address value) {
        // Access control
        this.onlyPolicy();

        // OK
        this.balancedDex.set(value);
        this.AddressChanged(value);
    }

    @External
    public void setBandOracle (Address value) {
        // Access control
        this.onlyPolicy();

        // OK
        this.bandOracle.set(value);
        this.AddressChanged(value);
    }

    // ================================================
    // Private methods
    // ================================================
    private BigInteger getPrice (String base, String quote) {
        // Special case for whitelisted stablecoins
        if (ArrayUtils.contains(STABLE_TOKENS, base) 
         && ArrayUtils.contains(STABLE_TOKENS, quote)) {
            // return 1$ for stablecoins, whatever happens
            return EXA;
        }

        // Special case for computing OMM price
        if (base.equals("OMM")) {
            return this.getOmmPrice(quote);
        }

        try {
            // Use Band Oracle as a default Oracle
            Map<String, ?> data = IBandOracle.get_reference_data(this.bandOracle.get(), base, quote);
            return (BigInteger) data.get("rate");
        } catch (Exception e) {
            // Use Balanced DEX as a fallback Oracle
            final Address dex = this.balancedDex.get();
            BigInteger poolId = IBalancedDex.lookupPid(dex, base + "/" + quote);

            Context.require(poolId != null && !poolId.equals(ZERO), 
                "getPrice: Invalid poolId");

            Map<String, ?> poolStats = IBalancedDex.getPoolStats(dex, poolId);
            return (BigInteger) poolStats.get("price");
        }
    }

    private BigInteger getOmmPrice (String quote) {
        BigInteger totalPrice = ZERO;
        BigInteger totalOmmSupply = ZERO;
        final Address dex = this.balancedDex.get();

        for (var token : OMM_TOKENS) {
            // key in band oracle
            BigInteger poolId = IBalancedDex.lookupPid(dex, "OMM/" + token.name);
            if (poolId == null || poolId.equals(ZERO)) {
                continue;
            }

            Map<String, ?> poolStats = IBalancedDex.getPoolStats(dex, poolId);
            // convert price to 10**18 precision and calculate price in quote
            BigInteger price = (BigInteger) poolStats.get("price");
            BigInteger quoteDecimals = (BigInteger) poolStats.get("quote_decimals");
            BigInteger baseDecimals = (BigInteger) poolStats.get("base_decimals");
            BigInteger averageDecimals = quoteDecimals.multiply(BigInteger.valueOf(18)).divide(baseDecimals);
            BigInteger adjustedPrice = token.convert(dex, price, averageDecimals.intValue());
            BigInteger convertedPrice = MathUtils.exaMul(adjustedPrice, this.getPrice(token.priceOracleKey, quote));
            
            BigInteger totalSupply = (BigInteger) poolStats.get("base");

            totalOmmSupply = totalOmmSupply.add(totalSupply);
            totalPrice = totalPrice.add(totalSupply.multiply(convertedPrice));
        }

        if (totalOmmSupply.equals(ZERO)) {
            return null;
        }

        return totalPrice.divide(totalOmmSupply);
    }

    // ================================================
    // Public methods
    // ================================================
    @External(readonly = true)
    public BigInteger get_reference_data (String base, String quote) {
        return this.getPrice(base, quote);
    }


    // @EventLog
    // public void Price (BigInteger price) {}

    // @External
    // public void get_reference_data_write (String base, String quote) {
    //     this.Price(this.getPrice(base, quote));
    // }

    // ================================================
    // Public variable getters
    // ================================================
    /**
     * Get the contract name
     */
    @External(readonly = true)
    public String name() {
        return this.name;
    }
    
    @External(readonly = true)
    public Address balancedDex () {
        return this.balancedDex.get();
    }

    @External(readonly = true)
    public Address bandOracle () {
        return this.bandOracle.get();
    }
}
