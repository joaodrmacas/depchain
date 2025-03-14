# DepChain: A Dependable Permissioned Blockchain System

## Overview
DepChain is a permissioned blockchain designed with high dependability guarantees, implementing the Byzantine Read/Write Epoch Consensus algorithm. The system provides strong safety and liveness properties, tolerating up to f Byzantine failures in a system with 3f+1 total nodes.

## Features
- **Static membership model** with pre-designated leader selection
- **Authenticated Perfect Links** for reliable communication
- **Byzantine Fault Tolerant** consensus using Read/Write Epoch algorithm
- **Client-server architecture** for submitting and processing blockchain transactions
- **Cryptographic security** with digital signatures for message authenticity
- **Epoch change mechanism** to ensure progress when consensus stalls

## System Components
- **Client Library**: Submits requests to append data to the blockchain
- **Blockchain Nodes**: Process and validate transactions via consensus
- **Consensus Layer**: Implements BFT Read/Write Epoch algorithm
- **Network Layer**: Provides authenticated communication channels

## Prerequisites

- Java JDK 11 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, run:
```
make build
```

This will compile the source code and package it into a JAR file.

## Key Generation

You need to generate key pairs for the blockchain members. To generate keys:

```
make keys
```

By default, this will generate 5 key pairs. If you want to generate a different number of keys, you can specify the N parameter:

```
make keys N=7
```

The keys will be stored in the `src/main/java/pt/tecnico/ulisboa/keys` directory.

## Testing

To run the unit tests:

```
make test
```

This will execute the unit tests for the **authenticated perfect links**.

## Running the System

### Starting a Server Node

To start a server node:

```
make server
```

This will start a server with ID 1 using the keys in the default location. The command arguments are the server ID and the keys directory path.

If you want to start additional server nodes, you can run the underlying command directly:

```
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.Node" -Dexec.args="2 src/main/java/pt/tecnico/ulisboa/keys"
```

Replace `2` with the desired server ID.

### Starting a Client

To start a client:

```
make client
```

This will start a client with ID 1 using the keys in the default location. The command arguments are the client ID and the keys directory path.

If you want to start additional clients, you can run the underlying command directly:

```
mvn exec:java -Dexec.mainClass="pt.tecnico.ulisboa.client.Client" -Dexec.args="2 src/main/java/pt/tecnico/ulisboa/keys"
```

Replace `2` with the desired client ID.

### Running the Consensus Demo

To run a demonstration of the consensus protocol with 4 server nodes and 1 client:

```
make consensus
```

This will start 4 server nodes and 1 client, allowing you to observe the consensus protocol in action.

**Note**: Due to the limited timeframe, it wasn't possible to debug all the code before the delivery date. Some components may not function as expected.