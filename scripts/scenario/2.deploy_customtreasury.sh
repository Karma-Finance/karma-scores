#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

# Bond ID must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <bond ID>"
  exit 1
fi

bondId=$1

# Start
info "Cleaning..."
./gradlew clean > /dev/null

# --- Setting up tokens ---
${setupScriptsDir}/2.1.tokens.sh ${bondId}

# --- Deploy Custom Treasury ---
${setupScriptsDir}/2.2.customtreasury.sh ${bondId}
