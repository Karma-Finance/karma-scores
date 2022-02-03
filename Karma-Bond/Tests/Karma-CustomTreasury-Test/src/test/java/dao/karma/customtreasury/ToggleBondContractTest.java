package dao.karma.customtreasury;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.clients.KarmaCustomTreasuryClient;
import dao.karma.standards.token.irc2.client.IRC2Client;
import dao.karma.test.AssertUtils;
import dao.karma.test.ScoreSpy;
import dao.karma.test.tokens.Bnusd;
import dao.karma.test.tokens.Usdc;
import dao.karma.utils.JSONUtils;

public class ToggleBondContractTest extends KarmaCustomTreasuryTest {
  
  ScoreSpy<Bnusd> depositToken;
  ScoreSpy<Bnusd> payoutToken;

  Account bondContract;

  @BeforeEach
  void setup() throws Exception {
    // Fake bond contract
    bondContract = sm.createAccount();

    // Deploy tokens
    depositToken = deploy(Usdc.class, "USDC", "USDC", 18);
    payoutToken = deploy(Bnusd.class, "bnUSD", "bnUSD", 18);

    setup_treasury(payoutToken.getAddress(), owner.getAddress());
    
    // Mint depositToken to bondingContract
    IRC2Client.mint(depositToken.score, owner, EXA.multiply(BigInteger.valueOf(1000)));
    IRC2Client.transfer(depositToken.score, owner, bondContract, BigInteger.valueOf(1000));
    // Mint payoutToken to treasury 
    IRC2Client.mint(payoutToken.score, owner, EXA.multiply(BigInteger.valueOf(1000)));
    IRC2Client.transfer(payoutToken.score, owner, treasury.getAddress(), BigInteger.valueOf(1000), JSONUtils.method("funding"));
  }

  @Test
  void testToggleBondContract () {
    // Check initial value
    assertEquals(false, KarmaCustomTreasuryClient.bondContract(treasury.score, bondContract.getAddress()));

    // toggle
    KarmaCustomTreasuryClient.toggleBondContract(treasury.score, owner, bondContract.getAddress());

    // Check the new toggle
    assertEquals(true, KarmaCustomTreasuryClient.bondContract(treasury.score, bondContract.getAddress()));

    // toggle again
    KarmaCustomTreasuryClient.toggleBondContract(treasury.score, owner, bondContract.getAddress());

    // Check the new toggle
    assertEquals(false, KarmaCustomTreasuryClient.bondContract(treasury.score, bondContract.getAddress()));
  }

  @Test
  void testNotPolicy () {
    // Only owner can call toggleBondContract, alice is not owner
    AssertUtils.assertThrowsMessage(
      () -> KarmaCustomTreasuryClient.toggleBondContract(treasury.score, alice, bondContract.getAddress()), 
      "onlyPolicy: caller is not the owner");
  }
}
