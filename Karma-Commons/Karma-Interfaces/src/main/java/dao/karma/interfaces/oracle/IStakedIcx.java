package dao.karma.interfaces.oracle;

import java.math.BigInteger;
import score.Address;
import score.Context;

public class IStakedIcx {

  public static BigInteger priceInLoop (Address address) {
    return (BigInteger) Context.call(address, "priceInLoop");
  }

}
