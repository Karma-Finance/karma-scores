#!/bin/bash

. ./scripts/util/get_address.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [network]"
  exit 1
fi

network=$1

echo " -- [Network: ${network}] -- "

echo "Custom Bond 1 bnUSD -> sICX : "$(getAddress "bond-custombond" ${network})
echo "Custom Treasury 1 bnUSD -> sICX : "$(getAddress "bond-customtreasury" ${network})
echo "Custom Bond 2 LP(BALN/bnUSD) -> BALN : "$(getAddress "bond-2-balanced-custombond" ${network})
echo "Custom Treasury 2 LP(BALN/bnUSD) -> BALN : "$(getAddress "bond-2-balanced-customtreasury" ${network})
echo "Factory : "$(getAddress "bond-factory" ${network})
echo "Factory Storage : "$(getAddress "bond-factorystorage" ${network})
echo "Subsidy Router : "$(getAddress "bond-subsidyrouter" ${network})
echo "Bond 1 PayoutToken : "$(getAddress "bond-payouttoken" ${network})
echo "Bond 1 PrincipalToken : "$(getAddress "bond-principaltoken" ${network})
echo "Bond 2 PayoutToken : "$(getAddress "bond-2-balanced-payouttoken" ${network})
echo "Bond 2 PrincipalToken : "$(getAddress "bond-2-balanced-principaltoken" ${network})

echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "bond-custombond" ${network})\": \"Custom Bond 1 bnUSD -> sICX\","
echo "  \"$(getAddress "bond-customtreasury" ${network})\": \"Custom Treasury 1 bnUSD -> sICX\","
echo "  \"$(getAddress "bond-2-balanced-custombond" ${network})\": \"Custom Bond 2 LP(BALN/bnUSD) -> BALN\","
echo "  \"$(getAddress "bond-2-balanced-customtreasury" ${network})\": \"Custom Treasury 2 LP(BALN/bnUSD) -> BALN\","
echo "  \"$(getAddress "bond-factory" ${network})\": \"Factory\","
echo "  \"$(getAddress "bond-factorystorage" ${network})\": \"Factory Storage\","
echo "  \"$(getAddress "bond-subsidyrouter" ${network})\": \"Subsidy Router\","
echo "  \"$(getAddress "bond-payouttoken" ${network})\": \"Bond 1 PayoutToken\","
echo "  \"$(getAddress "bond-principaltoken" ${network})\": \"Bond 1 PrincipalToken\","
echo "  \"$(getAddress "bond-2-balanced-payouttoken" ${network})\": \"Bond 2 PayoutToken\","
echo "  \"$(getAddress "bond-2-balanced-principaltoken" ${network})\": \"Bond 2 PrincipalToken\","
echo "}"