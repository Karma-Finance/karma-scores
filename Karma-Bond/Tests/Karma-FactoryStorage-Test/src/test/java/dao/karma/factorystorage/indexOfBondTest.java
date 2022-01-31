package dao.karma.factorystorage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.client.KarmaFactoryStorageClient;
import dao.karma.structs.factorystorage.BondDetails;
import score.Address;

public class indexOfBondTest extends KarmaFactoryStorageTest {

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
  void testIndexOfBond () {
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

    // First bond should be at index 0
    int index = KarmaFactoryStorageClient.indexOfBond (
      factoryStorage.score,
      bondAddress
    );

    assertEquals(0, index);

    // Push another
    KarmaFactoryStorageClient.pushBond (
      factoryStorage.score,
      owner,
      details
    );

    // First bond should be at index 0
    index = KarmaFactoryStorageClient.indexOfBond (
      factoryStorage.score,
      bondAddress
    );

    assertEquals(1, index);

    // Check bondDetails
    BondDetails detailsResult = KarmaFactoryStorageClient.bondDetails(
      factoryStorage.score,
      index
    );

    assertEquals(details.payoutToken, detailsResult.payoutToken);
    assertEquals(details.principalToken, detailsResult.principalToken);
    assertEquals(details.treasuryAddress, detailsResult.treasuryAddress);
    assertEquals(details.bondAddress, detailsResult.bondAddress);
    assertEquals(details.initialOwner, detailsResult.initialOwner);
    assertArrayEquals(details.tierCeilings, detailsResult.tierCeilings);
    assertArrayEquals(details.fees, detailsResult.fees);
  }
}
