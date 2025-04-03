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
    -Dexec.args="0 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/server_00.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.server.Server" \
    -Dexec.args="1 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/server_01.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.server.Server" \
    -Dexec.args="2 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/server_02.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.server.Server" \
    -Dexec.args="3 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/server_03.log" 2>&1 &