# Generate keys
make keys

ROOTPACKAGE=pt.tecnico.ulisboa
ROOTDIR=src/main/java/pt/tecnico/ulisboa

# Ensure the logs directory exists
mkdir -p "${ROOTDIR}/logs"

# Kill processes on specific ports
for PORT in 8080 9090 8081 9091 8082 9092 8083 9093 8084 9094 10011; do
    PID=$(lsof -t -i :$PORT)
    if [ -n "$PID" ]; then
        kill -9 "$PID"
        echo "Killed process on port $PORT"
    else
        echo "No process found on port $PORT"
    fi
done

# Initialize servers
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.Node" \
    -Dexec.args="0 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/node_00.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.Node" \
    -Dexec.args="1 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/node_01.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.Node" \
    -Dexec.args="2 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/node_02.log" 2>&1 &
mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.Node" \
    -Dexec.args="3 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/node_03.log" 2>&1 &

# Give servers time to start
sleep 5

# Run client
echo "a" | mvn exec:java -Dexec.mainClass="${ROOTPACKAGE}.client.Client" \
    -Dexec.args="1 ${ROOTDIR}/keys" > "${ROOTDIR}/logs/client_01.log" 2>&1 &
