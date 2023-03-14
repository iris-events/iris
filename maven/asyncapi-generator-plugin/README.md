# SmallRye AsyncAPI Maven Plugin

This maven plugin allows you to generate the AsyncAPI Schema on build.

# Using the maven plugin

Add this to your pom.xml:

```xml
<plugin>
    <artifactId>asyncapi-schema-generator-maven-plugin</artifactId>
    <groupId>id.global.events</groupId>
    <executions>
        <execution>
            <goals>
                <goal>generate-schema</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The schema will appear as `target/generated/asyncapi.yaml` and `target/generated/asyncapi.json` by default.

It's recommended to use the latest non-snapshot version of this plugin

# Configuration options

- `apicurioRegistryUrl` - To upload your generated schemas to an Apicurio registry.
  ```xml
  <configuration>
      <apicurioRegistryUrl>https://{url-to-apicurio-server}/</apicurioRegistryUrl>
  </configuration>
  ```
- `uploadType` - If `apicurioRegistryUrl` in set this will override the upload type (`json` by default).
  ```xml
  <configuration>
      <apicurioRegistryUrl>https://{url-to-apicurio-server}/</apicurioRegistryUrl>
      <uploadType>yaml</uploadType>
  </configuration>
  ```
- `scanPackages` - To override the list of packages to scan for annotations.
  ```xml
  <configuration>
      <scanPackages>
          id.global.myhandler.package,id.global.otherhandler.package
      </scanPackages>
  </configuration>
  ```
- `scanClasses` - To override the list of classes to scan for annotations.
  ```xml
  <configuration>
      <scanClasses>
          MyEventHandler
      </scanClasses>
  </configuration>
  ```
- `scanExcludePackages` - List of packages to exclude from annotation scanning.
  ```xml
  <configuration>
      <scanExcludePackages>
          id.global.myhandler.package
      </scanExcludePackages>
  </configuration>
  ```
- `scanExcludeClasses` - List of classes to exclude from annotation scanning.
  ```xml
  <configuration>
      <scanExcludeClasses>
          MyEventHandler
      </scanExcludeClasses>
  </configuration>
  ```
- `excludeFromSchemas` - Listing a package or a package prefix in the `excludeFromSchemas` will force the schema generator to handle everything matching
  that package as a `object`. One example could be a `Map<K,V>` property in a event class. If the `java.util` package is not present
  in the `excludeFromSchemas` it will show up in the `components/schemas` part of the AsyncApi definition.
  ```xml
  <configuration>
      <excludeFromSchemas>
          <excludeFromSchema>java.util</excludeFromSchema>
          <excludeFromSchema>org.your.package.prefix</excludeFromSchema>
      </excludeFromSchemas>
  </configuration>
  ```
  Example of `java.util.Map` included into schema generation: 
  ```yaml
  components:
    schemas:
      ....
      Map(String,Object):
        type: object
      ....
      EventWithMap:
        type: object
        properties:
          someMap:
            $ref: "#/components/schemas/Map(String,Object)"
          anotherProperty:
            type: string
  ....
  ```
  Example of `java.util.Map` excluded from schema generation:
  ```yaml
  components:
    schemas:
    ....
    EventWithMap:
      properties:
        someMap:
          type: object
        anotherProperty:
          type: string
    ....
  ```
- `scanDependenciesDisable` - Disable scanning the project's dependencies for AsyncAPI model classes too. False by default.

[comment]: <> (- `outputDirectory` - To override the default `target/generated/` outputDirectory where the json and yaml files will be created.)

[comment]: <> (- `schemaFilename` - To override the default `openapi` filename. This will be the name without the extension.)

[comment]: <> (- `includeDependenciesScopes` - If the above `includeDependencies` is true, you can control what scopes should be included. Default is `compile, system`)

[comment]: <> (- `includeDependenciesTypes` - If the above `includeDependencies` is true, you can control what types should be included. Default is `jar`)

[comment]: <> (- `configProperties` - Load any properties from a file. Example `${basedir}/src/main/resources/application.properties`)


# Asyncapi schema upload

Asyncapi generator plugin also contains a UploadSchemaMojo dedicated for uploading your generated schema to the apicurio registry. The plugin is not project dependent and can be run as:
- Latest version:
```shell
mvn id.global.iris:asyncapi-schema-generator-maven-plugin:upload-schema -DschemaFilePath=/path/to/asyncapi.json -DregistryUrl=https://schema.tools.global.id/ -DartifactId=artifact-id -DartifactVersion=1.0.0-YOUR-VERSION

```
- Specific version `3.1.6-SNAPSHOT` for instance:
```shell
mvn id.global.iris:asyncapi-schema-generator-maven-plugin:3.1.6-SNAPSHOT:upload-schema -DschemaFilePath=/path/to/asyncapi.json -DregistryUrl=https://schema.tools.global.id/ -DartifactId=artifact-id -DartifactVersion=1.0.0-YOUR-VERSION
```
