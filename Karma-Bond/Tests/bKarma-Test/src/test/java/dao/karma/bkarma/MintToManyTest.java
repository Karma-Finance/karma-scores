package dao.karma.bkarma;

import static java.math.BigInteger.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.iconloop.score.test.Account;
import dao.karma.clients.IRC2Client;
import dao.karma.utils.MathUtils;
import score.Address;

public class MintToManyTest extends BKarmaTest {
  
  @BeforeEach
  void setup() throws Exception {
    setup_bkarma("bKarma", "bKarma", 18);
  }

  @Test
  void testMintToMany () {
    Account alice = sm.createAccount();
    Account bob = sm.createAccount();
    Account charlie = sm.createAccount();

    Address[] addresses = {
      alice.getAddress(),
      bob.getAddress(),
      charlie.getAddress()
    };

    BigInteger[] amounts = {
      BigInteger.valueOf(100),
      BigInteger.valueOf(200),
      BigInteger.valueOf(300)
    };

    // Check balances
    assertEquals(ZERO, IRC2Client.balanceOf(bKarma.score, alice));
    assertEquals(ZERO, IRC2Client.balanceOf(bKarma.score, bob));
    assertEquals(ZERO, IRC2Client.balanceOf(bKarma.score, charlie));
    assertEquals(ZERO, IRC2Client.totalSupply(bKarma.score));

    bKarma.invoke(owner, "mintToMany", addresses, amounts);

    // Check balances
    assertEquals(amounts[0], IRC2Client.balanceOf(bKarma.score, alice));
    assertEquals(amounts[1], IRC2Client.balanceOf(bKarma.score, bob));
    assertEquals(amounts[2], IRC2Client.balanceOf(bKarma.score, charlie));
    assertEquals(MathUtils.sum(amounts), IRC2Client.totalSupply(bKarma.score));
    
    // Call again and make sure new balances are updated
    bKarma.invoke(owner, "mintToMany", addresses, amounts);
  
    // Check balances again
    assertEquals(TWO.multiply(amounts[0]), IRC2Client.balanceOf(bKarma.score, alice));
    assertEquals(TWO.multiply(amounts[1]), IRC2Client.balanceOf(bKarma.score, bob));
    assertEquals(TWO.multiply(amounts[2]), IRC2Client.balanceOf(bKarma.score, charlie));
    assertEquals(TWO.multiply(MathUtils.sum(amounts)), IRC2Client.totalSupply(bKarma.score));
  }

  @Test
  void testMintToManyOnlyMinter () {
    Account alice = sm.createAccount();
    Account bob = sm.createAccount();
    Account charlie = sm.createAccount();
    Account eve = sm.createAccount();
    Account minter = sm.createAccount();

    Address[] addresses = {
      alice.getAddress(),
      bob.getAddress(),
      charlie.getAddress()
    };

    BigInteger[] amounts = {
      BigInteger.valueOf(100),
      BigInteger.valueOf(200),
      BigInteger.valueOf(300)
    };

    // Eve shouldn't be able to call
    assertThrows(AssertionError.class, 
      () -> bKarma.invoke(eve, "mintToMany", addresses, amounts));

    bKarma.invoke(owner, "setMinter", minter.getAddress());

    // Still shouldn't be able to call it
    assertThrows(AssertionError.class, 
    () -> bKarma.invoke(eve, "mintToMany", addresses, amounts));
    
    // owner neither
    assertThrows(AssertionError.class, 
      () -> bKarma.invoke(owner, "mintToMany", addresses, amounts));

    // Only minter
    bKarma.invoke(minter, "mintToMany", addresses, amounts);
  }
}
