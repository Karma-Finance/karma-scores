package dao.karma.custombond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.test.AssertUtils;
import score.Address;

public class paySubsidyTest extends KarmaCustomBondTest {
  // Fake contracts
  final Account subsidyRouterAccount = sm.createAccount();
  final Address customTreasury = sm.createAccount().getAddress();
  final Address karmaTreasury = sm.createAccount().getAddress();
  final Address subsidyRouter = subsidyRouterAccount.getAddress();
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
  void testPaySubsidy () {
    reset(bond.spy);
    KarmaCustomBondClient.paySubsidy (
      bond.score,
      subsidyRouterAccount
    );
    ArgumentCaptor<BigInteger> oldPayout = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> newPayout = ArgumentCaptor.forClass(BigInteger.class);
    verify(bond.spy).PayoutUpdate(oldPayout.capture(), newPayout.capture());

    // check if reset
    assertEquals(BigInteger.ZERO, newPayout.getValue());
    BigInteger payoutSinceLastSubsidy = KarmaCustomBondClient.payoutSinceLastSubsidy(bond.score);
    assertEquals(BigInteger.ZERO, payoutSinceLastSubsidy);
  }

  @Test
  void testNotSubsidy () {
    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.paySubsidy (
        bond.score,
        alice
      ),
      "checkSubsidy: only Subsidy Router can call this method"
    );
  }
}
