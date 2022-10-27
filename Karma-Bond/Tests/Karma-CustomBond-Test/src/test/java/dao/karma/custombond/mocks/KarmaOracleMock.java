package dao.karma.custombond.mocks;

import java.math.BigInteger;
import dao.karma.utils.MathUtils;
import score.Address;
import score.annotation.External;

public class KarmaOracleMock {
  public KarmaOracleMock () {
    
  }

  @External
  public BigInteger getUsdPrice (Address base) {
    return MathUtils.pow10(18);
  }
}
