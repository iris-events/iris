package id.global.plugin.model.generator;

import com.sun.codemodel.JCodeModel;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Mojo(name = "generate-amqp-models", defaultPhase = LifecyclePhase.COMPILE)
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


    public void execute() throws MojoExecutionException {
        if (!skip) {
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
            Path p = Paths.get(project.getBasedir().toURI());
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
            deleteDirectoryAll(Path.of(project.getBasedir().toURI()).resolve(tmpFolder));
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

        Path path = Paths.get(project.getBasedir().toURI())
                .resolve(tmpSchemaFolder)
                .resolve(fileName);

        content = content.replace("#", "");

        Path p = Paths.get(project.getBasedir().toURI()).resolve(tmpSchemaFolder);


        //TODO windows will have problems ... check what original document has.. if generated document is OS dependent format
        //TODO: then we need to fix this if document is always in UNIX style path then no need to fix this
        content = content.replace(
                "\\/components\\/schemas\\/",
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

    public void drillDown(JSONObject root, String padding) {
        getLog().info("Creating JsonSchema files for model generator!");

        for (String key : (Iterable<String>) root.keySet()) {
            writeSchemaFile(key, root.get(key).toString());
        }
    }

    public void parseAsyncApiJson(String json) throws IOException, ParseException {
        getLog().info("Parsing AsyncApi definition!");

        JSONParser jsonParser = new JSONParser();

        JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
        JSONObject components = getJsonObject(jsonObject, "components");
        JSONObject schemas = getJsonObject(components, "schemas");
        JSONObject channels = getJsonObject(jsonObject, "channels");

        //create directory structure
        Path pathSource = Paths.get(project.getBasedir().toURI()).resolve(tmpSourceFolder);
        Path pathSchema = Paths.get(project.getBasedir().toURI()).resolve(tmpSchemaFolder);
        Files.createDirectories(pathSource);
        Files.createDirectories(pathSchema);

        drillDown(schemas, " ");

        for (String fileName : (Iterable<String>) schemas.keySet()) {
            generate(fileName);
        }

        generateAdditionalFiles(channels);
    }


    public void generateAdditionalFiles(JSONObject channels) {
        String stringPath = packageName.replace(".", File.separator);

        Path path = Paths.get(project.getBasedir().toURI())
                .resolve(tmpSourceFolder)
                .resolve(stringPath + File.separator + modelName)
                .resolve("client");

        String replaceWith = generateChannelSupportData(channels);
        writeFile("Exchanges.java", prepareExchangeTemplate("Exchanges.java", replaceWith), path.toString());

        Path pomPath = Paths.get(project.getBasedir().toURI())
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

    public JSONObject getJsonObject(JSONObject jsonObject, String key) {
        AtomicReference<JSONObject> o = new AtomicReference<>(jsonObject);
        Arrays.stream(key.split("\\.")).forEach(
                k -> o.set((JSONObject) o.get().get(k))
        );
        return o.get();
    }


    public String generateChannelSupportData(JSONObject jsonObject) {
        getLog().info("Creating JsonSchema generateChannelSupportData!");

        StringBuilder sb = new StringBuilder();


        for (String key : (Iterable<String>) jsonObject.keySet()) {

            String exchange = key.split("/")[0];
            String routingKey = key.split("/")[1];

            JSONObject exchangeObject = getJsonObject(jsonObject, key + "." + "bindings.amqp.exchange");
            String type = (String) exchangeObject.get("type");

            sb.append(
                    //will output ->  MYEXCHANGE_APTOCARDEVENT_ROOT("MYEXCHANGE","AptoCardEvent_Root","direct"),
                    String.format(enumTemplate,
                            exchange.toUpperCase(),
                            routingKey.toUpperCase(),
                            exchange,
                            routingKey,
                            type)
            );
        }

        int start = sb.toString().lastIndexOf(",");
        sb.replace(start, start + 1, ";");
        return sb.toString();
    }

    public void generate(String fileName) throws IOException {

        JCodeModel codeModel = new JCodeModel();

        Path clientPath = Paths.get(project.getBasedir().toURI()).resolve(tmpSourceFolder);
        Files.createDirectories(clientPath);

        SchemaMapper mapper = new SchemaMapper(
                new RuleFactory(
                        JsonSchemaGeneratorConfig.config,
                        new Jackson2Annotator(JsonSchemaGeneratorConfig.config),
                        new SchemaStore()),
                new SchemaGenerator());


        Path schemes = Paths.get(project.getBasedir().toURI()).resolve(tmpSchemaFolder).resolve(fileName);
        mapper.generate(codeModel, "ClassName", packageName + "." + modelName, schemes.toUri().toURL());

        codeModel.build(clientPath.toFile());
    }


    public void generateFromApiCurio(String baseUrl) {

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = defaultUrl;
        }

        String artifacts;

        try {
            artifacts = readContentFromWeb(baseUrl + "/api/artifacts");
            JSONParser jsonParser = new JSONParser();
            JSONArray jsonObject = (JSONArray) jsonParser.parse(artifacts);

            for (Object o : jsonObject) { //just an artifact name
                if (o.toString().endsWith("yaml")) //skip yaml files
                    continue;

                try {
                    String ymlContent = readContentFromWeb(baseUrl + "/api/artifacts/" + o);
                    getLog().info("Parsing for application: " + o);
                    modelName = o.toString().split(":")[0];
                    parseAsyncApiJson(ymlContent);
                } catch (IOException e) {
                    getLog().error("Generating from apicurio failed!", e);
                }
            }

        } catch (IOException | ParseException e) {
            getLog().error("Reading from apicurio failed!", e);
        }
    }
}