# Tests

Run from the directory containing pom.xml with JDK 21 and Maven:

```bash
mvn clean compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

Compile the standalone test classes with `target/classes` and the contents of `target/classpath.txt` on the classpath. Put the output in `target/test-classes`. Then run these main classes with both output directories and dependencies on the classpath:

- `MaterialCatalogTest`: naming/category differences, subject filtering, stock identity, unknown inventory, zero-stock catalog entries.
- `BatchRequestTest`: JDBC-double transaction rollback, stale availability and input validation; Java syntax parsing.
- `StudentSchemaTest`: JDBC-double migration and actionable permission errors.
- `FullIntegrationTest`: real database plus actual JavaFX FXML and button actions; 37 checks.

On Linux/macOS:

```bash
mkdir -p target/test-classes
TEST_CP="target/classes:$(cat target/classpath.txt)"
javac -cp "$TEST_CP" -d target/test-classes tests/*.java
java -cp "$TEST_CP:target/test-classes" MaterialCatalogTest
java -cp "$TEST_CP:target/test-classes" BatchRequestTest
java -cp "$TEST_CP:target/test-classes" StudentSchemaTest
```

Use `;` as the Java classpath separator on Windows.

## Full integration test

Use a disposable local MySQL/MariaDB test server. The test connects to `jdbc:mysql://127.0.0.1:3307/` as root with an empty password by default and creates uniquely named `lab_booking_integration_...` databases. It drops only those databases afterward. Do not point this test at your production database server.

Override the server URL with `-Dtest.server=jdbc:mysql://127.0.0.1:PORT/`. A graphical desktop is supported directly:

```bash
java -cp "$TEST_CP:target/test-classes" FullIntegrationTest
```

For headless Linux, add `org.testfx:openjfx-monocle:21.0.2` to the test classpath and pass:

```text
-Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.order=sw
```

`-Dtest.output=PATH` selects the directory for `student-cart.png`. This directory must exist.
The fixture is the original project's schema and seed stock. The test verifies both upgrade from that schema and the actual shipped fresh-install/manual-upgrade SQL.
