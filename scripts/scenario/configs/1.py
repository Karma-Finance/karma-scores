import json

BNUSD = "cx38b5f44ad2f4486172dfea12e6cde67a23eadaf1"
SICX = "cxcc57144332b23ca8f36d09d862bc202caa76dc30"

config = {
  # Type: String
  # "network" value must be:
  #   - "sejong" for Sejong Network,
  #   - "custom" for the custom Karma Network
  "network": "sejong",

  # Type: String
  # "type" value must be:
  #   - "Balanced" for Balance LP tokens,
  #   - "Base" for IRC2 base implementation
  "implementation": "Balanced",

  "bond": {
    "default": {
      # Type: Address
      # Principal and Payout tokens addresses
      "principalToken": BNUSD,
      "payoutToken": SICX,

      # Type: Address
      # Initial owner of the custom bond
      "initialOwner": "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",

      # Type: Integer
      # Vesting term value (in seconds)
      "vestingTermSeconds": 604800, # 7 * 24h * 3600s = 1 week

      # Array of fees tiers, in ten-thousandths (i.e. 33300 = 3.33%)
      "fees": [
        hex(33300), # = 3.33%
        hex(66600), # = 6.66%
      ],

      # Array of ceilings of principal bonded till next tier
      "tierCeilings": [
        hex(1 * 10**18),
        hex(2 * 10**18)
      ],

      "initialize": {
        "controlVariable": hex(400000),
        "minimumPrice": hex(5403),
        "maxPayout": hex(500),
        "maxDebt": hex(18),
        "initialDebt": hex(1560000000),
      },
    },

    "implementation": {
      # Type: String (hexstring)
      # If the implementation is Balanced, set the Balanced Pool ID
      # Refer to Balanced documentation for this value
      "principalPoolId": hex(3),
    },
  },

  "treasury": {
    "default": {
      # Type: Address
      # Initial owner of the custom treasury
      "initialOwner": "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",
      
      # Type: String (hexstring)
      # Initial funding of the custom treasury
      # This value must be a hexstring
      # If "0x0", do not send anything
      "initialPayoutFunding": hex(1 * 10**18), # 1 sICX
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))