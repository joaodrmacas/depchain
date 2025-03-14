
# Generate keys
make keys

#Initialize servers
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="0 src/main/java/pt/tecnico/ulisboa/keys" &
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="1 src/main/java/pt/tecnico/ulisboa/keys" &
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="2 src/main/java/pt/tecnico/ulisboa/keys" &
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="3 src/main/java/pt/tecnico/ulisboa/keys" &
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.client.Client" -Dexec.args="1 src/main/java/pt/tecnico/ulisboa/keys"