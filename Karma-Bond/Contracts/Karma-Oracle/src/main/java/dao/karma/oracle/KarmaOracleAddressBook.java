package dao.karma.oracle;

import score.Address;
import score.Context;

public class KarmaOracleAddressBook {
  final String NAME = "KarmaOracleAddressBook";

  // MainNet
  final Address USDS = Address.fromString("cxbb2871f468a3008f80b08fdde5b8b951583acf06");
  final Address USDB = Address.fromString("cx24fecad15fd6496652299d6b5d76e781e82cb815");
  final Address bnUSD = Address.fromString("cx88fd7df7ddff82f7cc735c871dc519838cb235bb");
  final Address IUSDC = Address.fromString("cxae3034235540b924dfcc1b45836c293dcc82bfb7");
  final Address IUSDT = Address.fromString("cx3a36ea1f6b9aa3d2dd9cb68e8987bcc3aabaaa88");

  public Address getAddress (String name) {
    switch (name) {
      case "USDS":
        return USDS;
      
      case "USDB":
        return USDB;
      
      case "bnUSD":
        return bnUSD;
      
      case "USDC":
      case "IUSDC":
        return IUSDC;
      
      case "IUSDT":
      case "USDT":
        return IUSDT;

      default:
        Context.revert(NAME + "::getAddress: Cannot find name");
        return null;
    }
  }
}
