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
import dao.karma.interfaces.bond.IBalancedDEX;
import dao.karma.interfaces.oracle.IBandOracle;
import dao.karma.interfaces.oracle.IStakedIcx;
import dao.karma.types.Ownable;
import dao.karma.utils.EnumerableSet;
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
    
    // OMM tokens
    private final OmmToken[] OMM_TOKENS;
    
    // Hardcoded addresses
    public final Address ZERO_ADDRESS = Address.fromString("cx0000000000000000000000000000000000000000");
    public final Address ICX = Address.fromString("cx1111111111111111111111111111111111111111");
    public final Address USDS;
    public final Address USDB;
    public final Address BNUSD;
    public final Address IUSDC;
    public final Address IUSDT;
    public final Address BALN;
    public final Address SICX;
    public final Address OMM;
    
    // Hardcoded SICX/ICX pool ID
    final BigInteger SICX_ICX_POOL_ID;
  
    // ================================================
    // DB Variables
    // ================================================
    // The Balanced DEX address
    private final VarDB<Address> balancedDex = Context.newVarDB(NAME + "_balancedDex", Address.class);
    // The Band Oracle address
    private final VarDB<Address> bandOracle = Context.newVarDB(NAME + "_bandOracle", Address.class);
    // List of stablecoins - the price for these tokens will always be evaluated at 1$ whatever happens
    private final EnumerableSet<Address> stablecoins = new EnumerableSet<>(NAME + "_stablecoins", Address.class);

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
        Address bandOracle,
        // Token Addresses
        Address USDS,
        Address USDB,
        Address BNUSD,
        Address IUSDC,
        Address IUSDT,
        Address BALN,
        Address SICX,
        Address OMM,
        BigInteger SICX_ICX_POOL_ID
    ) {
        super(initialOwner);

        // OK
        this.name = "Karma Price Oracle";
        
        // Tokens
        this.USDS = USDS;
        this.USDB = USDB;
        this.BNUSD = BNUSD;
        this.IUSDC = IUSDC;
        this.IUSDT = IUSDT;
        this.BALN = BALN;
        this.SICX = SICX;
        this.OMM = OMM;
        // Pool IDs
        this.SICX_ICX_POOL_ID = SICX_ICX_POOL_ID;

        // Default initialization
        this.OMM_TOKENS = new OmmToken[3];
        this.OMM_TOKENS[0] = new OmmToken(USDS, USDS);
        this.OMM_TOKENS[1] = new OmmToken(SICX, ICX);
        this.OMM_TOKENS[2] = new OmmToken(IUSDC, IUSDC);

        if (this.balancedDex.get() == null) {
            this.balancedDex.set(balancedDex);
        }

        if (this.bandOracle.get() == null) {
            this.bandOracle.set(bandOracle);
        }

        if (this.stablecoins.length() == 0) {
            if (!USDS.equals(ZERO_ADDRESS))  this.stablecoins.add(USDS);
            if (!USDB.equals(ZERO_ADDRESS))  this.stablecoins.add(USDB);
            if (!BNUSD.equals(ZERO_ADDRESS)) this.stablecoins.add(BNUSD);
            if (!IUSDC.equals(ZERO_ADDRESS)) this.stablecoins.add(IUSDC);
            if (!IUSDT.equals(ZERO_ADDRESS)) this.stablecoins.add(IUSDT);
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

    @External
    public void addStablecoin (Address newStablecoin) {
        // Access control
        this.onlyPolicy();

        // OK
        this.stablecoins.add(newStablecoin);
    }

    @External
    public void removeStablecoin (Address newStablecoin) {
        // Access control
        this.onlyPolicy();

        // OK
        this.stablecoins.remove(newStablecoin);
    }

    @EventLog(indexed = 1)
    public void Price (BigInteger price) {}

    // ================================================
    // Private methods
    // ================================================
    private BigInteger getPrice (Address base) {
        
        // Stablecoin / USD
        if (this.stablecoins.contains(base)) {
            // return 1$ for stablecoins, whatever happens
            return EXA;
        }

        // ICX / USD
        if (base.equals(ICX)) {
            // Use Band Oracle
            Map<String, ?> data = IBandOracle.get_reference_data(this.bandOracle.get(), "ICX", "USD");
            BigInteger price = (BigInteger) data.get("rate");
            return price;
        }

        // BALN / USD
        else if (base.equals(BALN)) {
            // Use Balanced BALN Oracle
            BigInteger price = IBalancedDEX.getBalnPrice(this.balancedDex.get());
            return price;
        }
    
        // OMM / USD
        else if (base.equals(OMM)) {
            return this.getOmmPrice();
        }

        // sICX / USD
        else if (base.equals(SICX)) {
            return this.getSicxPrice();
        }

        // Other pairs
        else {
            return this.getGenericPrice(base);
        }
    }

    private BigInteger getSicxPrice () {
        BigInteger icxPrice = this.getPrice(ICX);
        BigInteger sIcxRate = IStakedIcx.priceInLoop(SICX);
        return sIcxRate.multiply(icxPrice).divide(EXA);
    }

    private BigInteger getGenericPrice (Address base) {

        int stablecoinsSize = this.stablecoins.length();
        final Address dex = this.balancedDex.get();
        BigInteger totalPrice = ZERO;
        BigInteger totalBaseSupply = ZERO;

        for (int i = 0; i < stablecoinsSize; i++) {
            Address stablecoin = this.stablecoins.at(i);
            BigInteger poolId = ZERO;

            try {
                poolId = IBalancedDEX.getPoolId(dex, base, stablecoin);
            } catch (Exception e) {
                // The base / stablecoin pool may not exist, keep iterating
                continue;
            }
            Map<String, ?> poolStats = IBalancedDEX.getPoolStats(dex, poolId);
            
            // convert price to 10**18 precision and calculate price in quote
            BigInteger price = (BigInteger) poolStats.get("price");
            BigInteger quoteDecimals = (BigInteger) poolStats.get("quote_decimals");
            BigInteger baseDecimals = (BigInteger) poolStats.get("base_decimals");
            BigInteger averageDecimals = quoteDecimals.multiply(BigInteger.valueOf(18)).divide(baseDecimals);
            BigInteger adjustedPrice = MathUtils.convertToExa(price, averageDecimals.intValue());

            BigInteger oraclePrice = this.getPrice(stablecoin);
            BigInteger convertedPrice = MathUtils.exaMul(adjustedPrice, oraclePrice);
            BigInteger totalSupply = (BigInteger) poolStats.get("base");

            totalBaseSupply = totalBaseSupply.add(totalSupply);
            totalPrice = totalPrice.add(totalSupply.multiply(convertedPrice));
        }

        if (totalBaseSupply.equals(ZERO)) {
            Context.revert("getGenericPrice: this token doesn't exist or doesn't have any stablecoin associated with it");
            return null;
        }

        return totalPrice.divide(totalBaseSupply);
    }

    private BigInteger getOmmPrice () {
        BigInteger totalPrice = ZERO;
        BigInteger totalOmmSupply = ZERO;
        final Address dex = this.balancedDex.get();

        for (var token : OMM_TOKENS) {
            BigInteger poolId = IBalancedDEX.getPoolId(dex, OMM, token.address);

            if (poolId == null || poolId.equals(ZERO)) {
                Context.revert("getOmmPrice: Unexpected error while retrieving the OMM price");
                return null;
            }

            Map<String, ?> poolStats = IBalancedDEX.getPoolStats(dex, poolId);

            // convert price to 10**18 precision and calculate price in quote
            BigInteger price = (BigInteger) poolStats.get("price");
            BigInteger quoteDecimals = (BigInteger) poolStats.get("quote_decimals");
            BigInteger baseDecimals = (BigInteger) poolStats.get("base_decimals");
            BigInteger averageDecimals = quoteDecimals.multiply(BigInteger.valueOf(18)).divide(baseDecimals);
            BigInteger adjustedPrice = token.convert(dex, price, averageDecimals.intValue(), USDS, IUSDC, SICX, SICX_ICX_POOL_ID);

            BigInteger oraclePrice = this.getPrice(token.priceOracleKey);
            BigInteger convertedPrice = MathUtils.exaMul(adjustedPrice, oraclePrice);
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
    public BigInteger getUsdPrice (Address base) {
        try {
            return this.getPrice(base);
        } catch (Exception e) {
            Context.revert("getUsdPrice: cannot retrieve price properly");
            return null;
        }
    }

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

    @External(readonly = true)
    public Address[] stablecoins () {
        Address[] result = new Address[this.stablecoins.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.stablecoins.at(i);
        }

        return result;
    }
}
