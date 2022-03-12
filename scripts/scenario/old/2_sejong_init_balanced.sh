#!/bin/bash

source ./venv/bin/activate

. ./scripts/util/get_address.sh

echo "Cleaning..."
./gradlew clean

# ---- Constants - You may edit these variables ----
# The target network - "sejong" for the Sejong Network, or "custom" for the Custom Karma network
endpoint=sejong
lp="cx648a6d9c5f231f6b86c0caa9cc9eff8bd6040999"
baln="cx303470dbc10e5b4ab8831a61dbe00f75db10c38b"

# Define which one is the principal, and the payout tokens
principalToken=$lp
payoutToken=$baln

# Balanced Pool ID - refer to Balanced documentation for this value
principalPoolId="0x3" # BALN/bnUSD

# Amount of payout tokens sent to the treasury after deployment
# Please note that these tokens must be present on the operator address beforehand
payoutSentTreasury=$(python -c "print(10 * 10**18)") # 10 BALN

# Vesting term value (in seconds)
vestingTermSeconds=604800 # 7 * 24h * 3600s = 1 week

# Bond initial values
controlVariable=400000
minimumPrice=5403
maxPayout=500
maxDebt=$(python -c "print(5000 * 10**18)") # 5000 * 10**18
initialDebt=1560000000

# ---[ !! Do not touch below this line !! ] ---------------------------------------------------------------------
operator=$(cat ./config/keystores/${endpoint}/operator.icx | jq .address -r)
karmaDAO=$(cat ./config/keystores/${endpoint}/dao.icx | jq .address -r)
karmaTreasury=$(cat ./config/keystores/${endpoint}/dao_treasury.icx | jq .address -r)

# principal
echo '{}' | jq \
  --arg scoreAddress $principalToken \
  '{scoreAddress: $scoreAddress}' > ./config/deploy/bond-2-balanced-principaltoken/${endpoint}/deploy.json

# payout
echo '{}' | jq \
  --arg scoreAddress $payoutToken \
  '{scoreAddress: $scoreAddress}' > ./config/deploy/bond-2-balanced-payouttoken/${endpoint}/deploy.json

payoutToken=$(getAddress "bond-2-balanced-payouttoken" ${endpoint})
principalToken=$(getAddress "bond-2-balanced-principaltoken" ${endpoint})

# Deploy custom treasury
echo '{}' | jq \
  --arg payoutToken $payoutToken \
  --arg initialOwner $operator \
  '{payoutToken: $payoutToken, initialOwner: $initialOwner}' > ./config/deploy/bond-2-balanced-customtreasury/${endpoint}/params.json

./run.py -e ${endpoint} deploy bond-2-balanced-customtreasury
customTreasury=$(getAddress "bond-2-balanced-customtreasury" ${endpoint})

karmaSubsidyRouter=$(getAddress "bond-subsidyrouter" ${endpoint})

# Deploy Custom Bond
echo '{}' | jq \
  --arg customTreasury $customTreasury \
  --arg payoutToken $payoutToken \
  --arg principalToken $principalToken \
  --arg principalPoolId $principalPoolId \
  --arg karmaTreasury $karmaTreasury \
  --arg subsidyRouter $karmaSubsidyRouter \
  --arg initialOwner $operator \
  --arg karmaDAO $karmaDAO \
  '{fees: ["0x8214", "0x10428"], tierCeilings: ["0xde0b6b3a7640000", "0x1bc16d674ec80000"], karmaDAO: $karmaDAO, customTreasury: $customTreasury, payoutToken: $payoutToken, principalToken: $principalToken, principalPoolId: $principalPoolId, karmaTreasury: $karmaTreasury, subsidyRouter: $subsidyRouter, initialOwner: $initialOwner}' > ./config/deploy/bond-2-balanced-custombond/${endpoint}/params.json

./run.py -e ${endpoint} deploy bond-2-balanced-custombond

# send payoutToken to the custom treasury
_value=$(python -c "print(hex(${payoutSentTreasury}))")

echo '{}' | jq \
  --arg _to $customTreasury \
  '{method: "transfer", params: {_to: $_to, _value: $_value, _data: "0x7b226d6574686f64223a2266756e64696e67227d"}}' > ./config/calls/bond-2-balanced-payouttoken/${endpoint}/customtreasury_initial_deposit.json

./run.py -e ${endpoint} invoke bond-2-balanced-payouttoken customtreasury_initial_deposit

# Call setBondTerms for 1 week vesting term
parameter="0x0" # VESTING
vestingTerm=$(python -c "print(hex(${vestingTermSeconds}//2))")
echo '{}' | jq \
  --arg parameter $parameter \
  --arg input $vestingTerm \
  '{method: "setBondTerms", params: {parameter: $parameter, input: $input}}' > ./config/calls/bond-2-balanced-custombond/${endpoint}/setBondTerms.json

./run.py -e ${endpoint} invoke bond-2-balanced-custombond setBondTerms

# Initialize bond
controlVariable="0x61a80" # 400000
minimumPrice="0x151b" # 5403
maxPayout="0x1f4" # 500
maxDebt="0x10f0cf064dd59200000" # 5000 * 10**18
initialDebt="0x5cfbb600" # 1560000000
echo '{}' | jq \
  --arg controlVariable $controlVariable \
  --arg vestingTerm $vestingTerm \
  --arg minimumPrice $minimumPrice \
  --arg maxPayout $maxPayout \
  --arg maxDebt $maxDebt \
  --arg initialDebt $initialDebt \
  '{method: "initializeBond", params: {controlVariable: $controlVariable, vestingTerm: $vestingTerm, minimumPrice: $minimumPrice, maxPayout: $maxPayout, maxDebt: $maxDebt, initialDebt: $initialDebt}}' > ./config/calls/bond-2-balanced-custombond/${endpoint}/initializeBond.json

./run.py -e ${endpoint} invoke bond-2-balanced-custombond initializeBond
