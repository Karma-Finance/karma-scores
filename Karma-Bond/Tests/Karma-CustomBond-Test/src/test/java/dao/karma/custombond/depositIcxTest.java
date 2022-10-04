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

import dao.karma.clients.IRC2Client;
import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.clients.KarmaCustomTreasuryClient;
import dao.karma.custombond.mocks.KarmaOracleMock;
import dao.karma.custombond.mocks.KarmaTreasuryMock;
import dao.karma.custombond.tokens.PrincipalToken;
import dao.karma.customtreasury.KarmaCustomTreasury;
import dao.karma.test.ScoreSpy;
import score.Address;

public class depositIcxTest extends KarmaCustomBondTest {
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
  ScoreSpy<KarmaOracleMock> karmaOracle;
  Address payoutToken = dao.karma.utils.ICX.TOKEN_ADDRESS;
  ScoreSpy<KarmaCustomTreasury> customTreasury;

  @BeforeEach
  void setup() throws Exception {
    // Deploys
    principalToken = deploy(PrincipalToken.class);
    customTreasury = deploy(KarmaCustomTreasury.class, payoutToken, initialOwner);
    karmaTreasury = deploy(KarmaTreasuryMock.class);
    karmaOracle = deploy(KarmaOracleMock.class);

    setup_bond(
      customTreasury.getAddress(),
      payoutToken,
      principalToken.getAddress(),
      karmaTreasury.getAddress(),
      karmaOracle.getAddress(),
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }

  @Test
  void testDeposit () {
    BigInteger amount = EXA.multiply(BigInteger.valueOf(1_000));
    BigInteger maxPrice = BigInteger.valueOf(10_000);
    Address depositor = owner.getAddress();

    // send some principal token to alice
    IRC2Client.transfer(principalToken.score, owner, alice, amount);
    // send some payout token to the custom treasury
    customTreasury.getAccount().addBalance("ICX", EXA.multiply(BigInteger.valueOf(2_000_000)));

    // Initialize bond
    BigInteger controlVariable = BigInteger.valueOf(4_000);
    long vestingTerm = 302400; // 1 week
    BigInteger minimumPrice = BigInteger.valueOf(5_000);
    BigInteger maxPayout = EXA.multiply(BigInteger.valueOf(5_000));
    BigInteger maxDebt = EXA.multiply(BigInteger.valueOf(2_000));
    BigInteger initialDebt = new BigInteger("15600");
    BigInteger maxDiscount = new BigInteger("100"); // in thousands 100 = 10%

    KarmaCustomBondClient.setBondTerms(bond.score, owner, KarmaCustomBond.VESTING, BigInteger.valueOf(vestingTerm));
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
    BigInteger alicePrincipalBefore = IRC2Client.balanceOf(principalToken.score, alice);
    BigInteger bondPayoutBefore     = bond.getAccount().getBalance();

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
    BigInteger bondPayoutAfter     = bond.getAccount().getBalance();

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
    assertEquals(new BigInteger("1933400000000000000000000"), bondPayoutAfter);
  }
}
