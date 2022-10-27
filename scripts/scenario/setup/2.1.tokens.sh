#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/karma/pkg.sh

# principalToken must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <bondId>"
  exit 1
fi

bondId=$1

# Start
info "Setting up tokens..."

# Read the bond config file
bondConfig=$(python ${bondConfigsDir}/${bondId}.py)

# Read the bond config variables
network=$(echo ${bondConfig} | jq -r .network)
principalToken=$(echo ${bondConfig} | jq -r .bond.default.principalToken)
payoutToken=$(echo ${bondConfig} | jq -r .bond.default.payoutToken)

# -- Payout --
# Payout Package information
pkg=$(getPayoutPkg ${bondId})
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Configure the Payout deploy addresses
jq -n \
  --arg scoreAddress $payoutToken \
  '{scoreAddress: $scoreAddress}' \
  > ./${deployDir}/deploy.json

# -- Principal --
# Principal Package information
pkg=$(getPrincipalPkg ${bondId})
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Configure the Principal deploy addresses
jq -n \
  --arg scoreAddress $principalToken \
  '{scoreAddress: $scoreAddress}' \
  > ./${deployDir}/deploy.json
