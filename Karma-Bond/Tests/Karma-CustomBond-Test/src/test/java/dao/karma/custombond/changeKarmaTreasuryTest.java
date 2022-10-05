package dao.karma.custombond;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaCustomBondClient;
import dao.karma.structs.factorystorage.BondDetails;
import dao.karma.test.AssertUtils;
import score.Address;

public class changeKarmaTreasuryTest extends KarmaCustomBondTest {

  // Fake contracts
  final Account karmaDAOAccount = sm.createAccount();
  final Address customTreasury = sm.createAccount().getAddress();
  final Address karmaTreasury = sm.createAccount().getAddress();
  final Address karmaOracle = sm.createAccount().getAddress();
  final Address subsidyRouter = sm.createAccount().getAddress();
  final Address karmaDAO = karmaDAOAccount.getAddress();
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
      karmaOracle,
      subsidyRouter,
      initialOwner,
      karmaDAO,
      tierCeilings,
      fees
    );
  }

  @Test
  void testChangeKarmaTreasury () {
    KarmaCustomBondClient.changeKarmaTreasury (
      bond.score,
      karmaDAOAccount,
      karmaTreasury
    );
  }

  @Test
  void testNotKarmaDAO () {
    AssertUtils.assertThrowsMessage(() ->
      KarmaCustomBondClient.changeKarmaTreasury (
        bond.score,
        alice,
        karmaTreasury
      ),
      "checkKarmaDao: only KarmaDAO can call this method"
    );
  }
}
