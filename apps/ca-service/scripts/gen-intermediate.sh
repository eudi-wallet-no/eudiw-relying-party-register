#!/bin/bash

# Better than nothing-script to set up a intermediate CA
# change variables before running!

root_alias=root2
root_password=$1
root_crl_url=https://ca.eidas2sandkasse.dev/v1/certs/root.crl
root_cert_url=https://ca.eidas2sandkasse.dev/v1/certs/root.cer

ca_alias=eaa_provider2
ca_password=changeit
ca_dname="CN=eidas2sandkasse EAA Provider CA 2 dev, O=DIGITALISERINGSDIREKTORATET, C=NO, organizationIdentifier=NTRNO-NOFOR.991825827"

# Generate intermediate and csr
keytool \
-genkey \
-alias $ca_alias \
-keyalg EC  \
-groupname secp256r1 \
-sigalg SHA256withECDSA \
-validity 1824 \
-dname "$ca_dname" \
-ext bc:ca:true \
-ext ku:c=keyCertSign,cRLSign \
-storetype pkcs12 \
-storepass $ca_password \
-keystore $ca_alias.p12

keytool \
-keystore $ca_alias.p12 \
-certreq \
-alias $ca_alias \
-storepass $ca_password \
-file $ca_alias.csr

# Sign csr with root
keytool \
-keystore $root_alias.p12 \
-storepass $root_password \
-gencert \
-alias $root_alias \
-sigalg SHA256withECDSA \
-ext bc:ca:true \
-ext ku:c=keyCertSign,cRLSign \
-ext aia=caIssuers:uri:$root_cert_url \
-ext crl=uri:$root_crl_url \
-validity 1816 \
-infile $ca_alias.csr \
-rfc \
-outfile $ca_alias.cert

# Import root and signed intermediate certificate
keytool \
-importcert \
-trustcacerts \
-keystore $ca_alias.p12 \
-storepass $ca_password \
-alias $root_alias \
-file  $root_alias.cert

keytool \
-importcert \
-keystore $ca_alias.p12 \
-storepass $ca_password \
-alias $ca_alias \
-file $ca_alias.cert

echo "CA keystore in file <$ca_alias.p12>"
base64 -i $ca_alias.p12 -o $ca_alias.b64
echo "CA keystore base64 in file <$ca_alias.b64>"
