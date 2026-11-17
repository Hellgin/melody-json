# melody-json

JSON reader and writer. The class is `com.distantmelody.library.json.J`. The JDK is its only dependency. Requires Java 8.

```xml
<dependency>
  <groupId>com.distantmelody</groupId>
  <artifactId>melody-json</artifactId>
  <version>1.0.1</version>
</dependency>
```

```
mvn test
```

Apache License 2.0. See [LICENSE](LICENSE).

Publishing `1.0.1` to Maven Central uses `mvn -Prelease deploy` after the `com.distantmelody` namespace is verified, a Central Portal token is in `settings.xml` under server id `central`, and GPG signing works. That deploy has not been run.
