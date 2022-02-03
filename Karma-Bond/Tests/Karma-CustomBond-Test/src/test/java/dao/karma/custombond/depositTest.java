package dao.karma.custombond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.clients.KarmaCustomTreasuryClient;
import dao.karma.custombond.tokens.PayoutToken;
import dao.karma.custombond.tokens.PrincipalToken;
import dao.karma.customtreasury.KarmaCustomTreasury;
import dao.karma.standards.token.irc2.client.IRC2Client;
import dao.karma.test.ScoreSpy;
import dao.karma.utils.JSONUtils;
import score.Address;

public class depositTest extends KarmaCustomBondTest {
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
  ScoreSpy<PayoutToken> payoutToken;
  ScoreSpy<KarmaCustomTreasury> customTreasury;

  @BeforeEach
  void setup() throws Exception {
    // Deploys
    principalToken = deploy(PrincipalToken.class);
    payoutToken = deploy(PayoutToken.class);
    customTreasury = deploy(KarmaCustomTreasury.class, payoutToken.getAddress(), initialOwner);

    setup_bond(
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

  @Test
  void testDeposit () {
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

    KarmaCustomBondClient.setBondTerms(bond.score, owner, KarmaCustomBond.VESTING, BigInteger.valueOf(302400));
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
    BigInteger alicePrincipalBefore = IRC2Client.balanceOf(principalToken.score, alice);
    BigInteger bondPayoutBefore     = IRC2Client.balanceOf(payoutToken.score, bond.getAddress());

    reset(payoutToken.spy);
    reset(principalToken.spy);
    KarmaCustomBondClient.deposit(
      bond.score,
      alice, 
      principalToken.score,
      amount,
      maxPrice,
      depositor
    );
    BigInteger alicePrincipalAfter = IRC2Client.balanceOf(principalToken.score, alice);
    BigInteger bondPayoutAfter     = IRC2Client.balanceOf(payoutToken.score, bond.getAddress());

    // verify token transfers
    ArgumentCaptor<Address> from = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Address> to = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<byte[]> data = ArgumentCaptor.forClass(byte[].class);

    // Alice -> Bond
    verify(principalToken.spy, atLeastOnce()).Transfer(from.capture(), to.capture(), _amount.capture(), data.capture());
    assertEquals(from.getValue(), alice.getAddress());
    assertEquals(to.getValue(), bond.getAddress());
    assertEquals(_amount.getValue(), amount);

    assertEquals(alicePrincipalBefore.subtract(amount), alicePrincipalAfter);
    assertEquals(new BigInteger("0"), bondPayoutBefore);
    assertEquals(new BigInteger("178919119007958563209"), bondPayoutAfter);
  }
}
