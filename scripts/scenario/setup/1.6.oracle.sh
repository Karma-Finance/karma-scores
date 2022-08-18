#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh
source ./scripts/util/get_wallet_address.sh

source ./scripts/karma/pkg.sh

# Network must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
info "Deploying Oracle..."

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
ownerWallet="./scripts/config/keystores/${network}/owner.icx"
initialOwner=$(get_wallet_address ${ownerWallet})

if [[ "$network" == "berlin" ]] ; then
  balancedDex=cx4d3b86709c387dec2927158c0377ecabe002f503
  bandOracle=cxbf3b46689e8c7d9c327e03a61098c5a91df2081e
  sIcx=cxdd89d7a425b8f0b6448a8c80136727c517e64033
elif [[ "$network" == "sejong" ]] ; then
  balancedDex=cx648a6d9c5f231f6b86c0caa9cc9eff8bd6040999
  bandOracle=cxed354ecd9855e52a33ba0dab4e4439f8b4cb96ba
  sIcx=cx70806fdfa274fe12ab61f1f98c5a7a1409a0c108
elif [[ "$network" == "mainnet" ]] ; then
  balancedDex=cxa0af3165c08318e988cb30993b3048335b94af6c
  bandOracle=cxe647e0af68a4661566f5e9861ad4ac854de808a2
  sIcx=cx2609b924e33ef00b648a409245c7ea394c467824
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

# Special case for Berlin OMM
if [[ "$network" == "berlin" ]] ; then
  actionName=setOmmPoolName
  ommPoolName="OMM2"
  filter=$(cat <<EOF
  {
    method: "setOmmPoolName",
    params: {
      ommPoolName: \$ommPoolName
    }
  }
EOF
  )

  jq -n \
    --arg ommPoolName $ommPoolName \
    "${filter}" > ${callsDir}/${actionName}.json

  ./run.py -k ${ownerWallet} -e ${network} invoke ${pkg} ${actionName}
fi
