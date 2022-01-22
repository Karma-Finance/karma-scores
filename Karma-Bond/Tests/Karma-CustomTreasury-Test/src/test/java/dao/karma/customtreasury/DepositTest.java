package dao.karma.customtreasury;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.karma.customtreasury.client.KarmaCustomTreasuryClient;
import dao.karma.standards.token.irc2.client.IRC2Client;
import dao.karma.test.AssertUtils;
import dao.karma.test.ScoreSpy;
import dao.karma.test.tokens.Bnusd;
import dao.karma.test.tokens.Usdc;
import dao.karma.utils.JSONUtils;

public class DepositTest extends KarmaCustomTreasuryTest {
  
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
  void testDeposit () {
    KarmaCustomTreasuryClient.toggleBondContract(treasury.score, owner, bondContract.getAddress());
    KarmaCustomTreasuryClient.deposit(treasury.score, depositToken.score, bondContract, BigInteger.valueOf(1000), BigInteger.valueOf(1000));
  }

  @Test
  void testDepositNotBond () {
    AssertUtils.assertThrowsMessage(
      () -> KarmaCustomTreasuryClient.deposit(treasury.score, depositToken.score, owner, BigInteger.valueOf(1000), BigInteger.valueOf(1000)), 
      "deposit: caller is not a bond contract"
    );
  }
}
