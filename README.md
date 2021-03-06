# Http2.0 Server and Client

## Usage
first you should install the Java environment and maven, please download jdk from [JDK 8+](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html) and maven from [Maven](https://maven.apache.org/download.cgi).
then cd to the pom.xml directory

Please make sure your JAVA_HOME environment variable is set and points to your JDK installation
Your can run the following command to set JAVA_HOME points to JDK

```bash
export JAVA_HOME=jdk-install-dir
export PATH=$JAVA_HOME/bin:$PATH
```

and run

```bash
mvn -v
```
to confirm mvn installation success

```bash
$ mvn clean package
$ cd target
$ java -cp http2-1.0-SNAPSHOT-shaded.jar http2.server.Server
** then you will see **
HTTP/2 Server is listening on https://127.0.0.1:8888/

** open another term **
$ java -cp http2-1.0-SNAPSHOT-shaded.jar http2.client.Client
```

