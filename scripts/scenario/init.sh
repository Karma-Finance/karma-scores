#!/bin/bash

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 0
fi

source ./venv/bin/activate
endpoint=$1

. ./scripts/util/get_address.sh

# ------------------------------------------------------------------------
echo "Cleaning..."
./gradlew clean
