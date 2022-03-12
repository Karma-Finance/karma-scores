package dao.karma.custombond;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.test.AssertUtils;
import score.Address;

public class initializeBondTest extends KarmaCustomBondTest {

  // Fake contracts
  final Address customTreasury = sm.createAccount().getAddress();
  final Address karmaTreasury = sm.createAccount().getAddress();
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
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }

  @Test
  void testInitializeBondVesting () {
    BigInteger controlVariable = BigInteger.valueOf(400000);
    long vestingTerm = 302400; // 1 week
    BigInteger minimumPrice = BigInteger.valueOf(5403);
    BigInteger maxPayout = BigInteger.valueOf(500);
    BigInteger maxDebt = new BigInteger("5000000000");
    BigInteger initialDebt = new BigInteger("1560000000");

    // Set bond terms must be called beforehand
    KarmaCustomBondClient.setBondTerms (
      bond.score,
      owner,
      KarmaCustomBond.VESTING,
      BigInteger.valueOf(vestingTerm)
    );

    KarmaCustomBondClient.initializeBond (
      bond.score,
      owner,
      controlVariable,
      vestingTerm,
      minimumPrice,
      maxPayout,
      maxDebt,
      initialDebt
    );
  }

  @Test
  void testTermsNotInitialized () {
    BigInteger controlVariable = BigInteger.valueOf(400000);
    long vestingTerm = 302400; // 1 week
    BigInteger minimumPrice = BigInteger.valueOf(5403);
    BigInteger maxPayout = BigInteger.valueOf(500);
    BigInteger maxDebt = new BigInteger("5000000000");
    BigInteger initialDebt = new BigInteger("1560000000");

    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.initializeBond (
        bond.score,
        owner,
        controlVariable,
        vestingTerm,
        minimumPrice,
        maxPayout,
        maxDebt,
        initialDebt
      ),
      "debtDecay: The vesting term must be initialized first");
  }
}
