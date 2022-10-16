import json

bnUSD = "cx88fd7df7ddff82f7cc735c871dc519838cb235bb"
VOID = "cx32d5b6c3eff488b058feff88acec824abe7ed8ca"

config = {
  # Type: String
  # "network" value must be:
  #   - "sejong" for Sejong Network,
  #   - "berlin" for Berlin Network,
  #   - "mainnet" for Main Network
  "network": "mainnet",

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
      "initialOwner": "hx42c7aff8bc45b91dacd8713892aa8ee5687170e2",

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
        "controlVariable": hex(1000),
        "minimumPrice": hex(0),
        "maxPayout": hex(1460),
        "maxDebt": hex(100_000 * 10**18),
        "initialDebt": hex(695 * 10**18),
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
      "initialPayoutFunding": hex(0), # Initial owner (Inanis Invicuts will send 31119 VOID to the treasury)
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))