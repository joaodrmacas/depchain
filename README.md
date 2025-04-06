# DepChain: A Dependable Permissioned Blockchain System

## Overview
DepChain is a permissioned blockchain system implementing the Byzantine Read/Write Epoch Consensus algorithm, designed to provide high dependability guarantees. The system tolerates up to `f` Byzantine failures in a `3f+1` node configuration.

## Key Features
- Byzantine Fault Tolerant consensus
- Permissioned blockchain with static membership
- Strong cryptographic security with digital signatures
- Smart contract support with ERC-20 token implementation
- Blacklist functionality for regulatory compliance

## Prerequisites
- Java JDK 11+
- Maven 3.6+

## Quick Start

### Building the Project
```bash
make build
```

### Key Generation
Generate key pairs for blockchain members:
```bash
make keys        # Generates 5 key pairs by default
make keys N=7    # Generate custom number of key pairs
```
Keys are stored in `src/main/java/pt/tecnico/ulisboa/keys/`.

### Testing Components
```bash
make test
```
This runs tests for authenticated perfect links and solidity contracts, ensuring these two components work correctly independently of the rest of the code. Tests are located in `/depchain/src/test/java/pt/tecnico/ulisboa/`. Go to the test files to see what exactly is being tested.

### Running the System

#### Launch Blockchain Network
```bash
make consensus
```
This command:
- Starts 4 blockchain servers in the background
- Launches an interactive client (ID: -1)
- This client is the owner of IST tokens with control over the blacklist and the initial supply and can be used to run the commands displayed in the terminal.

#### Add Additional Client
To test multi-client transactions:
```bash
make client
```
This launches a second client (ID: -2) in a separate terminal that automatically connects to the running network.

## Smart Contracts
The system includes two primary smart contracts:
- **ISTCoin**: An ERC-20 token with custom transfer rules
- **Blacklist**: Security contract for access control

### Aditional Byzantine behaviour tests
Under the `/depchain/src/main/java/pt/tecnico/ulisboa/scripts/` directory there are a bunch of scripts we used to test byzantine behaviour.

## Note
For detailed implementation information, please refer to the accompanying project report. This README focuses on setup and execution instructions.