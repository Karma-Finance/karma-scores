package dao.karma.subsidyrouter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaSubsidyRouterClient;
import dao.karma.test.AssertUtils;
import score.Address;

public class AddSubsidyControllerTest extends KarmaSubsidyRouterTest {

  Address bond = sm.createAccount().getAddress();
  Address subsidyController = sm.createAccount().getAddress();

  @BeforeEach
  void setup() throws Exception {
    setup_subsidy_router();
  }

  @Test
  void testAddSubsidyController () {
    // Check if nothing written
    assertEquals(null, KarmaSubsidyRouterClient.bondForController(subsidyRouter.score, subsidyController));

    KarmaSubsidyRouterClient.addSubsidyController(subsidyRouter.score, owner, subsidyController, bond);

    // Check if written correctly
    assertEquals(bond, KarmaSubsidyRouterClient.bondForController(subsidyRouter.score, subsidyController));
  }

  @Test
  void testOverwriteSilently () {
    KarmaSubsidyRouterClient.addSubsidyController(subsidyRouter.score, owner, subsidyController, bond);
    // should silenctly overwrite it
    Address newBond = sm.createAccount().getAddress();
    KarmaSubsidyRouterClient.addSubsidyController(subsidyRouter.score, owner, subsidyController, newBond);
  }

  @Test
  void testNotOwner () {
    AssertUtils.assertThrowsMessage(
      () -> KarmaSubsidyRouterClient.addSubsidyController(subsidyRouter.score, alice, subsidyController, bond),
      "onlyPolicy: caller is not the owner"
    );
  }
}
