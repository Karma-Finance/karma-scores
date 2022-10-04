package dao.karma.custombond;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.test.AssertUtils;
import score.Address;

public class setBondTermsTest extends KarmaCustomBondTest {

  // Fake contracts
  final Address customTreasury = sm.createAccount().getAddress();
  final Address karmaTreasury = sm.createAccount().getAddress();
  final Address karmaOracle = sm.createAccount().getAddress();
  final Address subsidyRouter = sm.createAccount().getAddress();
  final Address karmaDAO = sm.createAccount().getAddress();
  final Address payoutToken = sm.createAccount().getAddress();
  final Address principalToken = sm.createAccount().getAddress();
  final Address treasuryAddress = sm.createAccount().getAddress();
  final Address bondAddress = sm.createAccount().getAddress();
  final Address initialOwner = owner.getAddress();
  final BigInteger[] tierCeilings = {
    EXA.multiply(BigInteger.valueOf(10)),
    EXA.multiply(BigInteger.valueOf(20))
  };
  final BigInteger[] fees = {
    BigInteger.valueOf(33300),
    BigInteger.valueOf(66600)
  };
  final BondDetails details = new BondDetails (
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
    setup_bond(
      customTreasury,
      payoutToken,
      principalToken,
      karmaTreasury,
      karmaOracle,
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }

  @Test
  void testOnlyPolicy () {
    var value = BigInteger.valueOf(302400); // 1 week

    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.setBondTerms (
        bond.score,
        alice,
        KarmaCustomBond.VESTING,
        value
      ),
      "onlyPolicy: caller is not the owner"
    );
  }

  @Test
  void testSetBondTermsVesting () {
    var value = BigInteger.valueOf(302400); // 1 week

    // Set bond terms must be called beforehand
    KarmaCustomBondClient.setBondTerms (
      bond.score,
      owner,
      KarmaCustomBond.VESTING,
      value
    );
  }

  @Test
  void testSetBondTermsVestingMustBeLongerThan36Hours () {
    int minHours = 36;
    int averageBlockTime = 2;
    int minVesting = minHours * 3600 / averageBlockTime;

    var value = BigInteger.valueOf(minVesting);

    // Set bond terms must be called beforehand
    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.setBondTerms (
        bond.score,
        owner,
        KarmaCustomBond.VESTING,
        value.subtract(BigInteger.ONE) // -1 so it should throw
      ),
      "setBondTerms: Vesting must be longer than " + KarmaCustomBond.BOND_TERMS_VESTING_MIN_SECONDS + " seconds"
    );
  }

  @Test
  void testSetBondTermsPayout () {
    var value = BigInteger.valueOf(500); // 500 = 0.5%

    // Set bond terms must be called beforehand
    KarmaCustomBondClient.setBondTerms (
      bond.score,
      owner,
      KarmaCustomBond.PAYOUT,
      value
    );
  }

  @Test
  void testSetBondTermsPayoutCannotBeAbove1Percent () {
    var value = BigInteger.valueOf(1000); // 1000 = 1%

    // Set bond terms must be called beforehand
    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.setBondTerms (
        bond.score,
        owner,
        KarmaCustomBond.PAYOUT,
        value.add(BigInteger.ONE) // +1 so it should throw
      ),
      "setBondTerms: Payout cannot be above 1 percent"
    );
  }

  @Test
  void testSetBondTermsDebt () {
    var value = new BigInteger("5000000000");

    // Set bond terms must be called beforehand
    KarmaCustomBondClient.setBondTerms (
      bond.score,
      owner,
      KarmaCustomBond.DEBT,
      value
    );
  }
}
