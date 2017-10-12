# core
This library offers some main functionalities and utilities for the Hobbit platform project. For more information take a look at the project web page (http://www.project-hobbit.eu) or at the Hobbit platform project on github (https://github.com/hobbit-project/platform).

### Building

`test` target needs RabbitMQ running. You may need to set RabbitMQ host in `src/test/java/org/hobbit/core/TestConstants.java`.

### Using it with Maven

```xml
  <repositories>
    <repository>
      <id>maven.aksw.internal</id>
      <name>University Leipzig, AKSW Maven2 Repository</name>
      <url>http://maven.aksw.org/repository/internal</url>
    </repository>
    <repository>
      <id>maven.aksw.snapshots</id>
      <name>University Leipzig, AKSW Maven2 Repository</name>
      <url>http://maven.aksw.org/repository/snapshots</url>
    </repository>
    ...
  </repositories>

  <dependencies>
    <!-- Hobbit core -->
    <dependency>
      <groupId>org.hobbit</groupId>
      <artifactId>core</artifactId>
      <version>1.0.3</version>
    </dependency>
    ...
  </dependencies>
```
