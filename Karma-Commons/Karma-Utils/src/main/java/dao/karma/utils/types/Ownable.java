package dao.karma.utils.types;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

public class Ownable {
  
  // ================================================
  // Consts
  // ================================================
  // Contract class name
  public static final String NAME = "Ownable";

  // ================================================
  // DB Variables
  // ================================================
  protected VarDB<Address> policy = Context.newVarDB(NAME + "_policy", Address.class);

  public Ownable () {
    final Address caller = Context.getCaller();

    if (this.policy.get() == null) {
      policy.set(caller);
    }
  }

  protected void onlyPolicy () {
    final Address caller = Context.getCaller();

    Context.require(policy.get().equals(caller),
      "onlyPolicy: caller is not the owner");
  }

  @External
  public void transferManagement (Address newOwner) {
    Context.require(!newOwner.equals(ZERO_ADDRESS), 
      "transferManagement: newOwner cannot be zero address");
    
    this.policy.set(newOwner);
  }
}
