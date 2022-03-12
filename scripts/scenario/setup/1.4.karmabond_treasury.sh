#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh

source ./scripts/karma/pkg.sh

# Network must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
info "Deploying Treasury..."

# Warn about the Treasury not being implemented yet
warning "Treasury isn't implemented yet, use a fixed EOA address in the meantime"

# Get the Fixed EOA address
karmaTreasury=$(cat ./config/keystores/${network}/dao_treasury.icx | jq .address -r)

# Package information
pkg=$(getKarmaTreasuryPkg)
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Configure the deploy addresses
jq -n \
  --arg scoreAddress $karmaTreasury \
  '{scoreAddress: $scoreAddress}' \
  > ./${deployDir}/deploy.json
