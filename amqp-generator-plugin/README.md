# AMQP Model generator Maven Plugin

This maven plugin allows you to generate models from AsyncAPI Schema on build.

# Using the maven plugin

# Available configurations
| propertyName  | required  | options  |  defaultValue | dependsOn/Notes  |
|---|---|---|---|---|
| artifactSource  |  yes |  FILE / APICURIO |  FILE | Chose if definition is read from url or file  |
| apicurioUrl | no  |   |  https://schema.internal.globalid.dev | artifactSource=APICURIO  |
| asyncApiFilename|  no |   | asyncapi.json  |  artifactSource=FILE |
| asyncApiDirectory | no  |   |  target,generated | artifactSource=FILE ; path should be seperated with ","  |
| modelVersion| yes  |   |   | modelVersion should use ${project.version}  |
| modelName|  yes |   |   | ModelName should use ${project.artifactId}  |
| packageName |  no |   | "id.global.amqp.models"  | Package name will be used with modelName  |
| skip| no  |   | false  |  Skip generation process |


Sample usage:

```xml
<plugin>
  <groupId>id.global.events</groupId>
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
<!--      <apicurioUrl>http://url.to.apicurio.registry</apicurioUrl>-->
      <asyncApiFilename>asyncapi.json</asyncApiFilename>
      <asyncApiDirectory>target,generated</asyncApiDirectory>
      <modelVersion>${project.version}</modelVersion>
      <modelName>${project.artifactId}</modelName>
      <packageName>id.global.amqp.models</packageName>
      <skip>false</skip>
  </configuration>
</plugin>
```

Generated models will appear in root folder `/models`.

It's recommended to use the latest non-snapshot version of this plugin

