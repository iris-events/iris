package id.global.plugin.model.generator;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.sun.codemodel.JCodeModel;

import id.global.plugin.model.generator.annotators.MetadataAnnotator;
import id.global.plugin.model.generator.configs.EventSchemaGeneratorConfig;
import id.global.plugin.model.generator.graph.GraphUtils;
import id.global.plugin.model.generator.models.ArtifactSource;
import id.global.plugin.model.generator.models.ChannelDetails;
import id.global.plugin.model.generator.models.ChannelSectionTypes;

@Mojo(name = "generate-amqp-models", defaultPhase = LifecyclePhase.COMPILE, requiresProject = false)
public class AmqpGeneratorMojo extends AbstractMojo {

    private static final String defaultUrl = "https://schema.internal.globalid.dev";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "artifactSource", required = true)
    ArtifactSource artifactSource;

    @Parameter(property = "apicurioUrl")
    String apicurioUrl;

    @Parameter(property = "asyncApiFilename", defaultValue = "asyncapi.json")
    String asyncApiFilename;

    @Parameter(property = "asyncApiDirectory", defaultValue = "target,generated")
    String asyncApiDirectory;

    @Parameter(property = "packageName", defaultValue = "id.global.amqp.models")
    String packageName;

    @Parameter(property = "modelVersion", required = true)
    String modelVersion;

    @Parameter(property = "modelName", required = true)
    String modelName;

    @Parameter(property = "skip", defaultValue = "false")
    boolean skip = false;

    private final Path tmpFolder = Paths.get("models");
    private final Path tmpSourceFolder = tmpFolder.resolve(Paths.get("src", "main", "java"));
    private final Path tmpSchemaFolder = tmpFolder.resolve(Paths.get("schemas"));

    private final String POM_XML = "pom.xml";
    private final String POM_TEMPLATE_XML = "pom-template.xml";
    private final String SUBSCRIBE = "subscribe";
    private final String PUBLISH = "publish";

    private final HashMap<String, List<String>> mainEventsNames = new HashMap<>();

    private URI baseDir;

    private final HashMap<String, ChannelDetails> channelDetailsList = new HashMap<>();

    public void execute() throws MojoExecutionException {
        modelName = getCleanModelName(modelName);

        if (skip) {
            getLog().info("Skipping model generation! Skip: [" + skip + "]");
            return;
        }
        if (project.getBasedir() != null) {
            baseDir = project.getBasedir().toURI();
        } else {
            try {
                baseDir = new URI("file:" + System.getProperty("user.dir"));
            } catch (URISyntaxException e) {
                throw new MojoExecutionException("Something is wrong with folder structure!");
            }
        }

        cleanUpDirectories(); //always try to do cleanup

        if (artifactSource == ArtifactSource.FILE) {
            generateFromFile();
        } else if (artifactSource == ArtifactSource.APICURIO) {
            generateFromApiCurio(apicurioUrl, modelName, modelVersion);
        } else {
            throw new MojoExecutionException("Execute failed! Artifact source location not known!");
        }

        getLog().info("Models generated successfully!");
    }

    private String readFile(final Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            getLog().error("Reading from file failed!", e);
            throw new RuntimeException(e);
        }
    }

    private void cleanUpDirectories() {
        try {
            deleteDirectoryRecursively(Path.of(baseDir).resolve(tmpFolder));
        } catch (IOException e) {
            getLog().error("Directory cleanup failed!", e);
            throw new RuntimeException(e);
        }
    }

    private  void deleteDirectoryRecursively(final Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk
                    .sorted(Comparator.reverseOrder())
                    .forEach(this::deleteDirectory);
            }
        }
    }

    private  void deleteDirectory(final Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            System.err.printf("Unable to delete this path : %s%n%s", path, e);
            throw new RuntimeException(e);
        }
    }

    private String readResourceFileContent(final String fileName) {
        String text;
        try {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                assert is != null;
                text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            return text;
        } catch (IOException e) {
            getLog().error("Cannot read resource file content!", e);
            throw new RuntimeException(e);
        }
    }

    private String readContentFromWeb(final String contentUrl) {
        try {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createBaseSchemaFiles(final String fileName, final String content) {

        Path path = Paths.get(baseDir)
                .resolve(tmpSchemaFolder)
                .resolve(fileName);

        replaceAndSaveSchemaFiles(content, path);

    }

    private void createEventSchemaFiles(final List<String> prefix, final String fileName, final String content) {
        prefix.forEach(single -> {
            Path path = Paths.get(baseDir)
                    .resolve(tmpSchemaFolder)
                    .resolve(single)
                    .resolve(fileName);

            replaceAndSaveSchemaFiles(content, path);
        });
    }

    private void replaceAndSaveSchemaFiles(final String content, final Path path) {
        Path pa = Paths.get(baseDir).resolve(tmpSchemaFolder);

        String contentReplaces = content
                .replace("#", "")
                .replace("/components/schemas/", pa.toUri().toString());

        try {
            Files.writeString(path, contentReplaces);
        } catch (IOException e) {
            getLog().error("Failed to write schema file", e);
            throw new RuntimeException(e);
        }
    }

    private void writeFile(final String fileName, final String content, final String path) {
        Path clientPath = Paths.get(path);
        try {
            Files.createDirectories(clientPath);
        } catch (IOException e) {
            getLog().error("Failed to create directories", e);
            throw new RuntimeException(e);
        }
        Path filePath = Paths.get(path, fileName);
        writeFile(filePath, content);

    }

    private void writeFile(Path path, String content) {
        try {
            Files.writeString(path, content);
        } catch (Exception e) {
            getLog().error("Failed to write file", e);
            throw new RuntimeException(e);
        }
    }

    private void createSchemaFiles(final JsonNode schemas, final HashMap<String, List<String>> exclude) {
        getLog().info("Creating JsonSchema files for model generator!");
        schemas.fields()
                .forEachRemaining((k) -> {
                    if (exclude.get(k.getKey()) == null) {
                        createBaseSchemaFiles(k.getKey(), k.getValue().toString());
                    } else {
                        createEventSchemaFiles(exclude.get(k.getKey()), k.getKey(), k.getValue().toString());
                    }
                });
    }

    private SchemaMapper createSchemaMapper(final GenerationConfig config, final Jackson2Annotator annotator) {
        return new SchemaMapper(
                new RuleFactory(
                        config,
                        annotator,
                        new SchemaStore()),
                new SchemaGenerator());
    }

    private void generate(final String fileName) {

        List<String> locationForGeneratedClasses = Optional.ofNullable(mainEventsNames.get(fileName))
                .orElse(List.of(""));

        Path clientPath = Paths.get(baseDir).resolve(tmpSourceFolder);
        try {
            Files.createDirectories(clientPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        locationForGeneratedClasses.forEach(locationForGeneratedClass -> {

            String packageNameGenerated = Stream.of(packageName, modelName, locationForGeneratedClass)
                    .filter(s -> !s.isBlank())
                    .collect(joining("."));

            Jackson2Annotator annotator = new Jackson2Annotator(EventSchemaGeneratorConfig.classConfig);

            if (!locationForGeneratedClass.isBlank()) {
                Optional<ChannelDetails> ch = Optional.of(
                        channelDetailsList.entrySet().stream()
                                .filter(entry -> entry.getValue().getContainedSections() != ChannelSectionTypes.NONE)
                                .filter(entry -> entry.getValue().publishEventName().equalsIgnoreCase(fileName)
                                        || entry.getValue().subscribeEventName().equalsIgnoreCase(fileName))
                                .findFirst()
                                .orElseThrow().getValue()
                );

                annotator = new MetadataAnnotator(ch.orElseThrow().node(), EventSchemaGeneratorConfig.eventConfig);
            }

            SchemaMapper mapper = createSchemaMapper(
                    locationForGeneratedClass.isBlank() ?
                            EventSchemaGeneratorConfig.classConfig :
                            EventSchemaGeneratorConfig.eventConfig,
                    annotator);

            Path schemaLocation = Optional.of(locationForGeneratedClass)
                    .filter(m -> !m.isBlank())
                    .map(m -> Paths.get(baseDir).resolve(tmpSchemaFolder).resolve(m).resolve(fileName))
                    .orElseGet(() -> Paths.get(baseDir).resolve(tmpSchemaFolder).resolve(fileName));

            JCodeModel codeModel = new JCodeModel();

            try {
                mapper.generate(codeModel, fileName, packageNameGenerated, readFile(schemaLocation));
                codeModel.build(clientPath.toFile());
            } catch (IOException e) {
                getLog().error("There was an error in Mapper or CodeModel.", e);
                throw new RuntimeException(e);
            }
        });
    }

    private void generatePom() {
        Path pomPath = Paths.get(baseDir).resolve(tmpFolder);
        writeFile(POM_XML, preparePomTemplate(POM_TEMPLATE_XML), pomPath.toString());

    }

    private String preparePomTemplate(final String templateFileName) {
        String pomTemplate = readResourceFileContent(templateFileName);
        Map<String, String> values = new HashMap<>();
        values.put("APPLICATION_NAME", modelName);
        values.put("APPLICATION_VERSION", modelVersion);
        return StringSubstitutor.replace(pomTemplate, values, "${", "}");

    }

    private List<String> filterList(final List<String> lst) {
        List<String> uniq = new ArrayList<>(lst);
        lst.forEach(elem -> uniq.removeIf(x -> !x.equals(elem) && elem.contains(x)));
        return uniq;
    }

    private String getCleanModelName(final String modelName) {
        return modelName.toLowerCase().replace("-", "_");
    }

    private Path resolveFilePath() {
        String[] pathSpliced = asyncApiDirectory.split(","); //separate directory via , to be platform independent

        Path path = Paths.get(baseDir).resolve(
                Arrays.stream(pathSpliced)
                        .collect(joining(FileSystems.getDefault().getSeparator())));

        return path.resolve(asyncApiFilename);
    }

    private void populateChannelList(Map.Entry<String, JsonNode> entry) {
        String subscribeEventName = Optional.ofNullable(entry.getValue()
                .path("subscribe")
                .path("message")
                .path("name")
                .textValue()
        ).orElse("");

        String publishEventName = Optional.ofNullable(entry.getValue()
                .path("publish")
                .path("message")
                .path("name")
                .textValue()
        ).orElse("");

        channelDetailsList.put(entry.getKey(),
                new ChannelDetails(entry.getKey(),
                        subscribeEventName,
                        publishEventName,
                        getSectionFromName(subscribeEventName, publishEventName),
                        entry.getValue()));
    }

    private ChannelSectionTypes getSectionFromName(String subscribeEventName, String publishEventName) {

        if (subscribeEventName.isBlank() && publishEventName.isBlank()) {
            return ChannelSectionTypes.NONE;
        }

        if (!subscribeEventName.isBlank() && !publishEventName.isBlank()) {
            return ChannelSectionTypes.BOTH;
        }
        if (!subscribeEventName.isBlank()) {
            return ChannelSectionTypes.SUBSCRIBE;
        }
        if (!publishEventName.isBlank()) {
            return ChannelSectionTypes.PUBLISH;
        }
        return ChannelSectionTypes.NONE;
    }

    private void rewriteRefsInSchemaFiles(Path schemaPath) throws IOException {

        List<File> files = Files.list(schemaPath)
                .map(Path::toFile)
                .collect(Collectors.toList());

        HashMap<String, String> classesSchemas = new HashMap<>();
        HashMap<String, String> enumSchemas = new HashMap<>();

        files.stream()
                .filter(File::isFile)
                .forEach(file -> {
                    String fileContent = readFile(Path.of(file.toURI()));
                    if (fileContent.contains("\"enum\"")) {
                        enumSchemas.put(file.getName(), file.getAbsolutePath());
                    } else {
                        classesSchemas.put(file.getName(), file.getAbsolutePath());
                    }
                });

        List<File> publish = getFileListInPath(schemaPath, PUBLISH);

        List<File> subscribe = getFileListInPath(schemaPath, SUBSCRIBE);

        // NOTE: 2. 10. 21 This fixes schemas that are not events for publishing
        Stream.concat(publish.stream(), subscribe.stream())
                .forEach(file -> {
                    String fileContent = readFile(Path.of(file.toURI()));
                    Stream.concat(enumSchemas.entrySet().stream(), classesSchemas.entrySet().stream())
                            .forEach((entry -> {
                                if (fileContent.contains(entry.getValue())) {
                                    replaceFileContent(file, fileContent, entry);
                                }
                            }));
                });

        // NOTE: 2. 10. 21 This fixes schemas that are not events for publishing
        List<File> baseFilesToFix = Files.list(schemaPath)
                .map(Path::toFile)
                .filter(File::isFile)
                .collect(Collectors.toList());

        // TODO: 3. 10. 21 Fix/refactor this
        baseFilesToFix.forEach(file -> {
            String fileContent = readFile(Path.of(file.toURI()));

            Pattern p = Pattern.compile("\"\\$ref.*?}"); //(\"\$ref(.*?)(\,))(?!\w)
            while (fileContent.contains("$ref")) {
                Matcher m = p.matcher(fileContent);

                String toReplace = "";
                if (m.find()) {
                    toReplace = m.group(0);
                }
                String name = Arrays.stream(toReplace.split("/"))
                        .reduce((first, second) -> second).orElse("")
                        .replaceAll("\"", "")
                        .replaceAll("}", "");

                fileContent = fileContent.replace(toReplace, replacementForRef(name) + "}"); //need to add } at the end

            }

            try {
                Files.writeString(Path.of(file.toURI()), fileContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

    private void replaceFileContent(File file, String fileContent, Map.Entry<String, String> entry) {
        String toReplace = refToBeReplaces(entry.getValue());
        String replaceWith = replacementForRef(entry.getKey());
        String replacementContent = fileContent.replace(toReplace, replaceWith);
        writeFile(Path.of(file.toURI()), replacementContent);
    }

    private List<File> getFileListInPath(Path schemaPath, String subscribe2) throws IOException {
        return Files.list(schemaPath.resolve(subscribe2))
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    private void manipulateAndGenerateFromJson(final String json) {

        try {
            getLog().info("Parsing AsyncApi definition!");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            JsonNode components = node.get("components");
            JsonNode schemas = components.get("schemas");
            JsonNode channels = node.get("channels");

            channels.fields().forEachRemaining(this::populateChannelList);

            //create directory structure
            Path pathSource = Paths.get(baseDir).resolve(tmpSourceFolder);
            Path pathSchema = Paths.get(baseDir).resolve(tmpSchemaFolder);
            Files.createDirectories(pathSource);
            Files.createDirectories(pathSchema);

            Files.createDirectories(pathSchema.resolve("subscribe"));
            Files.createDirectories(pathSchema.resolve("publish"));

            channelDetailsList.forEach((k, v) -> {
                switch (v.getContainedSections()) {
                    case BOTH -> mainEventsNames.put(v.publishEventName(), List.of(PUBLISH, SUBSCRIBE));
                    case PUBLISH -> mainEventsNames.put(v.publishEventName(), List.of(PUBLISH));
                    case SUBSCRIBE -> mainEventsNames.put(v.subscribeEventName(), List.of(SUBSCRIBE));
                }
            });

            createSchemaFiles(schemas, mainEventsNames);
            rewriteRefsInSchemaFiles(pathSchema);

            // NOTE: 2. 10. 21 This generates producer/consumer event classes
            mainEventsNames.keySet().forEach(this::generate);

            List<String> baseClasses = new ArrayList<>();
            schemas.fieldNames().forEachRemaining(name -> {
                if (mainEventsNames.get(name) == null) { //exclude main event
                    baseClasses.add(name);
                }
            });

            Map<String, String> dependencyMap = new HashMap<>();

            /* dependency count for each file */
            List<AbstractMap.SimpleEntry<String, Integer>> dependencyCount = baseClasses.stream()
                    .map(name -> {
                        String content = readFile(Paths.get(baseDir).resolve(tmpSchemaFolder).resolve(name));
                        JsonNode myNode;
                        try {
                            myNode = mapper.readTree(content);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        addKeys(name, myNode, dependencyMap);
                        int count = StringUtils.countMatches(content, "javaType");
                        return new AbstractMap.SimpleEntry<>(name, count);
                    })
                    .collect(Collectors.toList());

            // NOTE: 2. 10. 21 builds clean dependency map
            Map<String, List<String>> modelWithDependencies = new HashMap<>();
            dependencyMap.forEach((key, value) -> {
                String main = Arrays.stream(key.split("\\.")).reduce((first, second) -> first)
                        .orElse("");
                String dep = Arrays.stream(value.split("\\.")).reduce((first, second) -> second)
                        .orElse("");

                List<String> vals = Optional.ofNullable(modelWithDependencies.get(main)).orElseGet(ArrayList::new);
                vals.add(dep);
                modelWithDependencies.put(main, vals);

            });

            List<String> modelsWithoutDependencies = dependencyCount.stream()
                    .filter(entry -> entry.getValue() == 0)
                    .map(AbstractMap.SimpleEntry::getKey)
                    .collect(Collectors.toList());

            // NOTE: 3. 10. 21 This generates base classes
            dependencyCount.stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        generate(entry.getKey());
                        var vv = modelWithDependencies.get(entry.getKey());
                        if (vv != null && !vv.isEmpty())
                            vv.forEach(this::generate);

                    });

            // TODO: 3. 10. 21 this works, but I need to revisit this
            List<String> l = GraphUtils.get(modelWithDependencies);

            List<String> orderedDependencyList = l.stream()
                    .sorted((x, y) -> Integer.compare(y.split(",").length, x.split(",").length))
                    .collect(Collectors.toCollection(ArrayList::new));

            // TODO: 3. 10. 21 Check if this needs multiple iterations of filtering create simple test for this filtering
            List<String> filtered = filterList(orderedDependencyList);

            List<String> alreadyGenerate = new ArrayList<>();

            filtered.forEach(stringList -> Arrays.stream(stringList.split(","))
                    .forEach(nam -> {

                        if (!alreadyGenerate.contains(nam)) {
                            generate(nam);
                            alreadyGenerate.add(nam);
                        }

                    }));

            modelsWithoutDependencies
                    .forEach(this::generate);

            generatePom();
        } catch (Exception e) {
            getLog().error("Manipulation of json schema or generation of Java classes fail!", e);
            throw new RuntimeException(e);
        }
    }

    private String replacementForRef(String name) {
        return "\"type\" : \"object\"" + "," + "\"javaType\" : \"id.global.amqp.models." + modelName + "." + name + "\"";
    }

    private String refToBeReplaces(String value) {
        return "\"$ref\":\"file://" + value + "\"";
    }

    private void addKeys(final String currentPath, final JsonNode jsonNode, Map<String, String> map) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                addKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (int i = 0; i < arrayNode.size(); i++) {
                addKeys(currentPath + "[" + i + "]", arrayNode.get(i), map);
            }
        } else if (jsonNode.isValueNode()) {
            ValueNode valueNode = (ValueNode) jsonNode;
            if (currentPath.contains("javaType"))
                map.put(currentPath, valueNode.asText());
        }
    }

    private void generateFromFile() {

        Optional.of(resolveFilePath())
                .map(this::readFile)
                .ifPresent(this::manipulateAndGenerateFromJson);

    }

    private void generateFromApiCurio(final String baseUrl, String serviceName, String artifactVersion) {

        String apicurioUrl = Optional.of(baseUrl)
                .filter(Predicate.not(String::isBlank))
                .orElse(defaultUrl);

        Optional.of(serviceName)
                .map(artifactName -> readContentFromWeb(
                        apicurioUrl + "/api/artifacts/" + artifactName + ":" + artifactVersion + ":" + "json"))
                .filter(content -> !content.isBlank())
                .ifPresent(this::manipulateAndGenerateFromJson);
    }

}
