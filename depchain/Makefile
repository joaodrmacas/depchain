.PHONY: build run clean

build:
	mvn package

run:
	mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.App"

clean:
	mvn clean

keys:
	bash ./src/main/resources/generate_keys.sh