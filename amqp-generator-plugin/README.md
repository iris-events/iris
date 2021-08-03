# AMQP Model generator Maven Plugin

This maven plugin allows you to generate models from AsyncAPI Schema on build.

# Using the maven plugin

Add this to your pom.xml:

```xml
<plugin>
  <groupId>id.global</groupId>
  <artifactId>amqp-generator-maven-plugin</artifactId>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>generate-amqp-models</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <artifactSource>FILE</artifactSource>
    <packageName>id.global.models.${artifactId}</packageName>
    <fileDestination>asyncapi.json</fileDestination>
    <modelName>${artifactId}</modelName>
    <modelVersion>${version}</modelVersion>
  </configuration>
</plugin>
```

Generated models will appear in `target/generated/asyncapi.yaml`.

It's recommended to use the latest non-snapshot version of this plugin

# Configuration options

- `artifactSource`
- `packageName`
- `fileDestination`
- `modelName`
- `modelVersion`  
