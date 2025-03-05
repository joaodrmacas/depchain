#!/bin/bash

N=10  # Change this to the number of times you want to run it

cd src/main/resources/JavaCrypto/

rm src/main/resources/JavaCrypto/keys/*.key

javac -d out src/pt/ulisboa/tecnico/meic/sirs/*.java

for i in $(seq -f "%02g" 0 $((N-1))); do
    java -cp out pt.ulisboa.tecnico.meic.sirs.RSAKeyGenerator w "../keys/pub$i.key" "../keys/priv$i.key"
done