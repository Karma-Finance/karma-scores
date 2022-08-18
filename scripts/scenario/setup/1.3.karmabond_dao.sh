#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/get_wallet_address.sh

source ./scripts/karma/pkg.sh

# Network must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
info "Deploying DAO..."

# Warn about the DAO not being implemented yet
warning "DAO isn't implemented yet, use a fixed EOA address in the meantime"
karmaDAO=$(get_wallet_address ./scripts/config/keystores/${network}/dao.icx)

# Package information
pkg=$(getKarmaDAOPkg)
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Configure the deploy addresses
jq -n \
  --arg scoreAddress $karmaDAO \
  '{scoreAddress: $scoreAddress}' \
  > ./${deployDir}/deploy.json
