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
    @Parameter(property = "apicurioUrl", required = true)
    String apicurioUrl;

    @Parameter(property = "packageName", required = true)
    String packageName;


    private static final String srcJava = "/src/main/java";
    private static final String schemaTempDir = "schemas";
    private static final String tmpDirectory = "/";


    private final GenerationConfig config = new DefaultGenerationConfig() {
        @Override
        public boolean isGenerateBuilders() { // set config option by overriding method
            return true;
        }

        @Override
        public boolean isUseInnerClassBuilders() {
            return true;
        }

        @Override
        public boolean isIncludeDynamicBuilders() {
            return true;
        }

        @Override
        public boolean isSerializable() {
            return true;
        }

        //TODO: set to true, when apicurio schema is fixed
        @Override
        public boolean isIncludeAdditionalProperties() {
            return false;
        }
    };


    public void execute() throws MojoExecutionException, MojoFailureException {
        cleanUpDirectories();
        if (artifactSource == ArtifactSource.DISK) {
            generateFromFile();
        } else {
            generateFromApicurio();
        }
    }

    public void generateFromFile() {

        try {
            String ymlContent = null;
            try {
                ymlContent = readFileContent("vojko.json");
                parseAsyncApiJson(ymlContent,"");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            getLog().info("Generation completed!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void generateFromApicurio() {
        String artifcats;
        try {
            artifcats = readContentFromWeb("https://schema.internal.globalid.dev/api/artifacts");
            JSONParser jsonParser = new JSONParser();
            JSONArray jsonObject = (JSONArray) jsonParser.parse(artifcats);


            for (Object o : jsonObject) {  //just an artifact name
                if (o.toString().endsWith("yaml"))  //skip yaml files
                    continue;

                try {
                    String ymlContent = readContentFromWeb("https://schema.internal.globalid.dev/api/artifacts/" + o);
                    getLog().info("applicationFor: " + o);
                    String applicationName = o.toString().split(":")[0]; //TODO: make it nicer
                    parseAsyncApiJson(ymlContent, applicationName);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void cleanUpDirectories() {
        try {
            FileUtils.deleteDirectory(Path.of(project.getBasedir() + "/src").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.deleteDirectory(Path.of(project.getBasedir() + "/schemas").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public String readFileContent(String fileName) {
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
            myWriter = new FileWriter(project.getBasedir() + "/" + schemaTempDir + "/" + fileName);
            content = content.replace("#", "file:");
            content = content.replace(
                    "\\/components\\/schemas\\/",
                    project.getBasedir() + "/" + schemaTempDir + "/"
            );
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

        File f = Path.of(project.getBasedir() + tmpDirectory + srcJava).toFile();
        f.mkdirs();
        File f2 = Path.of(project.getBasedir() + "/" + schemaTempDir + "/").toFile();
        f2.mkdirs();

        for (String key : (Iterable<String>) root.keySet()) {
            writeFile(key, root.get(key).toString());
        }
    }


    public void parseAsyncApiJson(String json,String appName) throws IOException, ParseException {

        getLog().info("Parsing AsyncApi definition!");

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
        JSONObject components = (JSONObject) jsonObject.get("components");
        JSONObject schemas = (JSONObject) components.get("schemas"); //generate objects
        JSONObject channels = (JSONObject) jsonObject.get("channels"); //generate helpers (enums for exchanges, etc..)

        drillDown(schemas, " ");
        generateChannelSupportData(channels);

        for (String fileNema : (Iterable<String>) schemas.keySet()) {
            generate(fileNema);
        }

        //TODO: create Client package that contains different enums and helper function!
        generateSupportClasses(appName);
//        writeFile("version.txt",appName,project.getBasedir()+"/");

    }

    public void generateSupportClasses(String appname) {
        String path = project.getBasedir() + tmpDirectory + srcJava
                + "/" + packageName.replace(".", "/")
                .substring(0, packageName.lastIndexOf("."))
                + "/client/"+appname;

        Path.of(path).toFile().mkdirs();


        String template =  readFileContent("Exchanges.java");
        template = template.replace("!!!", appname);
        template = template.replace("#####", """
                        
        EXCHANGE1_ROUTING_KEY1("EXCHANGE1", "ROUTING_KEY1", "direct"),
                EXCHANGE1_ROUTING_KEY2("EXCHANGE1", "ROUTING_KEY2", "direct"),
                EXCHANGE2_ROUTING_KEY1("EXCHANGE2", "ROUTING_KEY1", "fanout"),
                EXCHANGE3_ROUTING_KEY2("EXCHANGE3", "ROUTING_KEY2", "topic"),
                EEEXCHANGE4_ROUTING_KEY3("EXCHANGE4", "ROUTING_KEY3", "fanout");""");

        String pomtemplate =  readFileContent("xpom.xml");

        writeFile("Exchanges.java",template, path);
//        writeFile("xpom.xml",pomtemplate, project.getBasedir().toString());

    }

    public void generateChannelSupportData(JSONObject jsonObject) {
        getLog().info("Creating JsonSchema generateChannelSupportData!");
        for (String key : (Iterable<String>) jsonObject.keySet()) {
            getLog().warn("Channel: " + key + "   " + jsonObject.get(key).toString());
        }
    }

    public void generate(String fileName) throws IOException, ParseException {
        getLog().info("Generating Java class from JsonSchema file: " + fileName);
        JCodeModel codeModel = new JCodeModel();
        URL source = Path.of(project.getBasedir() + "/" + schemaTempDir + "/" + fileName).toFile().toURI().toURL();
        File directory = Path.of(project.getBasedir() + tmpDirectory + srcJava).toFile();
        directory.mkdirs();
        SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
        mapper.generate(codeModel, "ClassName", packageName, source);
        codeModel.build(Path.of(project.getBasedir() + tmpDirectory + srcJava).toFile());
    }
}