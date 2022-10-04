package dao.karma.custombond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.IRC2Client;
import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.clients.KarmaCustomTreasuryClient;
import dao.karma.custombond.mocks.KarmaOracleMock;
import dao.karma.custombond.tokens.PayoutToken;
import dao.karma.custombond.tokens.PrincipalToken;
import dao.karma.customtreasury.KarmaCustomTreasury;
import dao.karma.test.AssertUtils;
import dao.karma.test.ScoreSpy;
import dao.karma.test.SleepUtils;
import dao.karma.utils.JSONUtils;
import dao.karma.utils.TimeUtils;
import score.Address;

public class redeemTest extends KarmaCustomBondTest {
  // Fake contracts
  final Account subsidyRouterAccount = sm.createAccount();
  final Address karmaTreasury = sm.createAccount().getAddress();
  final Address subsidyRouter = subsidyRouterAccount.getAddress();
  final Address karmaDAO = sm.createAccount().getAddress();
  final Address treasuryAddress = sm.createAccount().getAddress();
  final Address initialOwner = owner.getAddress();
  final BigInteger[] tierCeilings = {
    EXA.multiply(BigInteger.valueOf(10)),
    EXA.multiply(BigInteger.valueOf(20))
  };
  final BigInteger[] fees = {
    BigInteger.valueOf(33300),
    BigInteger.valueOf(66600)
  };

  ScoreSpy<PrincipalToken> principalToken;
  ScoreSpy<KarmaOracleMock> karmaOracle;
  ScoreSpy<PayoutToken> payoutToken;
  ScoreSpy<KarmaCustomTreasury> customTreasury;

  @BeforeEach
  void setup() throws Exception {
    // Deploys
    principalToken = deploy(PrincipalToken.class);
    payoutToken = deploy(PayoutToken.class);
    customTreasury = deploy(KarmaCustomTreasury.class, payoutToken.getAddress(), initialOwner);
    karmaOracle = deploy(KarmaOracleMock.class);

    setup_bond(
      customTreasury.getAddress(),
      payoutToken.getAddress(),
      principalToken.getAddress(),
      karmaTreasury,
      karmaOracle.getAddress(),
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }

  @Test
  void testRedeem () {
    BigInteger amount = EXA.divide(BigInteger.TEN);
    BigInteger maxPrice = BigInteger.valueOf(6000);
    Address depositor = owner.getAddress();

    // send some principal token to alice
    IRC2Client.transfer(principalToken.score, owner, alice, amount);
    // send some payout token to the custom treasury
    IRC2Client.transfer(payoutToken.score, owner, customTreasury.getAccount(), EXA.multiply(BigInteger.valueOf(1000)), JSONUtils.method("funding"));

    // Initialize bond
    BigInteger controlVariable = BigInteger.valueOf(400000);
    long vestingTerm = 302400; // 1 week
    BigInteger minimumPrice = BigInteger.valueOf(5403);
    BigInteger maxPayout = BigInteger.valueOf(500);
    BigInteger maxDebt = EXA.multiply(BigInteger.valueOf(5000));
    BigInteger initialDebt = new BigInteger("1560000000");
    BigInteger maxDiscount = new BigInteger("100"); // in thousands 100 = 10%

    KarmaCustomBondClient.setBondTerms(bond.score, owner, KarmaCustomBond.VESTING, BigInteger.valueOf(302400));
    KarmaCustomBondClient.initializeBond (
      bond.score,
      owner,
      controlVariable,
      vestingTerm,
      minimumPrice,
      maxPayout,
      maxDebt,
      initialDebt,
      maxDiscount
    );

    // Enable the bond contract in the custom treasury
    KarmaCustomTreasuryClient.toggleBondContract(customTreasury.score, owner, bond.getAddress());

    // Do the deposit from alice
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );

    // Redeem immediatly in the same block : there should be no payout
    BigInteger ownerBefore = IRC2Client.balanceOf(payoutToken.score, owner);
    KarmaCustomBondClient.redeem (
      bond.score,
      alice,
      owner.getAddress()
      );
    BigInteger ownerAfter = IRC2Client.balanceOf(payoutToken.score, owner);
    assertEquals(ownerBefore, ownerAfter);

    // Sleep 1 day
    SleepUtils.sleep(TimeUtils.ONE_DAY);

    ownerBefore = IRC2Client.balanceOf(payoutToken.score, owner);
    KarmaCustomBondClient.redeem (
      bond.score,
      alice,
      owner.getAddress()
      );
    ownerAfter = IRC2Client.balanceOf(payoutToken.score, owner);
    // depositor should have received payout tokens
    assertTrue(ownerAfter.compareTo(ownerBefore) > 0);

    // Sleep 1 week (vestingTerm)
    SleepUtils.sleep(TimeUtils.ONE_WEEK);

    ownerBefore = IRC2Client.balanceOf(payoutToken.score, owner);
    KarmaCustomBondClient.redeem (
      bond.score,
      alice,
      owner.getAddress()
    );
    ownerAfter = IRC2Client.balanceOf(payoutToken.score, owner);
    // depositor should have received payout tokens
    assertTrue(ownerAfter.compareTo(ownerBefore) > 0);

    // There shouldn't be anything left to redeem, the bond info should have been deleted
    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.redeem (
        bond.score,
        alice,
        owner.getAddress()
      ),
      "redeem: no bond registered for depositor"
    );
  }
}
