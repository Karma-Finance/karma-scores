import json

USDS= "cxc0dbb2eb24719f8355a7ec3c1aaa93826669ab8e"
BNUSD = "cx5838cb516d6156a060f90e9a3de92381331ff024"

config = {
  # Type: String
  # "network" value must be:
  #   - "sejong" for Sejong Network,
  #   - "lisbon" for Lisbon Network,
  #   - "mainnet" for Main Network
  "network": "sejong",

  # Type: String
  # "type" value must be:
  #   - "Balanced" for Balance LP tokens,
  #   - "Base" for IRC2 base implementation
  "implementation": "Base",

  "bond": {
    "default": {
      # Type: Address
      # Principal and Payout tokens addresses
      "principalToken": USDS,
      "payoutToken": BNUSD,

      # Type: Address
      # Initial owner of the custom bond
      "initialOwner": "hxb47252510c20acfcc62f9adee212135b06711efc",

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
        "controlVariable": hex(6500),
        "minimumPrice": hex(0),
        "maxPayout": hex(1000),
        "maxDebt": hex(7000 * 10**18),
        "initialDebt": hex(5000 * 10**18),
        "maxDiscount": hex(10**18),
      },
    },

    "implementation": {
      # Type: String (hexstring)
      # If the implementation is Balanced, set the Balanced Pool ID
      # Refer to Balanced documentation for this value
      "principalPoolId": hex(0),
    },
  },

  "treasury": {
    "default": {
      # Type: Address
      # Initial owner of the custom treasury
      "initialOwner": "hxb47252510c20acfcc62f9adee212135b06711efc",

      # Type: String (hexstring)
      # Initial funding of the custom treasury payout token
      # This value must be a hexstring
      # If "0x0", do not send anything
      "initialPayoutFunding": hex(5000 * 10**18), # 5k bnUSD
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))