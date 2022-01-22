package dao.karma.subsidyrouter.mocks;

import score.Address;
import score.Context;

public class SubsidyControllerMock {
  public final Address subsidyRouter;

  public SubsidyControllerMock (Address subsidyRouter) {
    this.subsidyRouter = subsidyRouter;
  }

  public void getSubsidyInfo () {
    Context.call(this.subsidyRouter, "getSubsidyInfo");
  }
}
