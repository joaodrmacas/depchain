.PHONY: build run clean keys

build:
	mvn package

run:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.App"

clean:
	mvn clean

#TO SPECIFY THE NUMBER OF KEYS TO GENERATE, USE N=number_of_keys
keys:
	bash ./src/main/java/pt/tecnico/ulisboa/scripts/generate_keys.sh $(if $(N),$(N),5) $(CURDIR)/src/main/java/pt/tecnico/ulisboa/keys

client:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.client.Client" -Dexec.args="1 8079 src/main/java/pt/tecnico/ulisboa/keys"

scripts:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.client.Client" -Dexec.args="1 8080 src/main/java/pt/tecnico/ulisboa/keys"