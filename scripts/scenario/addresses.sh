#!/bin/bash

. ./scripts/util/get_address.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 0
fi

endpoint=$1

echo " -- [Network: ${endpoint}] -- "

echo "Custom Bond : "$(getAddress "custombond" ${endpoint})
echo "Custom Treasury : "$(getAddress "customtreasury" ${endpoint})
echo "Factory : "$(getAddress "factory" ${endpoint})
