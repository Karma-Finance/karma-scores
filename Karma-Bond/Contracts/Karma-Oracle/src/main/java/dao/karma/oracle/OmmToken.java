package dao.karma.oracle;

import java.math.BigInteger;
import dao.karma.interfaces.bond.IBalancedDEX;
import dao.karma.utils.MathUtils;
import score.Address;

public class OmmToken {

  public Address address;
  // key in band oracle
  public Address priceOracleKey;

  public OmmToken (Address address, Address priceOracleKey) {
    this.address = address;
    this.priceOracleKey = priceOracleKey;
  }

  public BigInteger convert (
    Address source, 
    BigInteger amount, 
    int decimals,
    Address USDS, 
    Address IUSDC, 
    Address SICX,
    BigInteger SICX_ICX_POOL_ID
  ) {
    if (
      this.address.equals(USDS) 
  ||  this.address.equals(IUSDC)
    ) {
      return MathUtils.convertToExa(amount, decimals);
    }

    else if (this.address.equals(SICX)) {
      BigInteger priceExa = MathUtils.convertToExa(amount, decimals);
      BigInteger sicxIcxPrice = (BigInteger) IBalancedDEX.getPrice(source, SICX_ICX_POOL_ID);
      return MathUtils.exaMul(priceExa, sicxIcxPrice);
    }

    return null;
  }
}
