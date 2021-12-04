package dao.karma.utils.types;

import static dao.karma.utils.AddressUtils.ZERO_ADDRESS;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public class Ownable {
  
  // ================================================
  // Consts
  // ================================================
  // Contract class name
  public static final String NAME = "Ownable";

  // ================================================
  // Event Log
  // ================================================
  @EventLog(indexed = 2)
  public void OwnershipPushed(Address previousOwner, Address newOwner) {}
  
  @EventLog(indexed = 2)
  public void OwnershipPulled(Address previousOwner, Address newOwner) {}

  // ================================================
  // DB Variables
  // ================================================
  protected VarDB<Address> owner = Context.newVarDB(NAME + "_owner", Address.class);
  protected VarDB<Address> newOwner = Context.newVarDB(NAME + "_newOwner", Address.class);

  public Ownable () {
    final Address caller = Context.getCaller();

    if (this.owner.get() == null) {
      owner.set(caller);
      this.OwnershipPushed(ZERO_ADDRESS, caller);
    }
  }

  protected void onlyPolicy () {
    final Address caller = Context.getCaller();

    Context.require(owner.get().equals(caller),
      "onlyPolicy: caller is not the owner");
  }

  @External
  public void renounceManagement() {
    onlyPolicy();

    this.OwnershipPushed (this.owner.get(), ZERO_ADDRESS);
    this.owner.set(ZERO_ADDRESS);
  }
  
  @External
  public void pushManagement (Address newOwner) {
    onlyPolicy();

    Context.require(!newOwner.equals(ZERO_ADDRESS),
      "Ownable: new owner is the zero address");

    this.OwnershipPushed (this.owner.get(), newOwner );
    this.newOwner.set(newOwner);
  }

  @External
  public void pullManagement() {
    final Address caller = Context.getCaller();
    var newOwner = this.newOwner.get();

    Context.require(caller.equals(newOwner), 
      "Ownable: must be new owner to pull");

    this.OwnershipPulled(this.owner.get(), newOwner);
    this.owner.set(newOwner);
  }

  // ================================================
  // Public variable getters
  // ================================================
  @External(readonly = true)
  public Address policy () {
    return this.owner.get();
  }
}
