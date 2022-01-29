package dao.karma.factory;

import dao.karma.structs.bond.TreasuryBond;
import dao.karma.structs.factorystorage.BondDetails;
import score.annotation.External;

public class FactoryStorageMock {
  @External
  public TreasuryBond pushBond (BondDetails bond) {
     return new TreasuryBond (bond.treasuryAddress, bond.bondAddress);
  }
}
