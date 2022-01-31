package dao.karma.custombond;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.custombond.client.KarmaCustomBondClient;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.test.AssertUtils;
import score.Address;

public class initializeBondTest extends KarmaCustomBondTest {
  
  // Fake contracts
  Address customTreasury = sm.createAccount().getAddress();
  Address karmaTreasury = sm.createAccount().getAddress();
  Address subsidyRouter = sm.createAccount().getAddress();
  Address karmaDAO = sm.createAccount().getAddress();
  Address payoutToken = sm.createAccount().getAddress();
  Address principalToken = sm.createAccount().getAddress();
  Address treasuryAddress = sm.createAccount().getAddress();
  Address bondAddress = sm.createAccount().getAddress();
  Address initialOwner = owner.getAddress();

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
      )
      , "debtDecay: The vesting term must be initialized first");
  }
}
