# J

JSON reader and writer. Package `com.distantmelody.library.json`. The JDK is its only dependency.

```xml
<dependency>
  <groupId>com.distantmelody</groupId>
  <artifactId>j</artifactId>
  <version>0.1.0</version>
</dependency>
```

```
mvn test
```

Apache License 2.0. See [LICENSE](LICENSE).

Publishing `0.1.0` to Maven Central uses `mvn -Prelease deploy` after the `com.distantmelody` namespace is verified, a Central Portal token is in `settings.xml` under server id `central`, and GPG signing works. That deploy has not been run.
