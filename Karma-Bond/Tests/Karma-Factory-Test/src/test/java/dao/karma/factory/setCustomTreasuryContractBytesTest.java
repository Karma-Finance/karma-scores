package dao.karma.factory;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.factory.client.KarmaFactoryClient;
import dao.karma.test.AssertUtils;

public class setCustomTreasuryContractBytesTest extends KarmaFactoryTest {

  // Fake contracts
  Account karmaTreasury = sm.createAccount();
  Account karmaFactoryStorage = sm.createAccount();
  Account karmaSubsidyRouter = sm.createAccount();
  Account karmaDAO = sm.createAccount();
  Account payoutToken = sm.createAccount();
  Account principalToken = sm.createAccount();
  Account initialOwner = sm.createAccount();

  @BeforeEach
  void setup() throws Exception {
    setup_factory (
      karmaTreasury.getAddress(),
      karmaFactoryStorage.getAddress(),
      karmaSubsidyRouter.getAddress(),
      karmaDAO.getAddress()
    );
  }

  @Test
  void testSetCustomBondContractBytes () {
    KarmaFactoryClient.setCustomTreasuryContractBytes(
      factory.score,
      owner,
      new byte[] {}
    );
  }

  @Test
  void testAlreadySet () {
    KarmaFactoryClient.setCustomTreasuryContractBytes(
      factory.score,
      owner,
      new byte[] {}
    );

    AssertUtils.assertThrowsMessage(() -> 
      KarmaFactoryClient.setCustomTreasuryContractBytes(
        factory.score,
        owner,
        new byte[] {}
      ), 
      "checkCustomTreasuryContractBytesNotInitialized: already set"
    );
  }
}
