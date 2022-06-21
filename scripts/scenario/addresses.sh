#!/bin/bash

. ./scripts/util/get_address.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [network]"
  exit 1
fi

network=$1

echo " -- [Network: ${network}] -- "

echo "Factory : "$(getAddress "karmabond/factory" ${network})
echo "Factory Storage : "$(getAddress "karmabond/factorystorage" ${network})
echo "Subsidy Router : "$(getAddress "karmabond/subsidyrouter" ${network})
echo "DAO : "$(getAddress "karmabond/dao" ${network})
echo "Treasury : "$(getAddress "karmabond/treasury" ${network})

echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "karmabond/factory" ${network})\": \"Factory\","
echo "  \"$(getAddress "karmabond/factorystorage" ${network})\": \"Factory Storage\","
echo "  \"$(getAddress "karmabond/subsidyrouter" ${network})\": \"Subsidy Router\","
echo "  \"$(getAddress "karmabond/dao" ${network})\": \"DAO\","
echo "  \"$(getAddress "karmabond/treasury" ${network})\": \"Treasury\","
echo "}"