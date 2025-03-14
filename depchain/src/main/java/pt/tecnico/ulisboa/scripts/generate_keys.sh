#!/bin/bash

N=$1  # Number of key pairs to generate
KEY_DIR=${2:-../keys}  # Default to ../keys if not provided

if [[ -z "$N" ]]; then
    echo "Usage: $0 <number_of_keys> [key_directory]"
    exit 1
fi

# Create the keys directory if it doesn't exist
mkdir -p "$KEY_DIR"

# Remove old key files
rm -f "$KEY_DIR"/*.key

# Generate N key pairs
for i in $(seq -f "%02g" 0 $((N-1))); do
    openssl genpkey -algorithm RSA -out "$KEY_DIR/priv$i.key" -pkeyopt rsa_keygen_bits:4096 2>/dev/null
    openssl rsa -pubout -in "$KEY_DIR/priv$i.key" -out "$KEY_DIR/pub$i.key" 2>/dev/null
done

echo "Key generation completed: $N key pairs created in $KEY_DIR."

