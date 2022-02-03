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

public class WithdrawTest extends KarmaCustomTreasuryTest {
  
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
  
    // Do the deposit to the treasury
    BigInteger amountPayoutToken = BigInteger.valueOf(1000);
    KarmaCustomTreasuryClient.toggleBondContract(treasury.score, owner, bondContract.getAddress());
    KarmaCustomTreasuryClient.deposit(treasury.score, depositToken.score, bondContract, BigInteger.valueOf(1000), amountPayoutToken);
  }

  @Test
  void testWithdraw () {
    // Get the original balance
    BigInteger oldBalance = IRC2Client.balanceOf(depositToken.score, alice.getAddress());
    BigInteger amountWithdrawn = BigInteger.valueOf(1000);

    KarmaCustomTreasuryClient.withdraw(treasury.score, owner, depositToken.getAddress(), alice.getAddress(), amountWithdrawn);

    // Check the withdraw funds
    BigInteger newBalance = IRC2Client.balanceOf(depositToken.score, alice.getAddress());
    assertEquals(oldBalance.add(amountWithdrawn), newBalance);
  }

  @Test
  void testNotPolicy () {
    BigInteger amountWithdrawn = BigInteger.valueOf(1000);

    // Only owner can call withdraw, alice is not owner
    AssertUtils.assertThrowsMessage(
      () -> KarmaCustomTreasuryClient.withdraw(treasury.score, alice, depositToken.getAddress(), alice.getAddress(), amountWithdrawn), 
      "onlyPolicy: caller is not the owner");
  }
}
