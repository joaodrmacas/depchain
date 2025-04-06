#!/bin/bash

# Generate keys
make keys N=4

ROOTPACKAGE=pt.tecnico.ulisboa
ROOTDIR=src/main/java/pt/tecnico/ulisboa

# Ensure the logs directory exists
mkdir -p "${ROOTDIR}/logs"

# Kill processes on specific ports
bash ${ROOTDIR}/scripts/kill.sh

# Initialize servers
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.server.Server" \
    -Dexec.args="0 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/byztest_quietnode_server_00.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.server.Server" \
    -Dexec.args="1 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/byztest_quietnode_server_01.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.server.Server" \
    -Dexec.args="2 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/byztest_quietnode_server_02.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.server.byzantine.QuietServer" \
    -Dexec.args="3 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/byztest_quietnode_server_03.log" 2>&1 &

# Run client in background
CLIENT_LOG="${ROOTDIR}/logs/byztest_quietnode_client_01.log"
echo "ISTContract addToBlacklist 1" | mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.client.Client" \
    -Dexec.args="-1 ${ROOTDIR}/keys" > "$CLIENT_LOG" 2>&1 &
CLIENT_PID=$!

# Wait and terminate client
sleep 10
kill "$CLIENT_PID" 2>/dev/null

# Check result
if grep -q "Contract call executed successfully" "$CLIENT_LOG"; then
    echo -e "\n\033[1;32m=============================="
    echo "✅  Test PASSED"
    echo -e "==============================\033[0m"
    exit 0
else
    echo -e "\n\033[1;31m=============================="
    echo "❌  Test FAILED"
    echo -e "==============================\033[0m"
    exit 1
fi
