For additional information you can check our [wiki page](https://github.com/griddynamics/OpenGenesis/wiki).

#### How to run Genesis
To build project run:
```bash
mvn -Pdistributions clean package
```

in project directory(PROJECT_DIR).

To start Genesis server(in background) run:
```bash
"distribution-standalone/target/genesis-${VERSION}/genesis-${VERSION}/bin/genesis.sh start"
```

where ${VERSION} is your genesis artifact version, for example 2.4.0-SNAPSHOT

After Genesis has started you can access Web UI at:

http://\<host\>:\<genesis_port\>

where \<genesis_port\> is port configured in properties file, 8080 by default.

To stop Genesis service go to PROJECT_DIR and run:
```bash
"distribution-standalone/target/genesis-${VERSION}/genesis-${VERSION}/bin/genesis.sh stop"
```
