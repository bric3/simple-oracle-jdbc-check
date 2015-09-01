# simple-oracle-jdbc-check
Just open a connection to an Oracle DB, with a stopwatch

### Packaging

```sh
mvn clean package
```

### Runing

Create a property file like :

```
url=jdbc:oracle:thin:@some_tns_entry
user=me
password=yay
query=SELECT dummy FROM dual
oracle.net.tns_admin=/etc
diagnosability=false
```

Then run the program this way (requires Java 8) : 

```sh
java -jar path/to/simple-oracle-jdbc-check-0.2-jar-with-dependencies.jar path/to/the_property_file.properties
```
