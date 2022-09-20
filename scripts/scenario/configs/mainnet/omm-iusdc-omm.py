import json

OMM_IUSDC = "cxa0af3165c08318e988cb30993b3048335b94af6c"
OMM_IUSDC_LP_POOL_ID =  6
OMM = "cx1a29259a59f463a67bb2ef84398b30ca56b5830a"

config = {
    # Type: String
    # "network" value must be:
    #   - "sejong" for Sejong Network,
    #   - "custom" for the custom Karma Network
    "network": "mainnet",

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
            # MUST POINT TO THE DAO WALLET ADDRESS!!!
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
                "controlVariable": hex(40000),
                "minimumPrice": hex(0),
                "maxPayout": hex(9),
                "maxDebt": hex(650_000 * 10**18),
                "initialDebt": hex(25_000 * 10**18),
                "maxDiscount": hex(100),  # in thousands 100 = 10%
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
            "initialOwner": "cx25e76ddfbabe512e882297a7577ec51bc40c4018",

            # Type: String (hexstring)
            # Initial funding of the custom treasury payout token, sent from the operator address
            # This value must be a hexstring
            # If "0x0", do not send anything
            "initialPayoutFunding": hex(0), # 20k OMM
        },
    },
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))