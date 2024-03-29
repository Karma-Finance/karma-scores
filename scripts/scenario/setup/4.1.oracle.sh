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
policyOwnerWallet="./scripts/config/keystores/${network}/dao.icx"
initialOwner=$(get_wallet_address ${policyOwnerWallet})

if [[ "$network" == "berlin" ]] ; then
  balancedDex=cx4d3b86709c387dec2927158c0377ecabe002f503
  bandOracle=cxbf3b46689e8c7d9c327e03a61098c5a91df2081e
  USDS=cx91a9327ca44e78983e143b1cfb18e8024a1f31d9
  USDB=cx0000000000000000000000000000000000000000
  BNUSD=cx1cd2da25f9942fda5144e139bbda3e5108d3c083
  IUSDC=cx538a925f49427d4f1078aed638c8cb525071fc68
  IUSDT=cx0000000000000000000000000000000000000000
  BALN=cx9eefbe346b17328e2265573f6e166f6bc4a13cc4
  SICX=cxdd89d7a425b8f0b6448a8c80136727c517e64033
  OMM=cx0fa7815de5b2be6e51dc52caa0dc556012ae0f98
  SICX_ICX_POOL_ID=0x1
elif [[ "$network" == "lisbon" ]] ; then
  balancedDex=cx7a90ed2f781876534cf1a04be34e4af026483de4
  bandOracle=cx03018fc77ee8862c25f1656161f8a6dbe53620d3
  USDS=cx313388571690e0f77546cf3885d37da1d40ff207
  USDB=cx0000000000000000000000000000000000000000
  BNUSD=cx87f7f8ceaa054d46ba7343a2ecd21208e12913c6
  IUSDC=cx9634be155fc77754c4b2e1bceddc1c63d973f1f3
  IUSDT=cx0000000000000000000000000000000000000000
  BALN=cxc3c552054ba6823107b56086134c2afc26ab1dfa
  SICX=cx2d013cb55781fb54b81d629aa4b611be2daec564
  OMM=cx60d6cd6f54514eef43f5160dd60fc25c8a00f430
  SICX_ICX_POOL_ID=0x1
elif [[ "$network" == "mainnet" ]] ; then
  balancedDex=cxa0af3165c08318e988cb30993b3048335b94af6c
  bandOracle=cxe647e0af68a4661566f5e9861ad4ac854de808a2
  USDS=cxbb2871f468a3008f80b08fdde5b8b951583acf06
  USDB=cx24fecad15fd6496652299d6b5d76e781e82cb815
  BNUSD=cx88fd7df7ddff82f7cc735c871dc519838cb235bb
  IUSDC=cxae3034235540b924dfcc1b45836c293dcc82bfb7
  IUSDT=cx3a36ea1f6b9aa3d2dd9cb68e8987bcc3aabaaa88
  BALN=cxf61cd5a45dc9f91c15aa65831a30a90d59a09619
  SICX=cx2609b924e33ef00b648a409245c7ea394c467824
  OMM=cx1a29259a59f463a67bb2ef84398b30ca56b5830a
  SICX_ICX_POOL_ID=0x1
else
  error "Unsupported network"
  exit 1
fi

filter=$(cat <<EOF
{
  initialOwner: \$initialOwner,
  balancedDex: \$balancedDex,
  bandOracle: \$bandOracle,
  USDS: \$USDS,
  USDB: \$USDB,
  BNUSD: \$BNUSD,
  IUSDC: \$IUSDC,
  IUSDT: \$IUSDT,
  BALN: \$BALN,
  SICX: \$SICX,
  OMM: \$OMM,
  SICX_ICX_POOL_ID: \$SICX_ICX_POOL_ID
}
EOF
)

jq -n \
  --arg initialOwner $initialOwner \
  --arg balancedDex $balancedDex \
  --arg bandOracle $bandOracle \
  --arg USDS $USDS \
  --arg USDB $USDB \
  --arg BNUSD $BNUSD \
  --arg IUSDC $IUSDC \
  --arg IUSDT $IUSDT \
  --arg BALN $BALN \
  --arg SICX $SICX \
  --arg OMM $OMM \
  --arg SICX_ICX_POOL_ID $SICX_ICX_POOL_ID \
  "${filter}" > ${deployDir}/params.json

./run.py -e ${network} deploy ${pkg}
