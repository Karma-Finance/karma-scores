package dao.karma.subsidyrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaSubsidyRouterClient;
import dao.karma.subsidyrouter.client.SubsidyControllerMockClient;
import dao.karma.subsidyrouter.mocks.BondMock;
import dao.karma.subsidyrouter.mocks.SubsidyControllerMock;
import dao.karma.test.AssertUtils;
import dao.karma.test.ScoreSpy;

public class GetSubsidyInfoTest extends KarmaSubsidyRouterTest {

  ScoreSpy<BondMock> bond;
  ScoreSpy<SubsidyControllerMock> subsidyController;

  @BeforeEach
  void setup() throws Exception {
    setup_subsidy_router();

    bond = deploy(BondMock.class);
    subsidyController = deploy(SubsidyControllerMock.class, subsidyRouter.getAddress());
  }

  @Test
  void testGetSubsidyInfo () {
    KarmaSubsidyRouterClient.addSubsidyController(subsidyRouter.score, owner, subsidyController.getAddress(), bond.getAddress());
    SubsidyControllerMockClient.getSubsidyInfo(subsidyController.score, owner);
  }

  @Test
  void testNotMapped () {
    AssertUtils.assertThrowsMessage(
      () -> SubsidyControllerMockClient.getSubsidyInfo(subsidyController.score, owner), 
      "getSubsidyInfo: Address not mapped");
  }
}
