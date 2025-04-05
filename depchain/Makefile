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

server:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.server.Server" -Dexec.args="1 src/main/java/pt/tecnico/ulisboa/keys"

client:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.client.Client" -Dexec.args="-2 src/main/java/pt/tecnico/ulisboa/keys"

quietnode:
	bash ./src/main/java/pt/tecnico/ulisboa/tests/byztest_quietnode.sh

delayednode:
	bash ./src/main/java/pt/tecnico/ulisboa/tests/byztest_delayednode.sh

selectivedrop:
	bash ./src/main/java/pt/tecnico/ulisboa/tests/byztest_selectivedrop.sh

clientreqtamperer:
	bash ./src/main/java/pt/tecnico/ulisboa/tests/byztest_clientreqtamperer.sh

floodingnode:
	bash ./src/main/java/pt/tecnico/ulisboa/tests/byztest_floodingnode.sh

replaynode:
	bash ./src/main/java/pt/tecnico/ulisboa/tests/byztest_replaynode.sh

serverreqtamperer:
	bash ./src/main/java/pt/tecnico/ulisboa/tests/byztest_serverreqtamperer.sh

consensus:
	bash ./src/main/java/pt/tecnico/ulisboa/scripts/consensus_4servers.sh

4servers:
	bash ./src/main/java/pt/tecnico/ulisboa/scripts/4servers.sh

kill:
	bash ./src/main/java/pt/tecnico/ulisboa/scripts/kill.sh

cut_logs:
	bash ./src/main/java/pt/tecnico/ulisboa/scripts/cut_logs.sh