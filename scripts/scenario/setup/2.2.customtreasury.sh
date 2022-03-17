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
info "Deploying Custom Treasury for Bond ID ${bondId}..."

# Read the bond config file
bondConfig=$(python ${bondConfigsDir}/${bondId}.py)

# Read the bond config variables
network=$(echo ${bondConfig} | jq -r .network)
payoutToken=$(echo ${bondConfig} | jq -r .bond.default.payoutToken)
initialOwner=$(echo ${bondConfig} | jq -r .treasury.default.initialOwner)
implementationType=$(echo ${bondConfig} | jq -r .implementation)

# Package information
pkg=$(getCustomTreasuryPkg ${bondId})
if [ "$implementationType" = "Base" ]; then
  javaPkgName="Karma-CustomTreasury"
else
  javaPkgName="Karma-CustomTreasury${implementationType}"
fi
javaPkg=":Karma-Bond:Contracts:${javaPkgName}"
build="optimized"

# Setup packages
setupJavaDir ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Deploy on ICON network
case $implementationType in
  "Balanced")
    poolIdPrincipalToken=$(echo ${bondConfig} | jq -r .bond.implementation.principalPoolId)
    filter=$(cat <<EOF
    {
      payoutToken: \$payoutToken,
      initialOwner: \$initialOwner,
      poolIdPrincipalToken: \$poolIdPrincipalToken
    }
EOF
    )

    jq -n \
      --arg payoutToken $payoutToken \
      --arg initialOwner $initialOwner \
      --arg poolIdPrincipalToken $poolIdPrincipalToken \
      "${filter}" > ${deployDir}/params.json
    ;;

  "Base")
    filter=$(cat <<EOF
    {
      payoutToken: \$payoutToken, 
      initialOwner: \$initialOwner, 
    }
EOF
    )

    jq -n \
      --arg payoutToken $payoutToken \
      --arg initialOwner $initialOwner \
      "${filter}" > ${deployDir}/params.json
    ;;

  *)
    error "Invalid implementation type"
    exit -1
esac

./run.py -e ${network} deploy ${pkg}

# Send payout tokens to the custom treasury
customTreasuryInitialPayoutFunding=$(echo ${bondConfig} | jq -r .treasury.default.initialPayoutFunding)

if [ "$customTreasuryInitialPayoutFunding" != "0x0" ] ; then
  payoutPkg=$(getPayoutPkg ${bondId})
  payoutCallsDir=$(getCallsDir ${payoutPkg} ${network})
  customTreasury=$(getAddress ${pkg} ${network})
  actionName=customTreasuryInitialDeposit

  _to=$customTreasury
  _value=$customTreasuryInitialPayoutFunding
  _data=$(python -c "print('0x'+b'{\"method\":\"funding\"}'.hex())")

  filter=$(cat <<EOF
  {
    method: "transfer",
    params: {
      _to: \$_to, 
      _value: \$_value, 
      _data: \$_data
    }
  }
EOF
  )

  jq -n \
    --arg _to $_to \
    --arg _value $_value \
    --arg _data $_data \
    "${filter}" > ${payoutCallsDir}/${actionName}.json

  ./run.py -e ${network} invoke ${payoutPkg} ${actionName}
fi
