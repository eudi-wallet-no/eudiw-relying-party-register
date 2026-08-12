# How to update trusted lists


Remember to update the date of list issuance. You do this in the yaml file in the field list-issue-date-time for the specific list.  
You should set it to the date you update the list. Do not set it forward in time, because then the application will not start.

## Update 612 trustlist

Please see [this](https://docs.digdir.no/docs/lommebok/lommebok_taibruk_registrering_utstedar.html#registrering-p%C3%A5-tillitslista) link to docs in order to see what we need in the list. The external party (issuer) will need to provide the information specified here.  
Based on this, you will need to update the application-*.yaml with the new entry. As a starting point, [here](./src/main/resources/application.yaml) is application-yaml.



## Update 602 trustlists

In order to update the 602 trustlist, there are several mandatory parameters.
You will need to add the entity to the list of trusted entities. 
Spec [here](https://www.etsi.org/deliver/etsi_ts/119600_119699/119602/01.01.01_60/ts_119602v010101p.pdf).

You will need to update the application-*.yaml with the new entry. As a starting point, [here](./src/main/resources/application.yaml) is application.yaml.  
These are the different fields you should add to the configuration for the specific list you shall update:

### Add new trusted entity

The TrustedEntitiesList component provides a list of trusted entities and their services approved in accordance
with the list of trusted entities scheme.

#### Mandatory parameters

| Field            |                                          Explanation                                          |                                                                                            Must match |
|:-----------------|:---------------------------------------------------------------------------------------------:|------------------------------------------------------------------------------------------------------:|
| TEName           |                sequence of multilingual character strings. Orgname from BRREG.                |              Organization Name (O) in Subject of X509 certificate(s) in the Trusted Entity's services |
| TETradeName      |               sequence of multilingual character strings. Orgnumber from BRREG.               |   Organization Identifier (ORG_ID) in Subject of X509 certificate(s) in the Trusted Entity's services |
| TEAddress        | Contains TEPostalAddress and TEElectronicAddress (fill these out, PID specific: phone number) |                                                                                                       |
| TEInformationURI |       sequence of multilingual pointers. Specific requirements for the different lists.       |                                                                                                       |

### Add a service to the trusted entity

The TrustedEntityServices component contains a sequence identifying each of the TE's recognized services.

#### Mandatory parameters

| Field                  |                                                Explanation                                                 |                                                            Must match |
|:-----------------------|:----------------------------------------------------------------------------------------------------------:|----------------------------------------------------------------------:|
| ServiceTypeIdentifier  |         Identifier of service type expressed as a URI. Specific requirements for different lists.          |                     URI of one of the service types specified in spec |
| ServiceName            |          sequence of multilingual character strings. Name under which the TE provides the service          |                                                                       |
| ServiceDigitalIdentity | Contains x509 certificate(s) as Base64 string(in our lists). Cert shall contain attribute organizationName | In subject of X509 certificate(s): O -> TEName, ORG_ID -> TETradeName |


## Future work

As of now, we have not implemented any history elements. This shall be implemented in the future.

