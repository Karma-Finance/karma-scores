import json

PRINCIPAL_TETH = "cx99c79fd6dee53335f686c7f2cb513745622634f2"
PRINCIPAL_TETH_DECIMALS = 18
PAYOUT_BALN = "cxc3c552054ba6823107b56086134c2afc26ab1dfa"
PAYOUT_BALN_DECIMALS = 18

config = {
  # Type: String
  # "network" value must be:
  #   - "sejong" for Sejong Network,
  #   - "berlin" for Berlin Network,
  #   - "mainnet" for Main Network
  "network": "lisbon",

  # Type: String
  # "type" value must be:
  #   - "Balanced" for Balance LP tokens,
  #   - "Base" for IRC2 or ICX base implementation
  "implementation": "Base",

  "bond": {
    "default": {
      # Type: Address
      # Principal and Payout tokens addresses
      "principalToken": PRINCIPAL_TETH,
      "payoutToken": PAYOUT_BALN,

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

      # Array of ceilings of principal bonded till next tierfafafafafafafaasdsad
      "tierCeilings": [
        hex(1 * 10**18),
        hex(2 * 10**18)
      ],

      "initialize": {
        "controlVariable": hex(117_777),
        "minimumPrice": hex(0),
        "maxPayout": hex(15),
        "maxDebt": hex(38_000 * 10**PAYOUT_BALN_DECIMALS),
        "initialDebt": hex(int(round(5 * 10**PAYOUT_BALN_DECIMALS, PAYOUT_BALN_DECIMALS))),
        "maxDiscount": hex(100),  # in thousands, 100 = 10%
      },
    }
  },

  "treasury": {
    "default": {
      # Type: Address
      # Initial owner of the custom treasury
      "initialOwner": "cxdb3d3e2717d4896b336874015a4b23871e62fb6b",

      # Type: String (hexstring)
      # Initial funding of the custom treasury payout token, sent from the operator address
      # This value must be a hexstring
      # If "0x0", do not send anything
      "initialPayoutFunding": hex(0), # Baln TOKEN
    },
  },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))