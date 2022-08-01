#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/karma/pkg.sh

# Bond ID must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <bond ID>"
  exit 1
fi

bondId=$1

# Start
info "Deploying Custom Bond for Bond ID ${bondId}..."

# Read the bond config file
bondConfig=$(python ${bondConfigsDir}/${bondId}.py)

# Read the bond config variables
network=$(echo ${bondConfig} | jq -r .network)
initialOwner=$(echo ${bondConfig} | jq -r .bond.default.initialOwner)
principalToken=$(echo ${bondConfig} | jq -r .bond.default.principalToken)
payoutToken=$(echo ${bondConfig} | jq -r .bond.default.payoutToken)
vestingTermSeconds=$(echo ${bondConfig} | jq -r .bond.default.vestingTermSeconds)
fees=$(echo ${bondConfig} | jq -r .bond.default.fees)
tierCeilings=$(echo ${bondConfig} | jq -r .bond.default.tierCeilings)
implementationType=$(echo ${bondConfig} | jq -r .implementation)

# Get dependencies
customTreasuryPkg=$(getCustomTreasuryPkg ${bondId})
customTreasury=$(getAddress ${customTreasuryPkg} ${network})
karmaTreasuryPkg=$(getKarmaTreasuryPkg)
karmaTreasury=$(getAddress ${karmaTreasuryPkg} ${network})
karmaOraclePkg=$(getKarmaOraclePkg)
karmaOracle=$(getAddress ${karmaOraclePkg} ${network})
subsidyRouterPkg=$(getSubsidyRouterPkg)
subsidyRouter=$(getAddress ${subsidyRouterPkg} ${network})
karmaDAOPkg=$(getKarmaDAOPkg)
karmaDAO=$(getAddress ${karmaDAOPkg} ${network})

# Package information
pkg=$(getCustomBondPkg ${bondId})
if [ "$implementationType" = "Base" ]; then
  javaPkgName="Karma-CustomBond"
else
  javaPkgName="Karma-CustomBond${implementationType}"
fi
javaPkg=":Karma-Bond:Contracts:${javaPkgName}"
build="optimized"

# Setup packages
setupJavaDir ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})
bondCallsDir=$(getCallsDir ${pkg} ${network})

# Deploy on ICON network
case $implementationType in
  "Balanced")
    principalPoolId=$(echo ${bondConfig} | jq -r .bond.implementation.principalPoolId)
    filter=$(cat <<EOF
    {
      fees: \$fees, 
      tierCeilings: \$tierCeilings,
      customTreasury: \$customTreasury, 
      payoutToken: \$payoutToken, 
      principalToken: \$principalToken, 
      principalPoolId: \$principalPoolId,
      karmaTreasury: \$karmaTreasury, 
      karmaOracle: \$karmaOracle, 
      subsidyRouter: \$subsidyRouter, 
      initialOwner: \$initialOwner, 
      karmaDAO: \$karmaDAO
    }
EOF
    )

    jq -n \
      --argjson fees "$fees" \
      --argjson tierCeilings "$tierCeilings" \
      --arg customTreasury $customTreasury \
      --arg payoutToken $payoutToken \
      --arg principalToken $principalToken \
      --arg principalPoolId $principalPoolId \
      --arg karmaTreasury $karmaTreasury \
      --arg karmaOracle $karmaOracle \
      --arg subsidyRouter $subsidyRouter \
      --arg initialOwner $initialOwner \
      --arg karmaDAO $karmaDAO \
      "${filter}" > ${deployDir}/params.json
    ;;

  "Base")
    filter=$(cat <<EOF
    {
      fees: \$fees, 
      tierCeilings: \$tierCeilings, 
      customTreasury: \$customTreasury, 
      payoutToken: \$payoutToken, 
      principalToken: \$principalToken, 
      karmaTreasury: \$karmaTreasury, 
      karmaOracle: \$karmaOracle, 
      subsidyRouter: \$subsidyRouter, 
      initialOwner: \$initialOwner, 
      karmaDAO: \$karmaDAO
    }
EOF
    )
    jq -n \
      --argjson fees "$fees" \
      --argjson tierCeilings "$tierCeilings" \
      --arg customTreasury $customTreasury \
      --arg payoutToken $payoutToken \
      --arg principalToken $principalToken \
      --arg karmaTreasury $karmaTreasury \
      --arg karmaOracle $karmaOracle \
      --arg subsidyRouter $subsidyRouter \
      --arg initialOwner $initialOwner \
      --arg karmaDAO $karmaDAO \
      "${filter}" > ${deployDir}/params.json
    ;;
  
  *)
    error "Invalid implementation type"
    exit -1
esac

./run.py -e ${network} deploy ${pkg}

# Call setBondTerms for the vesting term
parameter="0x0" # VESTING
vestingTerm=$(python -c "print(hex(${vestingTermSeconds}//2))")
actionName="setBondTerms"

filter=$(cat <<EOF
{
  method: "setBondTerms",
  params: {
    parameter: \$parameter, 
    input: \$input
  }
}
EOF
)

jq -n \
  --arg parameter $parameter \
  --arg input $vestingTerm \
  "${filter}" > ${bondCallsDir}/${actionName}.json

./run.py -e ${network} invoke ${pkg} ${actionName}

# Initialize the bond
controlVariable=$(echo ${bondConfig} | jq -r .bond.default.initialize.controlVariable)
minimumPrice=$(echo ${bondConfig} | jq -r .bond.default.initialize.minimumPrice)
maxPayout=$(echo ${bondConfig} | jq -r .bond.default.initialize.maxPayout)
maxDebt=$(echo ${bondConfig} | jq -r .bond.default.initialize.maxDebt)
initialDebt=$(echo ${bondConfig} | jq -r .bond.default.initialize.initialDebt)
maxDiscount=$(echo ${bondConfig} | jq -r .bond.default.initialize.maxDiscount)
actionName="initializeBond"

filter=$(cat <<EOF
{
  method: "initializeBond",
  params: {
    controlVariable: \$controlVariable, 
    vestingTerm: \$vestingTerm, 
    minimumPrice: \$minimumPrice, 
    maxPayout: \$maxPayout, 
    maxDebt: \$maxDebt, 
    initialDebt: \$initialDebt,
    maxDiscount: \$maxDiscount,
  }
}
EOF
)

jq -n \
  --arg controlVariable $controlVariable \
  --arg vestingTerm $vestingTerm \
  --arg minimumPrice $minimumPrice \
  --arg maxPayout $maxPayout \
  --arg maxDebt $maxDebt \
  --arg initialDebt $initialDebt \
  --arg maxDiscount $maxDiscount \
  "${filter}" > ${bondCallsDir}/${actionName}.json

./run.py -e ${network} invoke ${pkg} ${actionName}

# Call toggleBondContract on the custom treasury for the current bond address
bondContract=$(getAddress ${pkg} ${network})
treasuryCallsDir=$(getCallsDir ${customTreasuryPkg} ${network})
actionName="toggleBondContract"

filter=$(cat <<EOF
{
  method: "toggleBondContract",
  params: {
    bondContract: \$bondContract
  }
}
EOF
)

jq -n \
  --arg bondContract $bondContract \
  "${filter}" > ${treasuryCallsDir}/${actionName}.json

./run.py -e ${network} invoke ${customTreasuryPkg} ${actionName}
