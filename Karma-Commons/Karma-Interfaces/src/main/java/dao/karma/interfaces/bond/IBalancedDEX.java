package dao.karma.interfaces.bond;

import java.math.BigInteger;
import java.util.Map;

import score.Address;
import score.Context;

public class IBalancedDEX {

  public static BigInteger getBalnPrice (Address address) {
    return (BigInteger) Context.call(address, "getBalnPrice");
  }

  public static BigInteger getPriceByName (Address address, String name) {
    return (BigInteger) Context.call(address, "getPriceByName", name);
  }

  public static BigInteger getPrice (Address address, BigInteger _id) {
    return (BigInteger) Context.call(address, "getPrice", _id);
  }

  public static BigInteger lookupPid (Address address, String name) {
    return (BigInteger) Context.call(address, "lookupPid", name);
  }

  public static BigInteger getPoolId (Address address, Address token1, Address token2) {
    return (BigInteger) Context.call(address, "getPoolId", token1, token2);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getPoolStats (Address principalTokenAddress, BigInteger poolIdPrincipalToken) {
    return (Map<String, Object>) Context.call(principalTokenAddress, "getPoolStats", poolIdPrincipalToken);
  }
}
