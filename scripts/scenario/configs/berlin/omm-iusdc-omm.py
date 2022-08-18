import json

OMM_IUSDC = "cx4d3b86709c387dec2927158c0377ecabe002f503"
OMM_IUSDC_LP_POOL_ID = 13
OMM = "cx0fa7815de5b2be6e51dc52caa0dc556012ae0f98"

config = {
  # Type: String
  # "network" value must be:
  #   - "sejong" for Sejong Network,
  #   - "custom" for the custom Karma Network
  "network": "berlin",

  # Type: String
  # "type" value must be:
  #   - "Balanced" for Balance LP tokens,
  #   - "Base" for IRC2 or ICX base implementation
  "implementation": "Balanced",

  "bond": {
    "default": {
      # Type: Address
      # Principal and Payout tokens addresses
      "principalToken": OMM_IUSDC,
      "payoutToken": OMM,

      # Type: Address
      # Initial owner of the custom bond
      "initialOwner": "hxb47252510c20acfcc62f9adee212135b06711efc",

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
        "controlVariable": hex(907777),
        "minimumPrice": hex(0),
        "maxPayout": hex(1),
        "maxDebt": hex(70_000 * 10**18),
        "initialDebt": hex(11500 * 10**18),
        "maxDiscount": hex(50),  # in thousands 100 = 10%
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
      "initialOwner": "hxb47252510c20acfcc62f9adee212135b06711efc",

      # Type: String (hexstring)
      # Initial funding of the custom treasury payout token, sent from the operator address
      # This value must be a hexstring
      # If "0x0", do not send anything
      "initialPayoutFunding": hex(30_000 * 10**18), # 40k OMM
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))