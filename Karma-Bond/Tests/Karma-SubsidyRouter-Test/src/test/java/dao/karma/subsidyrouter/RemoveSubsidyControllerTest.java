package dao.karma.subsidyrouter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.client.KarmaSubsidyRouterClient;
import dao.karma.test.AssertUtils;
import score.Address;

public class RemoveSubsidyControllerTest extends KarmaSubsidyRouterTest {
  
  Address bond = sm.createAccount().getAddress();
  Address subsidyController = sm.createAccount().getAddress();

  @BeforeEach
  void setup() throws Exception {
    setup_subsidy_router();
  }

  @Test
  void testRemoveSubsidyController () {
    KarmaSubsidyRouterClient.addSubsidyController(subsidyRouter.score, owner, subsidyController, bond);
    KarmaSubsidyRouterClient.removeSubsidyController(subsidyRouter.score, owner, subsidyController);
    
    // Check if correctly removed
    assertEquals(null, KarmaSubsidyRouterClient.bondForController(subsidyRouter.score, subsidyController));
  }
  
  @Test
  void testSilentlyRemoveNotExists () {
    // Shouldn't raise if it doesn't exist
    KarmaSubsidyRouterClient.removeSubsidyController(subsidyRouter.score, owner, subsidyController);
  }
  
  @Test
  void testNotOwner () {
    KarmaSubsidyRouterClient.addSubsidyController(subsidyRouter.score, owner, subsidyController, bond);

    AssertUtils.assertThrowsMessage(
      () -> KarmaSubsidyRouterClient.removeSubsidyController(subsidyRouter.score, alice, subsidyController),
      "onlyPolicy: caller is not the owner"
    );
  }
}
