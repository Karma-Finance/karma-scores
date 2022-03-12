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
import dao.karma.custombond.mocks.KarmaTreasuryMock;
import dao.karma.custombond.tokens.PrincipalToken;
import dao.karma.customtreasury.KarmaCustomTreasury;
import dao.karma.test.AssertUtils;
import dao.karma.test.ScoreSpy;
import dao.karma.test.SleepUtils;
import dao.karma.utils.TimeUtils;
import score.Address;

public class redeemIcxTest extends KarmaCustomBondTest {
  // Fake contracts
  final Account subsidyRouterAccount = sm.createAccount();
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
  ScoreSpy<KarmaTreasuryMock> karmaTreasury;
  Address payoutToken = dao.karma.utils.ICX.TOKEN_ADDRESS;
  ScoreSpy<KarmaCustomTreasury> customTreasury;

  @BeforeEach
  void setup() throws Exception {
    // Deploys
    principalToken = deploy(PrincipalToken.class);
    customTreasury = deploy(KarmaCustomTreasury.class, payoutToken, initialOwner);
    karmaTreasury = deploy(KarmaTreasuryMock.class);

    setup_bond(
      customTreasury.getAddress(),
      payoutToken,
      principalToken.getAddress(),
      karmaTreasury.getAddress(),
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }

  @Test
  void testRedeem () {
    BigInteger amount = EXA.multiply(BigInteger.valueOf(1_000));
    BigInteger maxPrice = BigInteger.valueOf(10_000);
    Address depositor = owner.getAddress();

    // send some principal token to alice
    IRC2Client.transfer(principalToken.score, owner, alice, amount);
    // send some payout token
    customTreasury.getAccount().addBalance("ICX", EXA.multiply(BigInteger.valueOf(2_000_000)));

    // Initialize bond
    BigInteger controlVariable = BigInteger.valueOf(4_000);
    long vestingTerm = 302400; // 1 week
    BigInteger minimumPrice = BigInteger.valueOf(5_000);
    BigInteger maxPayout = EXA.multiply(BigInteger.valueOf(5_000));
    BigInteger maxDebt = EXA.multiply(BigInteger.valueOf(2_000));
    BigInteger initialDebt = new BigInteger("15600");

    KarmaCustomBondClient.setBondTerms(bond.score, owner, KarmaCustomBond.VESTING, BigInteger.valueOf(vestingTerm));
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
    BigInteger ownerBefore = owner.getBalance();
    KarmaCustomBondClient.redeem (
      bond.score,
      alice,
      owner.getAddress()
      );
    BigInteger ownerAfter = owner.getBalance();
    assertEquals(ownerBefore, ownerAfter);

    // Sleep 1 day
    SleepUtils.sleep(TimeUtils.ONE_DAY);

    ownerBefore = owner.getBalance();
    KarmaCustomBondClient.redeem (
      bond.score,
      alice,
      owner.getAddress()
    );
    ownerAfter = owner.getBalance();
    // depositor should have received payout tokens
    assertTrue(ownerAfter.compareTo(ownerBefore) > 0);

    // Sleep 1 week (vestingTerm)
    SleepUtils.sleep(TimeUtils.ONE_WEEK);

    ownerBefore = owner.getBalance();
    KarmaCustomBondClient.redeem (
      bond.score,
      alice,
      owner.getAddress()
    );
    ownerAfter = owner.getBalance();
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
