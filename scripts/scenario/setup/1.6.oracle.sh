#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/karma/pkg.sh

# Network must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
info "Deploying Oracle on ${network}..."

# Package information
pkg=$(getKarmaOraclePkg)
javaPkg=":Karma-Bond:Contracts:Karma-Oracle"
build="optimized"

# Setup packages
setupJavaDir ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})
callsDir=$(getCallsDir ${pkg} ${network})

# Deploy on ICON network
karmaDAO=$(cat ./config/keystores/${network}/dao.icx | jq .address -r)

initialOwner=${karmaDAO}

if [[ "$network" == "berlin" ]] ; then
  balancedDex=cx4d3b86709c387dec2927158c0377ecabe002f503
  bandOracle=cxbf3b46689e8c7d9c327e03a61098c5a91df2081e
  sIcx=cxdd89d7a425b8f0b6448a8c80136727c517e64033
elif [[ "$network" == "sejong" ]] ; then
  balancedDex=cx648a6d9c5f231f6b86c0caa9cc9eff8bd6040999
  bandOracle=cxed354ecd9855e52a33ba0dab4e4439f8b4cb96ba
  sIcx=cx70806fdfa274fe12ab61f1f98c5a7a1409a0c108
else
  error "Unsupported network"
  exit 1
fi

filter=$(cat <<EOF
{
  initialOwner: \$initialOwner,
  balancedDex: \$balancedDex,
  bandOracle: \$bandOracle,
  sIcx: \$sIcx
}
EOF
)

jq -n \
  --arg initialOwner $initialOwner \
  --arg balancedDex $balancedDex \
  --arg bandOracle $bandOracle \
  --arg sIcx $sIcx \
  "${filter}" > ${deployDir}/params.json

./run.py -e ${network} deploy ${pkg}


if [[ "$network" == "berlin" ]] ; then
  actionName=setOmmPoolName
  ommPoolName="OMM2"
  filter=$(cat <<EOF
  {
    ommPoolName: \$ommPoolName
  }
EOF
  )

  jq -n \
    --arg ommPoolName $ommPoolName \
    "${filter}" > ${callsDir}/${actionName}.json

  ./run.py -e ${network} invoke ${pkg} ${actionName}
fi
