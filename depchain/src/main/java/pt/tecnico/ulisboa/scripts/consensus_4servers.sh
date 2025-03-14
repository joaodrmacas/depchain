
# Generate keys
make keys

# Initialize servers
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="0 src/main/java/pt/tecnico/ulisboa/keys" &
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="1 src/main/java/pt/tecnico/ulisboa/keys" &
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="2 src/main/java/pt/tecnico/ulisboa/keys" &
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="3 src/main/java/pt/tecnico/ulisboa/keys" &
echo "a" | mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.client.Client" -Dexec.args="1 src/main/java/pt/tecnico/ulisboa/keys" &

kill -9 $(lsof -t -i :8080)
kill -9 $(lsof -t -i :9090)
kill -9 $(lsof -t -i :8081)
kill -9 $(lsof -t -i :9091)
kill -9 $(lsof -t -i :8082)
kill -9 $(lsof -t -i :9092)
kill -9 $(lsof -t -i :8083)
kill -9 $(lsof -t -i :9093)
kill -9 $(lsof -t -i :8084)
kill -9 $(lsof -t -i :9094)
