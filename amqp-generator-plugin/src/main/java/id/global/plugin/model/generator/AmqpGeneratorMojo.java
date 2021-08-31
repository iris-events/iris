package id.global.plugin.model.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JCodeModel;

@Mojo(name = "generate-amqp-models", defaultPhase = LifecyclePhase.COMPILE, requiresProject = false)
public class AmqpGeneratorMojo extends AbstractMojo {

    private static final String defaultUrl = "https://schema.internal.globalid.dev";
    private static final String enumTemplate = """
                %s_%s("%s","%s","%s"),
            """;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "artifactSource", required = true)
    ArtifactSource artifactSource;

    @Parameter(property = "apicurioUrl")
    String apicurioUrl;

    @Parameter(property = "asyncApiFilename")
    String asyncApiFilename = "asyncapi.json";

    @Parameter(property = "asyncApiDirectory")
    String asyncApiDirectory = "target,generated";

    @Parameter(property = "packageName")
    String packageName = "id.global.models";

    @Parameter(property = "modelVersion", required = true)
    String modelVersion;

    @Parameter(property = "modelName", required = true)
    String modelName;

    @Parameter(property = "skip")
    boolean skip = false;

    private final Path tmpFolder = Paths.get("models");
    private final Path tmpSourceFolder = tmpFolder.resolve(Paths.get("src", "main", "java"));
    private final Path tmpSchemaFolder = tmpFolder.resolve(Paths.get("schemas"));

    private URI baseDir;

    public void execute() throws MojoExecutionException {
        modelName = getCleanModelName(modelName);

        if (!skip) {
            if (project.getBasedir() != null) {
                baseDir = project.getBasedir().toURI();
            } else {
                try {
                    baseDir = new URI("file:" + System.getProperty("user.dir"));
                } catch (URISyntaxException e) {
                    getLog().error("Something is wrong with folder structure!", e);
                }
            }
            cleanUpDirectories();
            if (artifactSource == ArtifactSource.FILE) {
                generateFromFile(asyncApiDirectory, asyncApiFilename);
            } else if (artifactSource == ArtifactSource.APICURIO) {
                generateFromApiCurio(apicurioUrl);
            } else {
                throw new MojoExecutionException("Execute failed! Artifact source location not known!");
            }
        } else {
            getLog().info("Skipping model generation! Skip: [" + skip + "]");
        }
    }

    public void generateFromFile(String asyncApiDirectory, String asyncApiFilename) {

        String ymlContent;
        try {

            String[] pathSpliced;
            pathSpliced = asyncApiDirectory.split(","); //separate directory via , to be platform independent
            Path p = Paths.get(baseDir);
            for (String sub : pathSpliced) {
                p = p.resolve(sub);
            }
            p = p.resolve(asyncApiFilename);
            ymlContent = readSchemaContent(p);
            parseAsyncApiJson(ymlContent);
        } catch (Exception e) {
            getLog().error("Parsing AsyncApi definition!", e);
        }
        getLog().info("Generation completed!");
    }

    public String readSchemaContent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            getLog().error("Schema content read failed!", e);
        }
        return null;
    }

    public void cleanUpDirectories() {
        try {
            deleteDirectoryAll(Path.of(baseDir).resolve(tmpFolder));
        } catch (IOException e) {
            getLog().error("Directory cleanup failed!", e);

        }
    }

    public static void deleteDirectoryAll(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk
                        .sorted(Comparator.reverseOrder())
                        .forEach(AmqpGeneratorMojo::deleteDirectory);
            }
        }

    }

    public static void deleteDirectory(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            System.err.printf("Unable to delete this path : %s%n%s", path, e);
        }
    }

    public String readResourceFileContent(String fileName) {
        String text;
        try {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                assert is != null;
                text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            return text;
        } catch (IOException e) {
            getLog().error("Cannot read resource file content!", e);
        }
        return null;

    }

    //Use this when plugin will have to generate models from apicurio url
    public String readContentFromWeb(String contentUrl) throws IOException {
        getLog().info("Reading AsyncApi definition from url: " + contentUrl);
        URL url = new URL(contentUrl);
        String inputLine;
        StringBuilder builder = new StringBuilder();
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(url.openStream()))) {
            while ((inputLine = br.readLine()) != null) {
                builder.append(inputLine);
            }
        }

        return builder.toString();
    }

    public void writeSchemaFile(String fileName, String content) {

        Path path = Paths.get(baseDir)
                .resolve(tmpSchemaFolder)
                .resolve(fileName);

        content = content.replace("#", "");

        Path p = Paths.get(baseDir).resolve(tmpSchemaFolder);

        //TODO windows will have problems ... check what original document has.. if generated document is OS dependent format
        //TODO: then we need to fix this if document is always in UNIX style path then no need to fix this
        content = content.replace(
                "/components/schemas/",
                p.toUri().toString()
        );
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            getLog().error("Failed to write schema file", e);
        }

    }

    public void writeFile(String fileName, String content, String path) {

        try {
            Path clientPath = Paths.get(path);
            Files.createDirectories(clientPath);

            Path filePath = Paths.get(path, fileName);
            Files.writeString(filePath, content);
        } catch (IOException e) {
            getLog().error("Failed to write file", e);
        }
    }

    public void drillDown(JsonNode root, String padding) {
        getLog().info("Creating JsonSchema files for model generator!");
        root.fields().forEachRemaining((k) -> writeSchemaFile(k.getKey(), k.getValue().toString()));
    }

    public void parseAsyncApiJson(String json) throws IOException {
        getLog().info("Parsing AsyncApi definition!");

        ObjectMapper mapper = new ObjectMapper();
        //        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode node = mapper.readTree(json);

        JsonNode components = node.get("components");
        JsonNode schemas = components.get("schemas");
        JsonNode channels = node.get("channels");

        //create directory structure
        Path pathSource = Paths.get(baseDir).resolve(tmpSourceFolder);
        Path pathSchema = Paths.get(baseDir).resolve(tmpSchemaFolder);
        Files.createDirectories(pathSource);
        Files.createDirectories(pathSchema);

        drillDown(schemas, " ");

        schemas.fieldNames().forEachRemaining(fileName -> {
                    try {
                        generate(fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );

        generateAdditionalFiles(channels);
    }

    public void generateAdditionalFiles(JsonNode channels) {
        String stringPath = packageName.replace(".", File.separator);

        Path path = Paths.get(baseDir)
                .resolve(tmpSourceFolder)
                .resolve(stringPath + File.separator + modelName)
                .resolve("client");

        try {
            String replaceWith = generateChannelSupportData(channels);
            writeFile("Exchanges.java", prepareExchangeTemplate("Exchanges.java", replaceWith), path.toString());
        } catch (Exception ignored) {
        }

        Path pomPath = Paths.get(baseDir)
                .resolve(tmpFolder);

        writeFile("pom.xml", preparePomTemplate("pom.xml"), pomPath.toString());

    }

    public String prepareExchangeTemplate(String templateFile, String content) {
        String template = readResourceFileContent(templateFile);
        template = template.replace("!!!", modelName);
        template = template.replace("#####", content);
        return template;
    }

    public String preparePomTemplate(String templateFile) {
        String pomTemplate = readResourceFileContent(templateFile);
        pomTemplate = pomTemplate.replace("XXXX", modelName);
        pomTemplate = pomTemplate.replace("YYYY", modelVersion);
        return pomTemplate;
    }

    public String generateChannelSupportData(JsonNode jsonObject) {
        getLog().info("Creating JsonSchema generateChannelSupportData!");

        StringBuilder sb = new StringBuilder();

        jsonObject.fields().forEachRemaining(k -> {
            String exchange = k.getKey().split("/")[0];
            String routingKey = k.getKey().split("/")[1];
            String type = k.getValue().path("bindings").path("amqp").path("exchange").path("type").textValue();

            sb.append(
                    //will output ->  MYEXCHANGE_APTOCARDEVENT_ROOT("MYEXCHANGE","AptoCardEvent_Root","direct"),
                    String.format(enumTemplate,
                            exchange.toUpperCase(),
                            routingKey.toUpperCase(),
                            exchange,
                            routingKey,
                            type)
            );
        });

        int start = sb.toString().lastIndexOf(",");
        sb.replace(start, start + 1, ";");
        return sb.toString();
    }

    public void generate(String fileName) throws IOException {

        JCodeModel codeModel = new JCodeModel();

        Path clientPath = Paths.get(baseDir).resolve(tmpSourceFolder);
        Files.createDirectories(clientPath);

        SchemaMapper mapper = new SchemaMapper(
                new RuleFactory(
                        JsonSchemaGeneratorConfig.config,
                        new Jackson2Annotator(JsonSchemaGeneratorConfig.config),
                        new SchemaStore()),
                new SchemaGenerator());

        Path schemes = Paths.get(baseDir).resolve(tmpSchemaFolder).resolve(fileName);
        mapper.generate(codeModel, "ClassName", packageName + "." + modelName, schemes.toUri().toURL());

        codeModel.build(clientPath.toFile());
    }

    //TODO: currenty all asyncapi json document are parsed!
    //TODO: uncomment filter part bellow to grab only one (version and name)
    public void generateFromApiCurio(String baseUrl) {

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = defaultUrl;
        }

        String artifacts;

        try {
            artifacts = readContentFromWeb(baseUrl + "/api/artifacts");

            ObjectMapper mm = new ObjectMapper();
            String[] obb = mm.readValue(artifacts, String[].class);
            for (Object o : obb) { //just an artifact name
                if (o.toString().endsWith("yaml")) //skip yaml files
                    continue;

                //TODO: filter out all other artifacts if name not equal
                //                if(!o.toString().contains(modelName))
                //                    continue;
                //TODO: filter out all other artifacts if version not equal
                //                if(!o.toString().contains(modelVersion))
                //                    continue;

                try {
                    String ymlContent = readContentFromWeb(baseUrl + "/api/artifacts/" + o);
                    getLog().info("Parsing for application: " + o);
                    modelName = o.toString().split(":")[0];
                    parseAsyncApiJson(ymlContent);
                } catch (IOException e) {
                    getLog().error("Generating from apicurio failed!", e);
                }
            }

        } catch (IOException e) {
            getLog().error("Reading from apicurio failed!", e);
        }
    }

    private String getCleanModelName(String modelName) {
        return modelName.toLowerCase().replace("-", "_");
    }
}