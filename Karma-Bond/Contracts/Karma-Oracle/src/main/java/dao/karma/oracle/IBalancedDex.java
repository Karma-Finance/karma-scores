package dao.karma.oracle;

import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.Context;

public class IBalancedDex {

  public static BigInteger getBalnPrice (Address address) {
    return (BigInteger) Context.call(address, "getBalnPrice");
  }

  public static BigInteger getPriceByName (Address address, String name) {
    return (BigInteger) Context.call(address, "getPriceByName", name);
  }

  public static BigInteger lookupPid (Address address, String name) {
    return (BigInteger) Context.call(address, "lookupPid", name);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, ?> getPoolStats (Address address, BigInteger poolId) {
    return (Map<String, ?>) Context.call (address, "getPoolStats", poolId);
  }
}
