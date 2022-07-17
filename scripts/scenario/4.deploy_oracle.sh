#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

# Network must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
info "Cleaning..."
./gradlew clean > /dev/null

# --- Deploy Custom Bond ---
${setupScriptsDir}/4.1.oracle.sh ${network}

success "Karma Oracle contract has been successfully deployed!"