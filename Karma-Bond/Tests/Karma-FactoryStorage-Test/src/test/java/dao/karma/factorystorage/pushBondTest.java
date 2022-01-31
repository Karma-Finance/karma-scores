package dao.karma.factorystorage;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.client.KarmaFactoryStorageClient;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.test.AssertUtils;
import score.Address;

public class pushBondTest extends KarmaFactoryStorageTest {

  // Fake contracts
  Address payoutToken = sm.createAccount().getAddress();
  Address principalToken = sm.createAccount().getAddress();
  Address treasuryAddress = sm.createAccount().getAddress();
  Address bondAddress = sm.createAccount().getAddress();
  Address initialOwner = sm.createAccount().getAddress();
  BigInteger[] tierCeilings = {
    EXA.multiply(BigInteger.valueOf(10)),
    EXA.multiply(BigInteger.valueOf(20))
  };
  BigInteger[] fees = {
    BigInteger.valueOf(33300),
    BigInteger.valueOf(66600)
  };
  BondDetails details = new BondDetails (
    payoutToken, 
    principalToken, 
    treasuryAddress, 
    bondAddress, 
    initialOwner, 
    tierCeilings, 
    fees
  );

  @BeforeEach
  void setup() throws Exception {
    setup_factory_storage();
  }

  @Test
  void testPushBond () {
    // Set the factory address as the caller so we can call it
    KarmaFactoryStorageClient.setFactoryAddress (
      factoryStorage.score, 
      owner, 
      owner.getAddress()
    );

    KarmaFactoryStorageClient.pushBond (
      factoryStorage.score,
      owner,
      details
    );
  }

  @Test
  void testNotFactoryAddress () {
    // Set the factory address as another address
    KarmaFactoryStorageClient.setFactoryAddress (
      factoryStorage.score, 
      owner, 
      alice.getAddress()
    );

    AssertUtils.assertThrowsMessage(() -> 
      KarmaFactoryStorageClient.pushBond (
        factoryStorage.score,
        owner,
        details
      ), 
      "checkIsKarmaFactory: Only Karma Factory can call this method"
    );
  }
}
