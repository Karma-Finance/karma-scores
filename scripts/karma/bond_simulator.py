import sys

from iconsdk.icon_service import IconService
from iconsdk.providers.http_provider import HTTPProvider
icon_service = IconService(HTTPProvider("https://berlin.net.solidwallet.io", 3))

DECIMALS_PRECISION_EXPONENT = 5
TRUE_BOND_PRICE_PRECISION = 10**6
CURRENT_BOND_PRICE_PRECISION = 10**7
VESTING = 0
PAYOUT = 1
DEBT = 2
BOND_TERMS_VESTING_MIN_SECONDS = 36 * 3600 # 36 hours
BOND_TERMS_PAYOUT_MAX_PERCENT = 1000

class FeeTiers:
  def __init__(self, tierCeilings, fees):
    self.tierCeilings = tierCeilings
    self.fees = fees

class Terms:
  @staticmethod
  def empty():
    return Terms(0, 0, 0, 0, 0, 0)

  def __init__ (
    self, 
    controlVariable,
    vestingTerm,
    minimumPrice,
    maxPayout,
    maxDebt,
    maxDiscount
  ):
    self.controlVariable = controlVariable
    self.vestingTerm = vestingTerm
    self.minimumPrice = minimumPrice
    self.maxPayout = maxPayout
    self.maxDebt = maxDebt
    self.maxDiscount = maxDiscount

class IToken:
  @staticmethod
  def decimals(token):
    if token == "cx0fa7815de5b2be6e51dc52caa0dc556012ae0f98":
      return 18

  @staticmethod
  def totalSupply(token):
    if token == "cx0fa7815de5b2be6e51dc52caa0dc556012ae0f98":
      return 150200000000000000000000000

class Context:
  @staticmethod
  def getBlockHeight():
    return int(icon_service.get_block('latest')['height'])

  @staticmethod
  def require(condition, message):
    if not condition:
      Context.revert(message)

  @staticmethod
  def revert(message):
    print("Condition failed: " + message)
    sys.exit(0)

class Contract:
  
  def __init__(self,
    customTreasury,
    payoutToken,
    principalToken,
    principalPoolId,
    karmaTreasury,
    karmaOracle,
    subsidyRouter,
    initialOwner,
    karmaDAO,
    tierCeilings,
    fees
  ):
    self.terms = Terms.empty()
    self.customTreasury = customTreasury
    self.payoutToken = payoutToken
    self.principalToken = principalToken
    self.principalPoolId = principalPoolId
    self.karmaTreasury = karmaTreasury
    self.karmaOracle = karmaOracle
    self.subsidyRouter = subsidyRouter
    self.initialOwner = initialOwner
    self.karmaDAO = karmaDAO
    self.feeTiers = []
    for i in range(len(tierCeilings)):
      self.feeTiers.append(
        FeeTiers(
          int(tierCeilings[i], 0), 
          int(fees[i], 0)
        )
      )
    self.totalDebt = 0
    self.lastDecay = 0
    self.totalPrincipalBonded = 0

  def initializeBond (
    self,
    controlVariable,
    vestingTerm, # in blocks
    minimumPrice,
    maxPayout,
    maxDebt,
    initialDebt,
    maxDiscount
  ):
    Context.require(self.currentDebt() == 0, 
      "initializeBond: Debt must be 0 for initialization")

    self.terms = Terms(
      controlVariable,
      vestingTerm,
      minimumPrice,
      maxPayout,
      maxDebt,
      maxDiscount
    )

    self.totalDebt = initialDebt
    self.lastDecay = 0x9b3f91 # Context.getBlockHeight()

  def setBondTerms(self, parameter, input):
    if parameter == VESTING:
      averageBlockTimeInSeconds = 2
      minVesting = BOND_TERMS_VESTING_MIN_SECONDS // averageBlockTimeInSeconds
      Context.require(input >= minVesting, 
        f"setBondTerms: Vesting must be longer than {BOND_TERMS_VESTING_MIN_SECONDS} seconds")
      self.terms.vestingTerm = input

    elif parameter == PAYOUT:
      Context.require(input <= BOND_TERMS_PAYOUT_MAX_PERCENT,
          "setBondTerms: Payout cannot be above 1 percent");
      self.terms.maxPayout = input
    elif parameter == DEBT:
      self.terms.maxDebt = input
    else:
      Context.revert("setBondTerms: invalid parameter")

  def debtDecay(self):
    terms = self.terms

    Context.require(terms.vestingTerm != 0, 
      "debtDecay: The vesting term must be initialized first")

    totalDebt = self.totalDebt
    blockHeight = Context.getBlockHeight()
    blocksSinceLast = blockHeight - self.lastDecay
    vestingTerm = terms.vestingTerm
    decay = totalDebt * blocksSinceLast // vestingTerm
    if decay > totalDebt:
      decay = totalDebt
    return decay

  def currentDebt(self):
    # currentDebt = totalDebt() - debtDecay()
    return self.totalDebt - self.debtDecay()

  def debtRatio(self):
    # debtRatio = currentDebt() * IRC2(payoutToken).decimals() / IRC2(payoutToken).totalSupply()
    return self.currentDebt() * 10**(IToken.decimals(self.payoutToken)) // IToken.totalSupply(self.payoutToken)

  def currentKarmaFee(self):
    tierLength = len(self.feeTiers)
    totalPrincipalBonded = self.totalPrincipalBonded
    for i in range(len(self.feeTiers)):
      feeTier = self.feeTiers[i]
      if totalPrincipalBonded < feeTier.tierCeilings \
        or i == (tierLength - 1):
        return feeTier.fees

    return 0

  def trueBondPrice(self, bondPrice = 0):
    if bondPrice == 0:
      # recursion only if no parameter given
      return self.bondPrice()
    return bondPrice + (bondPrice * self.currentKarmaFee() // TRUE_BOND_PRICE_PRECISION)

  def lpMarketUsdPrice(self):
    return 0x24323fc899d122cd

  def payoutTokenMarketPriceUSD(self):
    return 0x16518914f26d22cd

  def bondPriceUSD(self, bondPrice):
    return self.trueBondPrice(bondPrice) * self.lpMarketUsdPrice() // CURRENT_BOND_PRICE_PRECISION

  def currentBondDiscount(self, bondPrice = 0):
    bondPriceUSD = self.bondPriceUSD(bondPrice)
    payoutTokenMarketPriceUSD = self.payoutTokenMarketPriceUSD()
    return (payoutTokenMarketPriceUSD - bondPriceUSD) * 10**7 // (payoutTokenMarketPriceUSD)

  def bondPrice(self):
    terms = self.terms

    # price = BCV * debtRatio / (10**(IRC2(payoutToken).decimals()-DECIMALS_PRECISION))
    numerator = terms.controlVariable * self.debtRatio()
    denominator = 10**(IToken.decimals(self.payoutToken) - DECIMALS_PRECISION_EXPONENT)
    price = numerator // denominator
    maxDiscount = terms.maxDiscount

    # check if max discount is greater than 0 and increase price to fit the capped discount
    # NOTE: if minimumPrice is set in the terms capped discount is not applied!
    if price < terms.minimumPrice:
      price = terms.minimumPrice
    elif maxDiscount > 0:
      bondDiscount = self.currentBondDiscount(price)
      
      # if bond discount is greater than max discount, increase bond price to fit the max discount
      if bondDiscount > maxDiscount:
        payoutTokenMarketPriceUSD = self.payoutTokenMarketPriceUSD()

        newTrueBondPrice = ((payoutTokenMarketPriceUSD * 10**3) \
                         - (maxDiscount * payoutTokenMarketPriceUSD)) \
                         * 10**4 // self.lpMarketUsdPrice()

        newBondPrice = newTrueBondPrice * TRUE_BOND_PRICE_PRECISION // (TRUE_BOND_PRICE_PRECISION + self.currentKarmaFee())

        # only apply new bond price if it is higher than the old one, this should mitigate oracle risk
        # by defaulting to the un-capped bond price
        if newBondPrice > price:
          price = newBondPrice

    return price

if __name__ == '__main__':
  contract = Contract (
		customTreasury = "cx50aac9abdfc295263fb75d5478d1489a1b049394",
		fees = [
			"0x8214",
			"0x8214"
		],
		initialOwner = "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",
		karmaDAO = "hxa05b1a31d6ec95a5b22a296405ea9ac5e9fe726e",
		karmaOracle = "cx373129fb68b4444d4080aad7edb6bee4b2c488b7",
		karmaTreasury = "hxdca38f5010b3cb56032ff0778d6051c5bfb27430",
		payoutToken = "cx0fa7815de5b2be6e51dc52caa0dc556012ae0f98",
		principalPoolId = "0xd",
		principalToken = "cx4d3b86709c387dec2927158c0377ecabe002f503",
		subsidyRouter = "cx5f1b9c1ae0cb1247024a4028f03dbec75fa8ce54",
		tierCeilings = [
			"0xde0b6b3a7640000",
			"0x1bc16d674ec80000"
		]
  )

  contract.setBondTerms(VESTING, 0x49d40)
  contract.initializeBond (
		controlVariable = 0xdda01,
		initialDebt = 0x202fefbf2d7c2f00000,
		maxDebt = 0xed2b525841adfc00000,
		maxDiscount = 0x32,
		maxPayout = 0x1,
		minimumPrice = 0x0,
		vestingTerm = 0x49d40,
  )

  bondPrice = contract.bondPrice()
  bondPriceUSD = contract.bondPriceUSD(bondPrice)
  trueBondPrice = contract.trueBondPrice(bondPrice)
  debtRatio = contract.debtRatio()
  currentBondDiscount = contract.currentBondDiscount(bondPrice)

  print(f"bondPrice             = {hex(bondPrice):20s} => {bondPrice}")
  print(f"bondPriceUSD          = {hex(bondPriceUSD):20s} => {bondPriceUSD}")
  print(f"trueBondPrice         = {hex(trueBondPrice):20s} => {trueBondPrice}")
  print(f"debtRatio             = {hex(debtRatio):20s} => {debtRatio}")
  print(f"currentBondDiscount % = {currentBondDiscount * 100 / 10**7}%")
