package id.global.plugin.model.generator;

import com.sun.codemodel.JCodeModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.nio.file.Path;

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


    private static final String srcJava = "aaa/src/main/java";
    private static final String schemaTempDir = "aaa/schemas";
    private static final String tmpDirectory = "/";


    private static final String temporaryFolder = "/models/";
    private static final String temporarySourceFolder = temporaryFolder + "src/main/java/";
    private static final String temporarySchemaFolder = temporaryFolder + "schemas/";


    public void execute() throws MojoExecutionException, MojoFailureException {
        cleanUpDirectories();
        if (artifactSource == ArtifactSource.FILE) {
            generateFromFile(fileDestination);
        } else {
            generateFromApicurio();
        }
    }

    public void generateFromFile(String fileDestination) {

        try {
            String ymlContent = null;
            try {
                ymlContent = readSchemaContent(project.getBasedir() + "/target/generated/" + fileDestination);
                parseAsyncApiJson(ymlContent, "");
            } catch (IOException e) {
                e.printStackTrace();
            }

            getLog().info("Generation completed!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readSchemaContent(String fileName) {
        String text = "";
        try {
            InputStream inputStream = getFileInputStream(fileName);
            text = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
            inputStream.close();
        } catch (URISyntaxException | IOException | ParseException e) {
            e.printStackTrace();
        }
        return text;
    }


    public InputStream getFileInputStream(String fileName) throws URISyntaxException, IOException, ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = new FileInputStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }


    public void cleanUpDirectories() {
        try {
            FileUtils.deleteDirectory(Path.of(project.getBasedir() + "/aaa/src").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.deleteDirectory(Path.of(project.getBasedir() + "/aaa/schemas").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String readFileContent(String fileName) {
        String text = "";
        try {
            InputStream inputStream = getResourceFileInputStream(fileName);
            text = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
            inputStream.close();
        } catch (URISyntaxException | IOException | ParseException e) {
            e.printStackTrace();
        }
        return text;
    }

    public InputStream getResourceFileInputStream(String fileName) throws URISyntaxException, IOException, ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    public String readContentFromWeb(String contentUrl) throws IOException {
        getLog().info("Reading AsyncApi definition from url: " + contentUrl);

        URL url = new URL(contentUrl);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder builder = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            builder.append(inputLine);
        }
        in.close();
        return builder.toString();
    }

    public void writeFile(String fileName, String content) {
        FileWriter myWriter = null;
        try {
            myWriter = new FileWriter(project.getBasedir() + temporarySchemaFolder + fileName);
            content = content.replace("#", "file:");
            content = content.replace(
                    "\\/components\\/schemas\\/",
                    project.getBasedir() + temporarySchemaFolder);
            myWriter.write(content);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeFile(String fileName, String content, String path) {
        FileWriter myWriter = null;
        try {
            myWriter = new FileWriter(path + "/" + fileName);
            myWriter.write(content);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void drillDown(JSONObject root, String padding) {
        getLog().info("Creating JsonSchema files for model generator!");

        for (String key : (Iterable<String>) root.keySet()) {
            writeFile(key, root.get(key).toString());
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
        Path.of(project.getBasedir() + temporarySourceFolder).toFile().mkdirs();
        Path.of(project.getBasedir() + temporarySchemaFolder).toFile().mkdirs();


        drillDown(schemas, " ");


        for (String fileNema : (Iterable<String>) schemas.keySet()) {
            generate(fileNema);
        }

        //TODO: create Client package that contains different enums and helper function!
        generateChannelSupportData(channels);
        generateAdditonalFiles();

    }


    public void generateAdditonalFiles() {

        String supportDataPath = project.getBasedir() + temporarySourceFolder + packageName.replace(".", "/") + "/client/";
        Path.of(supportDataPath).toFile().mkdirs();
        writeFile("Exchanges.java", prepareExchangeTemplate("Exchanges.java"), supportDataPath);


        String pomFileLocation = project.getBasedir() + temporaryFolder;
        writeFile("pom.xml", preparePomTemplate("pom.xml"), pomFileLocation);

    }

    public String prepareExchangeTemplate(String templateFile){
        String template = readFileContent(templateFile);
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

    public String preparePomTemplate(String templateFile){
        String pomtemplate = readFileContent(templateFile);
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

    public void generate(String fileName) throws IOException, ParseException {

        JCodeModel codeModel = new JCodeModel();
        URL source = Path.of(project.getBasedir() + temporarySchemaFolder + fileName).toFile().toURI().toURL();
        Path.of(project.getBasedir() + temporarySourceFolder).toFile().mkdirs();


        SchemaMapper mapper = new SchemaMapper(
                new RuleFactory(
                        JsonSchemaGeneratorConfig.config,
                        new Jackson2Annotator(JsonSchemaGeneratorConfig.config),
                        new SchemaStore()),
                new SchemaGenerator());


        //TODO: investigate what ClassName does!
        mapper.generate(codeModel, "ClassName", packageName, source);


        codeModel.build(Path.of(project.getBasedir() + temporarySourceFolder).toFile());
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
                    e.printStackTrace();
                }
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}