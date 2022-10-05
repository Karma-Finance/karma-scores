import json

bnUSD = "cx1cd2da25f9942fda5144e139bbda3e5108d3c083"
VOID = "cxc2faf30ef79327f8a177f4b236ee3a1db8de81ec"

config = {
  # Type: String
  # "network" value must be:
  #   - "sejong" for Sejong Network,
  #   - "berlin" for Berlin Network,
  #   - "mainnet" for Main Network
  "network": "berlin",

  # Type: String
  # "type" value must be:
  #   - "Balanced" for Balance LP tokens,
  #   - "Base" for IRC2 or ICX base implementation
  "implementation": "Base",

  "bond": {
    "default": {
      # Type: Address
      # Principal and Payout tokens addresses
      "principalToken": bnUSD,
      "payoutToken": VOID,

      # Type: Address
      # Initial owner of the custom bond
      # Should point to the dao.icx address
      "initialOwner": "hx73920a92f3dc273fbf1a17aacc74b7bc31c6a932",

      # Type: Integer
      # Vesting term value (in seconds)
      "vestingTermSeconds": 604800, # 7 * 24h * 3600s = 1 week

      # Array of fees tiers, in ten-thousandths (i.e. 33300 = 3.33%)
      "fees": [
        hex(33300), # = 3.33%
        hex(33300), # = 3.33%
      ],

      # Array of ceilings of principal bonded till next tier
      "tierCeilings": [
        hex(1 * 10**18),
        hex(2 * 10**18)
      ],

      "initialize": {
        "controlVariable": hex(180),
        "minimumPrice": hex(0),
        "maxPayout": hex(100),
        "maxDebt": hex(100_000 * 10**18),
        "initialDebt": hex(8_000 * 10**18),
        "maxDiscount": hex(100),  # in thousands, 100 = 10%
      },
    }
  },

  "treasury": {
    "default": {
      # Type: Address
      # Initial owner of the custom treasury
      "initialOwner": "hxc5e0b88cb9092bbd8b004a517996139334752f62",

      # Type: String (hexstring)
      # Initial funding of the custom treasury payout token, sent from the operator address
      # This value must be a hexstring
      # If "0x0", do not send anything
      "initialPayoutFunding": hex(2000 * 10**18), # VOID TOKEN
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))