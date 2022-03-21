import json

IUSDC = "cx648a6d9c5f231f6b86c0caa9cc9eff8bd6040999"
ICX   = "cx0000000000000000000000000000000000000000"

config = {
  # Type: String
  # "network" value must be:
  #   - "sejong" for Sejong Network,
  #   - "custom" for the custom Karma Network
  "network": "sejong",

  # Type: String
  # "type" value must be:
  #   - "Balanced" for Balance LP tokens,
  #   - "Base" for IRC2 or ICX base implementation
  "implementation": "Base",

  "bond": {
    "default": {
      # Type: Address
      # Principal and Payout tokens addresses
      "principalToken": IUSDC,
      "payoutToken": ICX,

      # Type: Address
      # Initial owner of the custom bond
      "initialOwner": "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",

      # Type: Integer
      # Vesting term value (in seconds)
      "vestingTermSeconds": 129600, # 1 day

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
        "controlVariable": hex(2000),
        "minimumPrice": hex(0),
        "maxPayout": hex(500),
        "maxDebt": hex(150010000000000000000000),
        "initialDebt": hex(10000000000),
      },
    }
  },

  "treasury": {
    "default": {
      # Type: Address
      # Initial owner of the custom treasury
      "initialOwner": "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",

      # Type: String (hexstring)
      # Initial funding of the custom treasury payout token, sent from the operator address
      # This value must be a hexstring
      # If "0x0", do not send anything
      "initialPayoutFunding": hex(1 * 10**18), # 1 ICX
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))