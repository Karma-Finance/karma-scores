#!/bin/bash

source ./venv/bin/activate

. ./scripts/util/get_address.sh

echo "Cleaning..."
./gradlew clean

# ---- Constants - You may edit these variables ----
# The target network - "sejong" for the Sejong Network, or "custom" for the Custom Karma network
endpoint=sejong
bnusd="cx38b5f44ad2f4486172dfea12e6cde67a23eadaf1"
sicx="cxcc57144332b23ca8f36d09d862bc202caa76dc30"

# Define which one is the principal, and the payout tokens
principalToken=$bnusd
payoutToken=$sicx

# Amount of payout tokens sent to the treasury after deployment
# Please note that these tokens must be present on the operator address beforehand
payoutSentTreasury=$(python -c "print(10 * 10**18)") # 10 sICX

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
  '{scoreAddress: $scoreAddress}' > ./config/deploy/bond-principaltoken/${endpoint}/deploy.json

# payout
echo '{}' | jq \
  --arg scoreAddress $payoutToken \
  '{scoreAddress: $scoreAddress}' > ./config/deploy/bond-payouttoken/${endpoint}/deploy.json

payoutToken=$(getAddress "bond-payouttoken" ${endpoint})
principalToken=$(getAddress "bond-principaltoken" ${endpoint})

# Deploy custom treasury
echo '{}' | jq \
  --arg payoutToken $payoutToken \
  --arg initialOwner $operator \
  '{payoutToken: $payoutToken, initialOwner: $initialOwner}' > ./config/deploy/bond-customtreasury/${endpoint}/params.json

./run.py -e ${endpoint} deploy bond-customtreasury
customTreasury=$(getAddress "bond-customtreasury" ${endpoint})

# Deploy Factory Storage
echo '{}' | jq \
  '{}' > ./config/deploy/bond-factorystorage/${endpoint}/params.json

./run.py -e ${endpoint} deploy bond-factorystorage
karmaFactoryStorage=$(getAddress "bond-factorystorage" ${endpoint})

# Deploy Subsidy Router
echo '{}' | jq \
  '{}' > ./config/deploy/bond-subsidyrouter/${endpoint}/params.json

./run.py -e ${endpoint} deploy bond-subsidyrouter
karmaSubsidyRouter=$(getAddress "bond-subsidyrouter" ${endpoint})

# Deploy Factory
echo '{}' | jq \
  --arg karmaTreasury $karmaTreasury \
  --arg karmaFactoryStorage $karmaFactoryStorage \
  --arg karmaSubsidyRouter $karmaSubsidyRouter \
  --arg karmaDAO $karmaDAO \
  '{karmaTreasury: $karmaTreasury, karmaFactoryStorage: $karmaFactoryStorage, karmaSubsidyRouter: $karmaSubsidyRouter, karmaDAO: $karmaDAO}' > ./config/deploy/bond-factory/${endpoint}/params.json

./run.py -e ${endpoint} deploy bond-factory

# Deploy Custom Bond
echo '{}' | jq \
  --arg customTreasury $customTreasury \
  --arg payoutToken $payoutToken \
  --arg principalToken $principalToken \
  --arg karmaTreasury $karmaTreasury \
  --arg subsidyRouter $karmaSubsidyRouter \
  --arg initialOwner $operator \
  --arg karmaDAO $karmaDAO \
  '{fees: ["0x8214", "0x10428"], tierCeilings: ["0xde0b6b3a7640000", "0x1bc16d674ec80000"], karmaDAO: $karmaDAO, customTreasury: $customTreasury, payoutToken: $payoutToken, principalToken: $principalToken, karmaTreasury: $karmaTreasury, subsidyRouter: $subsidyRouter, initialOwner: $initialOwner}' > ./config/deploy/bond-custombond/${endpoint}/params.json

./run.py -e ${endpoint} deploy bond-custombond

# send payoutToken to the custom treasury
_value=$(python -c "print(hex(${payoutSentTreasury}))")

echo '{}' | jq \
  --arg _to $customTreasury \
  --arg _value $_value \
  '{method: "transfer", params: {_to: $_to, _value: $_value, _data: "0x7b226d6574686f64223a2266756e64696e67227d"}}' > ./config/calls/bond-payouttoken/${endpoint}/customtreasury_initial_deposit.json

./run.py -e ${endpoint} invoke bond-payouttoken customtreasury_initial_deposit

# Call setBondTerms for setting the vesting term
parameter="0x0" # VESTING
vestingTerm=$(python -c "print(hex(${vestingTermSeconds}//2))")
echo '{}' | jq \
  --arg parameter $parameter \
  --arg input $vestingTerm \
  '{method: "setBondTerms", params: {parameter: $parameter, input: $input}}' > ./config/calls/bond-custombond/${endpoint}/setBondTerms.json

./run.py -e ${endpoint} invoke bond-custombond setBondTerms

# Initialize bond
controlVariable=$(python -c "print(hex(${controlVariable}))")
minimumPrice=$(python -c "print(hex(${minimumPrice}))")
maxPayout=$(python -c "print(hex(${maxPayout}))")
maxDebt=$(python -c "print(hex(${maxDebt}))")
initialDebt=$(python -c "print(hex(${initialDebt}))")
echo '{}' | jq \
  --arg controlVariable $controlVariable \
  --arg vestingTerm $vestingTerm \
  --arg minimumPrice $minimumPrice \
  --arg maxPayout $maxPayout \
  --arg maxDebt $maxDebt \
  --arg initialDebt $initialDebt \
  '{method: "initializeBond", params: {controlVariable: $controlVariable, vestingTerm: $vestingTerm, minimumPrice: $minimumPrice, maxPayout: $maxPayout, maxDebt: $maxDebt, initialDebt: $initialDebt}}' > ./config/calls/bond-custombond/${endpoint}/initializeBond.json

./run.py -e ${endpoint} invoke bond-custombond initializeBond
