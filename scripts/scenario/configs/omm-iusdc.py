import json

OMM_IUSDC = "cx648a6d9c5f231f6b86c0caa9cc9eff8bd6040999"
OMM_IUSDC_LP_POOL_ID = 35
OMM = "cxb94c0da9a8e650cc09b2f6b1bbfaa957ab47ec4c"

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
      "principalToken": OMM_IUSDC,
      "payoutToken": OMM,

      # Type: Address
      # Initial owner of the custom bond
      "initialOwner": "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",

      # Type: Integer
      # Vesting term value (in seconds)
      "vestingTermSeconds": 129600, # 7 * 24h * 3600s = 1 week

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
        "controlVariable": hex(60000),
        "minimumPrice": hex(0),
        "maxPayout": hex(500),
        "maxDebt": hex(10001000000000000000000),
        "initialDebt": hex(6001000000000000000000),
      },
    },

    "implementation": {
      # Type: String (hexstring)
      # If the implementation is Balanced, set the Balanced Pool ID
      # Refer to Balanced documentation for this value
      "principalPoolId": hex(OMM_IUSDC_LP_POOL_ID),
    },
  },

  "treasury": {
    "default": {
      # Type: Address
      # Initial owner of the custom treasury
      "initialOwner": "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",

      # Type: String (hexstring)
      # Initial funding of the custom treasury payout token
      # This value must be a hexstring
      # If "0x0", do not send anything
      "initialPayoutFunding": hex(20000 * 10**18), # 20k OMM
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))