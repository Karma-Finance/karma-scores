package dao.karma.custombond;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.IRC2Client;
import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.clients.KarmaCustomTreasuryClient;
import dao.karma.custombond.tokens.PayoutToken;
import dao.karma.custombond.tokens.PrincipalToken;
import dao.karma.customtreasury.KarmaCustomTreasury;
import dao.karma.structs.bond.Terms;
import dao.karma.test.ScoreSpy;
import dao.karma.test.SleepUtils;
import dao.karma.utils.JSONUtils;
import score.Address;

public class adjustTest extends KarmaCustomBondTest {

  // Fake contracts
  Address karmaTreasury = sm.createAccount().getAddress();
  Address subsidyRouter = sm.createAccount().getAddress();
  Address karmaDAO = sm.createAccount().getAddress();
  Address bondAddress = sm.createAccount().getAddress();
  Address initialOwner = owner.getAddress();

  ScoreSpy<PrincipalToken> principalToken;
  ScoreSpy<PayoutToken> payoutToken;
  ScoreSpy<KarmaCustomTreasury> customTreasury;

  BigInteger[] tierCeilings = {
    EXA.multiply(BigInteger.valueOf(10)),
    EXA.multiply(BigInteger.valueOf(20))
  };
  BigInteger[] fees = {
    BigInteger.valueOf(33300),
    BigInteger.valueOf(66600)
  };

  // Initialization
  BigInteger controlVariable = BigInteger.valueOf(400000);
  long vestingTerm = 302400; // 1 week
  BigInteger minimumPrice = BigInteger.valueOf(5403);
  BigInteger maxPayout = BigInteger.valueOf(500);
  BigInteger maxDebt = EXA.multiply(BigInteger.valueOf(5000));
  BigInteger initialDebt = new BigInteger("1560000000");

  // Deposit
  BigInteger amount = EXA.divide(BigInteger.TEN);
  BigInteger maxPrice = BigInteger.valueOf(1_000_000);
  Address depositor = owner.getAddress();

  // Adjustment
  boolean addition = true;
  BigInteger increment = BigInteger.valueOf(1);
  BigInteger target = controlVariable.add(BigInteger.valueOf(3));
  long buffer = 1;

  @BeforeEach
  void setup() throws Exception {
    // Deploys
    principalToken = deploy(PrincipalToken.class);
    payoutToken = deploy(PayoutToken.class);
    customTreasury = deploy(KarmaCustomTreasury.class, payoutToken.getAddress(), initialOwner);

    setup_bond (
      customTreasury.getAddress(),
      payoutToken.getAddress(),
      principalToken.getAddress(),
      karmaTreasury,
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }

  private void initializeBond () {
    // send some principal token to alice
    IRC2Client.transfer(principalToken.score, owner, alice, amount.multiply(BigInteger.valueOf(100)));

    // send some payout token to the custom treasury
    IRC2Client.transfer(payoutToken.score, owner, customTreasury.getAccount(), EXA.multiply(BigInteger.valueOf(1000)), JSONUtils.method("funding"));

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

    // Enable the bond contract in the custom treasury
    KarmaCustomTreasuryClient.toggleBondContract(customTreasury.score, owner, bond.getAddress());
  }

  private void setAdjustment (boolean addition, BigInteger increment, BigInteger target, long buffer) {
    // Set the adjustment for triggering adjust()
    KarmaCustomBondClient.setAdjustment (
      bond.score,
      owner,
      addition,
      increment,
      target,
      buffer
    );
  }

  @Test
  void testAdjust () {
    initializeBond();
    setAdjustment(addition, increment, target, buffer);

    // sleep buffer
    SleepUtils.sleep(10);

    // Check BCV before
    Terms terms0 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(controlVariable, terms0.controlVariable);

    // call adjust() with deposit
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 1 ---
    Terms terms1 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms0.controlVariable.add(increment), terms1.controlVariable);

    // Adjust again
    SleepUtils.sleep(10);
    // call adjust() with deposit
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 2 ---
    Terms terms2 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms1.controlVariable.add(increment), terms2.controlVariable);

    // Adjust again
    SleepUtils.sleep(10);
    // call adjust() with deposit
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 3 ---
    Terms terms3 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms2.controlVariable.add(increment), terms3.controlVariable);

    // Sleep again, but shouldn't adjust
    SleepUtils.sleep(10);
    // That deposit shouldn't trigger adjustment
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 4 ---
    Terms terms4 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms3.controlVariable, terms4.controlVariable);
  }

  @Test
  void testNegativeAdjust () {
    initializeBond();
    setAdjustment(false, increment, controlVariable.subtract(BigInteger.valueOf(3)), buffer);

    // sleep buffer
    SleepUtils.sleep(buffer);

    // Check BCV before
    Terms terms0 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(controlVariable, terms0.controlVariable);

    // call adjust() with deposit
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 1 ---
    Terms terms1 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms0.controlVariable.subtract(increment), terms1.controlVariable);

    for (int i = 0; i < 3; i++) {
      // sleep buffer
      SleepUtils.sleep(buffer);
  
      // call adjust() with deposit
      KarmaCustomBondClient.deposit(
        bond.score,
        alice, 
        principalToken.score,
        amount,
        maxPrice,
        depositor
      );
    }

    // --- Check BCV N ---
    Terms termsN = KarmaCustomBondClient.terms(bond.score);
    assertEquals(controlVariable.subtract(BigInteger.valueOf(3)), termsN.controlVariable);
  }

  @Test
  void testAdjustHighIncrement () {
    initializeBond();
    setAdjustment(true, BigInteger.TEN, controlVariable.add(BigInteger.valueOf(5)), buffer);

    // sleep buffer
    SleepUtils.sleep(10);

    // Check BCV before
    Terms terms0 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(controlVariable, terms0.controlVariable);

    // call adjust() with deposit
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 1 ---
    Terms terms1 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms0.controlVariable.add(BigInteger.TEN), terms1.controlVariable);
  }

  @Test
  void testAdjustBuffer () {
    initializeBond();
    setAdjustment(true, BigInteger.TEN, controlVariable.add(BigInteger.TEN), 10);

    // sleep buffer - not enough
    SleepUtils.sleep(9);

    // Check BCV before
    Terms terms0 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(controlVariable, terms0.controlVariable);

    // call adjust() with deposit
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 1 ---
    Terms terms1 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms0.controlVariable, terms1.controlVariable);

    // Sleep enough
    SleepUtils.sleep(1);

    // call adjust() with deposit
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // --- Check BCV 2 ---
    Terms terms2 = KarmaCustomBondClient.terms(bond.score);
    assertEquals(terms0.controlVariable.add(BigInteger.TEN), terms2.controlVariable);
  }
}
