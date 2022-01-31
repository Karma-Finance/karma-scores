package dao.karma.factory;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.client.KarmaFactoryClient;
import dao.karma.test.AssertUtils;

public class setCustomBondContractBytesTest extends KarmaFactoryTest {

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
    KarmaFactoryClient.setCustomBondContractBytes(
      factory.score,
      owner,
      new byte[] {}
    );
  }

  @Test
  void testAlreadySet () {
    KarmaFactoryClient.setCustomBondContractBytes(
      factory.score,
      owner,
      new byte[] {}
    );

    AssertUtils.assertThrowsMessage(() -> 
      KarmaFactoryClient.setCustomBondContractBytes(
        factory.score,
        owner,
        new byte[] {}
      ), 
      "checkCustomBondContractBytesNotInitialized: already set"
    );
  }
}
