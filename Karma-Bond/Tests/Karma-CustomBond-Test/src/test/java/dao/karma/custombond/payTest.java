package dao.karma.custombond;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.custombond.mocks.KarmaOracleMock;
import dao.karma.custombond.tokens.PayoutToken;
import dao.karma.custombond.tokens.PrincipalToken;
import dao.karma.customtreasury.KarmaCustomTreasury;
import dao.karma.test.AssertUtils;
import dao.karma.test.ScoreSpy;
import score.Address;

public class payTest extends KarmaCustomBondTest {
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
  void testPay () {
    // Do the funding from owner
    KarmaCustomBondClient.pay (
      bond.score,
      owner, 
      payoutToken.score,
      BigInteger.valueOf(1000)
    );
  }

  @Test
  void testOnlyPayout () {
    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.pay (
        bond.score,
        owner, 
        principalToken.score,
        BigInteger.valueOf(1000)
      ),
      "pay: Only payout token is accepted as payment"
    );
  }
}
