.PHONY: build run clean keys

build:
	mvn package

clean:
	mvn clean

test:
	mvn test

#TO SPECIFY THE NUMBER OF KEYS TO GENERATE, USE N=number_of_keys
keys:
	bash ./src/main/java/pt/tecnico/ulisboa/scripts/generate_keys.sh $(if $(N),$(N),5) $(CURDIR)/src/main/java/pt/tecnico/ulisboa/keys

client:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.client.Client" -Dexec.args="1 src/main/java/pt/tecnico/ulisboa/keys"

server:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="1 src/main/java/pt/tecnico/ulisboa/keys"

consensus:
	bash ./src/main/java/pt/tecnico/ulisboa/scripts/consensus_4servers.sh
