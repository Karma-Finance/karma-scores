package dao.karma.factory;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.factory.client.KarmaFactoryClient;
import dao.karma.test.AssertUtils;

public class createBondAndTreasuryTest extends KarmaFactoryTest {

  // Fake contracts
  Account karmaTreasury = sm.createAccount();
  Account karmaSubsidyRouter = sm.createAccount();
  Account karmaDAO = sm.createAccount();
  Account payoutToken = sm.createAccount();
  Account principalToken = sm.createAccount();
  Account initialOwner = sm.createAccount();
  Score karmaFactoryStorage = null;

  @BeforeEach
  void setup() throws Exception {

    karmaFactoryStorage = deploy(FactoryStorageMock.class).score;

    setup_factory (
      karmaTreasury.getAddress(),
      karmaFactoryStorage.getAddress(),
      karmaSubsidyRouter.getAddress(),
      karmaDAO.getAddress()
    );
  }

  @Test
  void testCreateBondAndTreasury () {
    BigInteger[] tierCeilings = {
      EXA.multiply(BigInteger.valueOf(10)),
      EXA.multiply(BigInteger.valueOf(20))
    };
    BigInteger[] fee = {
      BigInteger.valueOf(33300),
      BigInteger.valueOf(66600)
    };

    KarmaFactoryClient.setCustomTreasuryContractBytes(
      factory.score,
      owner,
      new byte[] {}
    );

    KarmaFactoryClient.setCustomBondContractBytes(
      factory.score,
      owner,
      new byte[] {}
    );

    KarmaFactoryClient.createBondAndTreasury(
      factory.score,
      owner,
      payoutToken.getAddress(),
      principalToken.getAddress(),
      initialOwner.getAddress(),
      tierCeilings,
      fee
    );
  }

  @Test
  void testCreateBondAndTreasuryContractNotSet () {
    BigInteger[] tierCeilings = {
      EXA.multiply(BigInteger.valueOf(10)),
      EXA.multiply(BigInteger.valueOf(20))
    };
    BigInteger[] fee = {
      BigInteger.valueOf(33300),
      BigInteger.valueOf(66600)
    };

    AssertUtils.assertThrowsMessage (
      () -> KarmaFactoryClient.createBondAndTreasury(
        factory.score,
        owner,
        payoutToken.getAddress(),
        principalToken.getAddress(),
        initialOwner.getAddress(),
        tierCeilings,
        fee
      ), 
      "checkCustomTreasuryContractBytesInitialized: not set");
    ;
  }
}
