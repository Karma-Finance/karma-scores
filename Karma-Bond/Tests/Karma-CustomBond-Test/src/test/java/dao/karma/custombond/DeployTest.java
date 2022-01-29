package dao.karma.custombond;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.structs.factorystorage.BondDetails;
import score.Address;

public class DeployTest extends KarmaCustomBondTest {
  
  // Fake contracts
  Address customTreasury = sm.createAccount().getAddress();
  Address karmaTreasury = sm.createAccount().getAddress();
  Address subsidyRouter = sm.createAccount().getAddress();
  Address karmaDAO = sm.createAccount().getAddress();
  Address payoutToken = sm.createAccount().getAddress();
  Address principalToken = sm.createAccount().getAddress();
  Address treasuryAddress = sm.createAccount().getAddress();
  Address bondAddress = sm.createAccount().getAddress();
  Address initialOwner = sm.createAccount().getAddress();
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
  void testDeploy () {

  }
}
