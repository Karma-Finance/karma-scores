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
info "Deploying Factory..."

# Get dependencies
karmaTreasury=$(getAddress $(getKarmaTreasuryPkg) ${network})
karmaDAO=$(getAddress $(getKarmaDAOPkg) ${network})
karmaFactoryStorage=$(getAddress "karmabond/factorystorage" ${network})
karmaSubsidyRouter=$(getAddress "karmabond/subsidyrouter" ${network})

# Package information
pkg=$(getFactoryPkg)
javaPkg=":Karma-Bond:Contracts:Karma-Factory"
build="optimized"

# Setup packages
setupJavaDir ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Deploy on ICON network
filter=$(cat <<EOF
{
  karmaTreasury: \$karmaTreasury, 
  karmaFactoryStorage: \$karmaFactoryStorage, 
  karmaSubsidyRouter: \$karmaSubsidyRouter, 
  karmaDAO: \$karmaDAO
}
EOF
)

jq -n \
  --arg karmaTreasury $karmaTreasury \
  --arg karmaFactoryStorage $karmaFactoryStorage \
  --arg karmaSubsidyRouter $karmaSubsidyRouter \
  --arg karmaDAO $karmaDAO \
  "${filter}" > ${deployDir}/params.json

./run.py -e ${network} deploy ${pkg}