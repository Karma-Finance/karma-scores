package dao.karma.customtreasury;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaCustomTreasuryClient;
import dao.karma.test.ScoreSpy;
import dao.karma.test.tokens.Bnusd;

public class DeployTest extends KarmaCustomTreasuryTest {
  
  ScoreSpy<Bnusd> payoutToken;

  @BeforeEach
  void setup() throws Exception {
    payoutToken = deploy(Bnusd.class, "bnUSD", "bnUSD", 18);
    setup_treasury(payoutToken.getAddress(), owner.getAddress());
  }

  @Test
  void testDeploy () {
    // Check initial parameters
    assertEquals(payoutToken.getAddress(), KarmaCustomTreasuryClient.payoutToken(treasury.score));
    assertEquals(owner.getAddress(), KarmaCustomTreasuryClient.policy(treasury.score)); 
  }
}
