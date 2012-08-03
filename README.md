For additional information you can check our [wiki page](https://github.com/griddynamics/OpenGenesis/wiki).

#### How to run Genesis
To build project run:
```bash
mvn clean package
```

in project directory(PROJECT_DIR).

To start Genesis server run:
```bash
java -Dbackend.properties=(path to the configuration file) -cp "${TARGET}/*" com.griddynamics.genesis.GenesisFrontend
```

After Genesis has started you can access Web UI at:

http://\<host\>:\<genesis_port\>

where \<genesis_port\> is port configured in properties file, 8080 by default.
