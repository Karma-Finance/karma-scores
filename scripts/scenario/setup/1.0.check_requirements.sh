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
info "Checking requirements..."

# Check if wallets are set
operator=$(get_wallet_address ./scripts/config/keystores/${network}/operator.icx)
karmaTreasury=$(get_wallet_address ./scripts/config/keystores/${network}/dao_treasury.icx)
karmaDAO=$(get_wallet_address ./scripts/config/keystores/${network}/dao.icx)
funder=$(get_wallet_address ./scripts/config/keystores/${network}/funder.icx)
owner=$(get_wallet_address ./scripts/config/keystores/${network}/owner.icx)

success "All requirements are OK"