package dao.karma.custombond.mocks;

import java.math.BigInteger;

import score.Context;
import score.annotation.External;
import score.annotation.Payable;

public class KarmaTreasuryMock {
  @Payable
  @External
  public void depositIcx () {
    // Make sure we're actually deposit some ICX
    Context.require(Context.getValue().compareTo(BigInteger.ZERO) > 0);
  }
}
