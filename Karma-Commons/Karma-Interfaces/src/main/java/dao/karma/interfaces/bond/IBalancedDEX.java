package dao.karma.interfaces.bond;

import java.math.BigInteger;
import java.util.Map;

import score.Address;
import score.Context;

public class IBalancedDEX {

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getPoolStats (Address principalTokenAddress, BigInteger poolIdPrincipalToken) {
    return (Map<String, Object>) Context.call(principalTokenAddress, "getPoolStats", poolIdPrincipalToken);
  }
}
