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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

@Mojo(name = "generate-amqp-models", defaultPhase = LifecyclePhase.COMPILE)
public class AmqpGeneratorMojo extends AbstractMojo {


    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "artifactSource", required = true)
    ArtifactSource artifactSource;

    //TODO: use this instead hardcoded url!
    @Parameter(property = "apicurioUrl", required = false)
    String apicurioUrl;

    @Parameter(property = "fileDestination", required = false)
    String fileDestination;

    @Parameter(property = "packageName", required = true)
    String packageName;


    @Parameter(property = "modelVersion", required = true)
    String modelVersion;

    @Parameter(property = "modelName", required = true)
    String modelName;

    private  final Path tmpFolder = Paths.get("models");
    private  final Path tmpSourceFolder = tmpFolder.resolve(Paths.get("src","main","java"));
    private  final Path tmpSchemaFolder = tmpFolder.resolve(Paths.get("schemas"));


    public void execute() throws MojoExecutionException {


        cleanUpDirectories();
        if (artifactSource == ArtifactSource.FILE) {
            generateFromFile(fileDestination);
        } else if (artifactSource == ArtifactSource.APICURIO) {
            generateFromApicurio();
        } else {
            throw new MojoExecutionException("Execute failed! Artifact source location not known!");
        }
    }

    public void generateFromFile(String fileDestination) {

        String ymlContent;
        try {
            Paths.get(project.getBasedir().toURI()).resolve("target").resolve("generated").resolve(fileDestination);
            ymlContent = readSchemaContent(project.getBasedir() + "/target/generated/" + fileDestination);
            parseAsyncApiJson(ymlContent, "");
        } catch (Exception e) {
            getLog().error("Parsing AsyncApi definition!", e);
        }
        getLog().info("Generation completed!");
    }

    public String readSchemaContent(String fileName) {
        try {
            return Files.readString(Paths.get(fileName));
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
                //TODO: check this out...
                text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            return text;
        } catch (IOException e) {
            getLog().error("Cannot read resource file content!", e);
        }
        return null;

    }

    public String readContentFromWeb(String contentUrl) throws IOException {
        //TODO: for later use if needed
        return "";
//        getLog().info("Reading AsyncApi definition from url: " + contentUrl);
//
//        URL url = new URL(contentUrl);
//        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
//        StringBuilder builder = new StringBuilder();
//        String inputLine;
//        while ((inputLine = in.readLine()) != null) {
//            builder.append(inputLine);
//        }
//        in.close();
//        return builder.toString();
    }

    public void writeSchemaFile(String fileName, String content) {

        Path path = Paths.get(project.getBasedir().toURI())
                .resolve(tmpSchemaFolder)
                .resolve(fileName);

        content = content.replace("#", "");

        Path p = Paths.get(project.getBasedir().toURI()).resolve(tmpSchemaFolder);


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

    public void parseAsyncApiJson(String json, String appName) throws IOException, ParseException {

        getLog().info("Parsing AsyncApi definition!");

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
        JSONObject components = (JSONObject) jsonObject.get("components");
        JSONObject schemas = (JSONObject) components.get("schemas"); //generate objects
        JSONObject channels = (JSONObject) jsonObject.get("channels"); //generate helpers (enums for exchanges, etc..)


        //create directory structure
        Path pathSource = Paths.get(project.getBasedir().toURI()).resolve(tmpSourceFolder);
        Path pathSchema = Paths.get(project.getBasedir().toURI()).resolve(tmpSchemaFolder);
        Files.createDirectories(pathSource);
        Files.createDirectories(pathSchema);


        drillDown(schemas, " ");


        for (String fileNema : (Iterable<String>) schemas.keySet()) {
            generate(fileNema);
        }

        //TODO: create Client package that contains different enums and helper function!
        generateChannelSupportData(channels);
        generateAdditonalFiles();

    }


    public void generateAdditonalFiles() {
        String stringPath = packageName.replace(".",File.separator);

        Path path = Paths.get(project.getBasedir().toURI())
                .resolve(tmpSourceFolder)
                .resolve(stringPath)
                .resolve("client");

        writeFile("Exchanges.java", prepareExchangeTemplate("Exchanges.java"), path.toString());

        Path pomPath = Paths.get(project.getBasedir().toURI())
                .resolve(tmpFolder);

        writeFile("pom.xml", preparePomTemplate("pom.xml"), pomPath.toString());

    }

    public String prepareExchangeTemplate(String templateFile) {
        String template = readResourceFileContent(templateFile);
        template = template.replace("!!!", modelName);
        //TODO: generate whatever will be needed here...
        template = template.replace("#####", """
                EXCHANGE1_ROUTING_KEY1("EXCHANGE1", "ROUTING_KEY1", "direct"),
                        EXCHANGE1_ROUTING_KEY2("EXCHANGE1", "ROUTING_KEY2", "direct"),
                        EXCHANGE2_ROUTING_KEY1("EXCHANGE2", "ROUTING_KEY1", "fanout"),
                        EXCHANGE3_ROUTING_KEY2("EXCHANGE3", "ROUTING_KEY2", "topic"),
                        EEEXCHANGE4_ROUTING_KEY3("EXCHANGE4", "ROUTING_KEY3", "fanout");""");
        return template;
    }

    public String preparePomTemplate(String templateFile) {
        String pomtemplate = readResourceFileContent(templateFile);
        pomtemplate = pomtemplate.replace("XXXX", modelName);
        pomtemplate = pomtemplate.replace("YYYY", modelVersion);
        return pomtemplate;
    }


    public void generateChannelSupportData(JSONObject jsonObject) {
        //TODO: do the magic for channel stuff!
        getLog().info("Creating JsonSchema generateChannelSupportData!");
        for (String key : (Iterable<String>) jsonObject.keySet()) {
            getLog().warn("Channel: " + key + "   " + jsonObject.get(key).toString());
        }
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
        //TODO: investigate what ClassName does!
        mapper.generate(codeModel, "ClassName", packageName, schemes.toUri().toURL());

        codeModel.build(clientPath.toFile());
    }


    public void generateFromApicurio() {
        String artifcats;
        try {
            artifcats = readContentFromWeb("https://schema.internal.globalid.dev/api/artifacts");
            JSONParser jsonParser = new JSONParser();
            JSONArray jsonObject = (JSONArray) jsonParser.parse(artifcats);

            for (Object o : jsonObject) { //just an artifact name
                if (o.toString().endsWith("yaml")) //skip yaml files
                    continue;

                try {
                    String ymlContent = readContentFromWeb("https://schema.internal.globalid.dev/api/artifacts/" + o);
                    getLog().info("applicationFor: " + o);
                    String applicationName = o.toString().split(":")[0]; //TODO: make it nicer
                    parseAsyncApiJson(ymlContent, applicationName);
                } catch (IOException e) {
                    getLog().error("Generating from appicurio failed!", e);
                }
            }

        } catch (IOException | ParseException e) {
            getLog().error("Reading from appicurio failed!", e);
        }
    }
}