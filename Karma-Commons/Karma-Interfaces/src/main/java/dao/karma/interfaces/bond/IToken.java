package dao.karma.interfaces.bond;

import java.math.BigInteger;

import dao.karma.interfaces.irc2.IIRC2;
import dao.karma.utils.ICX;
import dao.karma.utils.JSONUtils;
import score.Address;

public class IToken {

  public static void transfer (Address token, Address destination, BigInteger value) {
    if (ICX.isICX(token)) {
      ICX.transfer(destination, value);
    } else {
      IIRC2.transfer(token, destination, value, "".getBytes());
    }
  }

  public static void transfer (Address token, Address destination, BigInteger value, String method) {
    if (ICX.isICX(token)) {
      ICX.transfer(destination, value, method + "Icx");
    } else {
      IIRC2.transfer(token, destination, value, JSONUtils.method(method));
    }
  }

  public static String symbol (Address token) {
    if (ICX.isICX(token)) {
      return ICX.SYMBOL;
    } else {
      return IIRC2.symbol(token);
    }
  }

  public static int decimals (Address token) {
    if (ICX.isICX(token)) {
      return ICX.DECIMALS;
    } else {
      return IIRC2.decimals(token);
    }
  }

  public static BigInteger totalSupply (Address token) {
    if (ICX.isICX(token)) {
      return ICX.totalSupply();
    } else {
      return IIRC2.totalSupply(token);
    }
  }
}
