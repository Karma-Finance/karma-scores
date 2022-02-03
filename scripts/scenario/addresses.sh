#!/bin/bash

. ./scripts/util/get_address.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 0
fi

endpoint=$1

echo " -- [Network: ${endpoint}] -- "

echo "Custom Bond : "$(getAddress "bond-custombond" ${endpoint})
echo "Custom Treasury : "$(getAddress "bond-customtreasury" ${endpoint})
echo "Factory : "$(getAddress "bond-factory" ${endpoint})
echo "Factory Storage : "$(getAddress "bond-factorystorage" ${endpoint})
echo "Subsidy Router : "$(getAddress "bond-subsidyrouter" ${endpoint})
echo "PayoutToken : "$(getAddress "bond-payouttoken" ${endpoint})
echo "PrincipalToken : "$(getAddress "bond-principaltoken" ${endpoint})

echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "bond-custombond" ${endpoint})\": \"Custom Bond\","
echo "  \"$(getAddress "bond-customtreasury" ${endpoint})\": \"Custom Treasury\","
echo "  \"$(getAddress "bond-factory" ${endpoint})\": \"Factory\","
echo "  \"$(getAddress "bond-factorystorage" ${endpoint})\": \"Factory Storage\","
echo "  \"$(getAddress "bond-subsidyrouter" ${endpoint})\": \"Subsidy Router\","
echo "  \"$(getAddress "bond-payouttoken" ${endpoint})\": \"PayoutToken\","
echo "  \"$(getAddress "bond-principaltoken" ${endpoint})\": \"PrincipalToken\","
echo "}"