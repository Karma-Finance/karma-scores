package dao.karma.custombond;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.structs.factorystorage.BondDetails;
import score.Address;

public class DeployTest extends KarmaCustomBondTest {

  // Fake contracts
  final Address customTreasury = sm.createAccount().getAddress();
  final Address karmaTreasury = sm.createAccount().getAddress();
  final Address karmaOracle = sm.createAccount().getAddress();
  final Address subsidyRouter = sm.createAccount().getAddress();
  final Address karmaDAO = sm.createAccount().getAddress();
  final Address payoutToken = sm.createAccount().getAddress();
  final Address principalToken = sm.createAccount().getAddress();
  final Address treasuryAddress = sm.createAccount().getAddress();
  final Address bondAddress = sm.createAccount().getAddress();
  final Address initialOwner = sm.createAccount().getAddress();
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

  }

  @Test
  void testDeploy () throws Exception {
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
}
