package dao.karma.subsidyrouter.mocks;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

import score.annotation.EventLog;
import score.annotation.External;

public class BondMock {

  public BondMock () {}

  @EventLog
  public void PayoutUpdate (
      BigInteger oldPayout,
      BigInteger newPayout
  ) {}

  @External
  public BigInteger paySubsidy () {
    this.PayoutUpdate(ZERO, ONE);
    return ONE;
  }
}
