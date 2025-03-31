#!/bin/bash

# Better than nothing-script to set up a intermediate CA
# change variabkes before running!

root_alias=systest_root
root_password=$1
dname="CN=eidas2sandkasse root CA systest, OU=Digdir, C=no, 2.5.4.97=NTRNO-991825827"

# Generate root
keytool \
-genkey \
-alias $root_alias \
-keyalg RSA \
-keysize 4096 \
-validity 1825 \
-dname "$dname" \
-ext bc:ca:true \
-ext ku:c=keyCertSign,cRLSign \
-storetype pkcs12 \
-keystore $root_alias.p12 \
-storepass $root_password

echo "Root keystore in file <$root_alias.p12>"
base64 -i $root_alias.p12 -o $root_alias.b64
echo "Root keystore base64 in file <$root_alias.b64>"

# Export root cert
keytool \
-exportcert \
-rfc \
-alias $root_alias \
-keystore $root_alias.p12 \
-storepass $root_password \
-file $root_alias.cert

