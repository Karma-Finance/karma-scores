package dao.karma.oracle;

import java.math.BigInteger;
import dao.karma.interfaces.bond.IBalancedDEX;
import dao.karma.utils.MathUtils;
import score.Address;

public class OmmToken {
  
  public String name;
  // key in band oracle
  public String priceOracleKey;

  public OmmToken (String name, String priceOracleKey) {
    this.name = name;
    this.priceOracleKey = priceOracleKey;
  }

  public BigInteger convert (Address source, BigInteger amount, int decimals) {
    if (
      name.equals("USDS") 
  ||  name.equals("IUSDC")
    ) {
      return MathUtils.convertToExa(amount, decimals);
    }

    else if (name.equals("sICX")) {
      BigInteger priceExa = MathUtils.convertToExa(amount, decimals);
      BigInteger sicxIcxPrice = (BigInteger) IBalancedDEX.getPriceByName(source, "sICX/ICX");
      return MathUtils.exaMul(priceExa, sicxIcxPrice);
    }

    return null;
  }
}
