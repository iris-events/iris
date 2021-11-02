package id.global.plugin.model.generator;

import static id.global.plugin.model.generator.utils.StringConstants.COMMA;
import static id.global.plugin.model.generator.utils.StringConstants.COMPONENTS_SCHEMAS;
import static id.global.plugin.model.generator.utils.StringConstants.DOT;
import static id.global.plugin.model.generator.utils.StringConstants.DOT_REGEX;
import static id.global.plugin.model.generator.utils.StringConstants.EMPTY_STRING;
import static id.global.plugin.model.generator.utils.StringConstants.FORWARD_SLASH;
import static id.global.plugin.model.generator.utils.StringConstants.HASH;
import static id.global.plugin.model.generator.utils.StringConstants.JAVA_TYPE;
import static id.global.plugin.model.generator.utils.StringConstants.POM_TEMPLATE_XML;
import static id.global.plugin.model.generator.utils.StringConstants.POM_XML;
import static id.global.plugin.model.generator.utils.StringConstants.PUBLISH;
import static id.global.plugin.model.generator.utils.StringConstants.REF;
import static id.global.plugin.model.generator.utils.StringConstants.REF_REGEX;
import static id.global.plugin.model.generator.utils.StringConstants.SUBSCRIBE;
import static id.global.plugin.model.generator.utils.StringReplacement.getRefToBeReplaced;
import static id.global.plugin.model.generator.utils.StringReplacement.getReplacementForRef;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
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
import id.global.plugin.model.generator.models.JsonSchemaWrapper;
import id.global.plugin.model.generator.utils.FileInteractor;
import id.global.plugin.model.generator.utils.PathResolver;

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

    @SuppressWarnings("unused") //skip is assigned via system property e.g. -Dskip=true
    @Parameter(property = "skip", defaultValue = "false")
    boolean skip;

    private Log log;

    private final PathResolver pathResolver = new PathResolver();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileInteractor fileInteractor = new FileInteractor(pathResolver);

    private final Pattern REF_PATTERN = Pattern.compile(REF_REGEX);

    private final BinaryOperator<String> first = (first, second) -> first;
    private final BinaryOperator<String> last = (first, second) -> second;

    private final Map<String, List<String>> mainEventsNames = new HashMap<>();
    private final Map<String, ChannelDetails> channelDetails = new HashMap<>();

    public void execute() throws MojoExecutionException {
        this.log = getLog();

        if (skip) {
            log.info("Skipping model generation as skip flag is set to [true]");
            return;
        }

        modelName = getCleanModelName(modelName);

        fileInteractor.cleanUpDirectories(pathResolver.getWorkingDirectory()); //always try to do cleanup

        if (artifactSource == ArtifactSource.FILE) {
            generateFromFile();
        } else if (artifactSource == ArtifactSource.APICURIO) {
            generateFromApiCurio(apicurioUrl, modelName, modelVersion);
        } else {
            throw new MojoExecutionException("Execute failed! Artifact source location not known!");
        }

        log.info("Models generated successfully!");
    }

    private void createBaseSchemaFile(final String fileName, final String content) {

        Path path = pathResolver.getSchemasDirectory()
                .resolve(fileName);

        replaceAndSaveSchemaFiles(content, path);

    }

    private void createEventSchemaFile(final List<String> prefix, final String fileName, final String content) {
        prefix.forEach(single -> {
            Path path = pathResolver.getSchemasDirectory()
                    .resolve(single)
                    .resolve(fileName);

            replaceAndSaveSchemaFiles(content, path);
        });
    }

    private void replaceAndSaveSchemaFiles(final String content, final Path path) {
        Path schemaPath = pathResolver.getSchemasDirectory();

        String contentReplaces = content
                .replace(HASH, EMPTY_STRING)
                .replace(COMPONENTS_SCHEMAS, schemaPath.toUri().toString());

        fileInteractor.writeFile(path, contentReplaces);
    }

    private void createSchemaFiles(final JsonNode schemas, final Map<String, List<String>> exclude) {
        log.info("Creating JsonSchema files for model generator!");
        schemas.fields()
                .forEachRemaining((schema) -> {
                    var schemaName = schema.getKey();
                    var schemaContent = schema.getValue().toString();
                    createSchemaFileFromContent(exclude, schemaName, schemaContent);
                });
    }

    private void createSchemaFileFromContent(final Map<String, List<String>> exclude, final String schemaName, final String schemaContent) {
        var optionalExclude = Optional.ofNullable(exclude.get(schemaName));

        optionalExclude.ifPresentOrElse(
                exclusions -> createEventSchemaFile(exclusions, schemaName, schemaContent),
                () -> createBaseSchemaFile(schemaName, schemaContent));
    }

    private SchemaMapper createSchemaMapper(final GenerationConfig config, final Jackson2Annotator annotator) {
        return new SchemaMapper(
                new RuleFactory(config, annotator, new SchemaStore()),
                new SchemaGenerator()
        );
    }

    private void generate(final String fileName) {

        List<String> locationForGeneratedClasses = Optional.ofNullable(mainEventsNames.get(fileName))
                .orElse(List.of(EMPTY_STRING));

        locationForGeneratedClasses.forEach(locationForGeneratedClass -> {
            var classPackageName = generatePackageName(locationForGeneratedClass);
            var annotator = getJackson2Annotator(fileName, locationForGeneratedClass);
            var config = getConfigurationByLocation(locationForGeneratedClass);
            var schemaMapper = createSchemaMapper(config, annotator);
            var schemaPath = getSchemaFilePath(fileName, locationForGeneratedClass);

            try {
                var schemaContent = fileInteractor.readFile(schemaPath);
                var codeModel = new JCodeModel();
                var destinationDirectory =  pathResolver.getSourceDirectory().toFile();
                schemaMapper.generate(codeModel, fileName, classPackageName, schemaContent);
                codeModel.build(destinationDirectory);
            } catch (IOException e) {
                log.error("There was an error in Mapper or CodeModel.", e);
                throw new RuntimeException(e);
            }
        });
    }

    private Path getSchemaFilePath(String fileName, String locationForGeneratedClass) {
        return Optional.of(locationForGeneratedClass)
                .filter(m -> !m.isBlank())
                .map(m -> pathResolver.getSchemasDirectory().resolve(m).resolve(fileName))
                .orElseGet(() -> pathResolver.getSchemasDirectory().resolve(fileName));
    }

    private GenerationConfig getConfigurationByLocation(String locationForGeneratedClass) {
        return locationForGeneratedClass.isBlank() ?
                EventSchemaGeneratorConfig.classConfig :
                EventSchemaGeneratorConfig.eventConfig;
    }

    private Jackson2Annotator getJackson2Annotator(String fileName, String locationForGeneratedClass) {
        Jackson2Annotator annotator = new Jackson2Annotator(EventSchemaGeneratorConfig.classConfig);

        if (!locationForGeneratedClass.isBlank()) {
            Optional<ChannelDetails> ch = Optional.of(
                    channelDetails.entrySet().stream()
                            .filter(nonEmptyChannelSection())
                            .filter(hasMainEvent(fileName))
                            .findFirst()
                            .orElseThrow()
                            .getValue()
            );

            annotator = new MetadataAnnotator(ch.orElseThrow().node(), EventSchemaGeneratorConfig.eventConfig);
        }
        return annotator;
    }

    private Predicate<Map.Entry<String, ChannelDetails>> hasMainEvent(String fileName) {
        return entry -> entry.getValue().publishEventName().equalsIgnoreCase(fileName)
                        || entry.getValue().subscribeEventName().equalsIgnoreCase(fileName);
    }

    private Predicate<Map.Entry<String, ChannelDetails>> nonEmptyChannelSection() {
        return entry -> !entry.getValue().getSectionsForChannelEvent().getValue().isEmpty();
    }

    private String generatePackageName(String locationForGeneratedClass) {
        return Stream.of(packageName, modelName, locationForGeneratedClass)
                .filter(s -> !s.isBlank())
                .collect(joining(DOT));
    }

    private void generatePom() {
        var pomPath = pathResolver.getWorkingDirectory().resolve(POM_XML);
        var pomTemplate = preparePomTemplate();
        fileInteractor.writeFile(pomPath, pomTemplate);

    }

    private String preparePomTemplate() {
        var pomTemplate = fileInteractor.readResourceFileContent(POM_TEMPLATE_XML);
        return pomTemplate
                .replace("APPLICATION_NAME", modelName)
                .replace("APPLICATION_VERSION", modelVersion);

    }

    private List<String> sortAndFilterClassChains(final List<String> lst) {
        List<String> uniq = new ArrayList<>(lst);
        lst.stream()
                .sorted((classChainFirst, classChainSecond) ->
                        Integer.compare(classChainSecond.split(COMMA).length,
                                classChainFirst.split(COMMA).length)
                )
                .collect(Collectors.toCollection(ArrayList::new))
                .forEach(elem -> uniq.removeIf(x -> !x.equals(elem) && elem.contains(x)));
        return uniq;
    }

    private String getCleanModelName(final String modelName) {
        return modelName.toLowerCase().replace("-", "_");
    }

    private Path resolveAsyncApiFilePath() {
        String[] pathSpliced = asyncApiDirectory.split(COMMA); //separate directory via , to be platform independent

        String asyncApiFileLocation = Arrays.stream(pathSpliced)
                .collect(joining(FileSystems.getDefault().getSeparator()));

        Path asyncApiPath = pathResolver.getRootDirectory().resolve(asyncApiFileLocation);

        return asyncApiPath.resolve(asyncApiFilename);
    }

    private void prepareChannelDetails(Map.Entry<String, JsonNode> entry) {

        String channelName = entry.getKey();
        JsonNode channelDetailsNode = entry.getValue();

        String subscribeEventName = getEventNameFromDetails(channelDetailsNode, SUBSCRIBE);
        String publishEventName = getEventNameFromDetails(channelDetailsNode, PUBLISH);

        this.channelDetails.put(channelName,
                new ChannelDetails(
                        channelName,
                        subscribeEventName,
                        publishEventName,
                        channelDetailsNode));
    }

    private String getEventNameFromDetails(JsonNode channelDetailsNode, String channelType) {
        String possibleEventName = channelDetailsNode
                .path(channelType)
                .path("message")
                .path("name")
                .textValue();

        return Optional.ofNullable(possibleEventName)
                .orElse(EMPTY_STRING);
    }

    private void rewriteRefsInSchemaFiles(Path schemasPath) {

        try {
            HashMap<String, String> schemaNamesToLocation = new HashMap<>();

            Files.list(schemasPath)
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(file -> schemaNamesToLocation.put(file.getName(), file.getAbsolutePath()));

            Stream<File> publishFiles = getFileListInPath(schemasPath, PUBLISH).stream();
            Stream<File> subscribeFiles = getFileListInPath(schemasPath, SUBSCRIBE).stream();

            //rewrite event schemas
            Stream.concat(publishFiles, subscribeFiles)
                    .forEach(file -> {
                        String fileContent = fileInteractor.readFile(Path.of(file.toURI()));
                        schemaNamesToLocation.entrySet().stream()
                                .filter(entry -> fileContent.contains(entry.getValue()))
                                .forEach(entry -> replaceFileContent(file, fileContent, entry));
                    });

            //rewrite base schemas
            Files.list(schemasPath)
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(this::replaceAllRefs);

        } catch (IOException e) {
            log.error("Problem rewriting refs in schema files!");
            throw new RuntimeException(e);
        }
    }

    private void replaceAllRefs(File file) {
        String fileContent = fileInteractor.readFile(Path.of(file.toURI()));
        while (fileContent.contains(REF)) {
            Matcher matcher = REF_PATTERN.matcher(fileContent);

            if (matcher.find()) {
                var toReplace = matcher.group(0);
                var name = Arrays.stream(matcher.group(3).split(FORWARD_SLASH))
                        .reduce((first, second) -> second).orElse(EMPTY_STRING);
                fileContent = fileContent.replace(toReplace, getReplacementForRef(name, packageName, modelName));
            }
        }
        fileInteractor.writeFile(Path.of(file.toURI()), getFormattedJson(fileContent));
    }

    private String getFormattedJson(String content) {
        var node = getJsonNodeFromString(content);
        return node.toPrettyString();
    }

    private void replaceFileContent(File file, String fileContent, Map.Entry<String, String> nameWithLocation) {
        String fileName = nameWithLocation.getKey();
        String replaceWith = getReplacementForRef(fileName, packageName, modelName);

        String fileLocation = nameWithLocation.getValue();
        String toReplace = getRefToBeReplaced(fileLocation);

        String replacementContent = fileContent.replace(toReplace, replaceWith);
        fileInteractor.writeFile(Path.of(file.toURI()), getFormattedJson(replacementContent));
    }

    private List<File> getFileListInPath(Path schemaPath, String subFolder) throws IOException {
        return Files.list(schemaPath.resolve(subFolder))
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    private void manipulateAndGenerateFromJson(final String jsonContent) {

        log.info("Parsing AsyncApi definition!");

        var asyncApiJsonNode = getJsonNodeFromString(jsonContent);
        var channelsJsonNode = asyncApiJsonNode.get("channels");
        var componentsJsonNode = asyncApiJsonNode.get("components");
        var schemasJsonNode = componentsJsonNode.get("schemas");

        fileInteractor.initializeDirectories();

        //manipulation of schema files

        channelsJsonNode
                .fields()
                .forEachRemaining(this::prepareChannelDetails);

        prepareMainEventsFromChannels(channelDetails, mainEventsNames);

        createSchemaFiles(schemasJsonNode, mainEventsNames);

        rewriteRefsInSchemaFiles(pathResolver.getSchemasDirectory());

        //preparation of supporting structure for correct java classes generation

        List<String> baseClasses
                = extractBaseClasses(schemasJsonNode);

        List<JsonSchemaWrapper> baseClassesSchemasWithContent
                = extractSchemaContentForClasses(baseClasses);

        Map<String, String> javaTypeLocationToCanonicalName
                = extractJavaTypeLocationToCanonicalName(baseClassesSchemasWithContent);

        Map<String, List<String>> parentToChildClassesRelation
                = extractParentToChildRelationsStructure(javaTypeLocationToCanonicalName);

        List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount
                = extractParentToChildrenCount(baseClassesSchemasWithContent);

        List<String> classChains
                = GraphUtils.getClassChains(parentToChildClassesRelation);

        List<String> filteredClassChains
                = sortAndFilterClassChains(classChains);

        //generation of concrete classes

        generateJavaClasses(parentToChildClassesRelation, parentToChildCount, filteredClassChains);

    }

    private void generateJavaClasses(
            Map<String, List<String>> parentToChildClassesRelation,
            List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount,
            List<String> filteredClassChains) {

        List<String> alreadyGeneratedClasses = new ArrayList<>();

        generateMainEvents(mainEventsNames);

        generateChildClasses(parentToChildClassesRelation, parentToChildCount);

        generateChildClassesByChain(filteredClassChains, alreadyGeneratedClasses);

        generateLeafChildClasses(parentToChildCount);

        generatePom();
    }

    private void generateLeafChildClasses(List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount) {
        parentToChildCount.stream()
                .filter(entry -> entry.getValue() == 0)
                .map(AbstractMap.SimpleEntry::getKey)
                .forEach(this::generate);
    }

    private void generateChildClassesByChain(List<String> filteredClassChains, List<String> alreadyGeneratedClasses) {
        filteredClassChains.forEach(classChain ->
                Arrays.stream(classChain.split(COMMA))
                        .forEach(className -> skipOrGenerate(className, alreadyGeneratedClasses)));
    }

    private void generateChildClasses(Map<String, List<String>> parentToChildClassesRelation,
            List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount) {

        parentToChildCount.stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {

                    generate(entry.getKey());

                    List<String> childClasses = Optional.ofNullable(parentToChildClassesRelation.get(entry.getKey()))
                            .orElseGet(Collections::emptyList)
                            .stream()
                            .distinct()
                            .collect(Collectors.toList());

                    if (!childClasses.isEmpty())
                        childClasses.forEach(this::generate);
                });
    }

    private List<AbstractMap.SimpleEntry<String, Integer>> extractParentToChildrenCount(
            List<JsonSchemaWrapper> baseClassesSchemasWithContent) {
        return baseClassesSchemasWithContent.stream()
                .map(schemaWithContent -> {
                    var javaTypeCount = StringUtils.countMatches(schemaWithContent.getSchemaContent(), JAVA_TYPE);
                    return new AbstractMap.SimpleEntry<>(schemaWithContent.getClassName(), javaTypeCount);
                }).collect(Collectors.toList());
    }

    private Map<String, String> extractJavaTypeLocationToCanonicalName(List<JsonSchemaWrapper> baseClassesSchemasWithContent) {
        Map<String, String> javaTypeLocationToCanonicalName = new HashMap<>();

        baseClassesSchemasWithContent.forEach(schemaWithContent -> {
            var className = schemaWithContent.getClassName();
            var schemaNode = schemaWithContent.getSchemaNode();
            createPathToJavaTypeProperty(className, schemaNode, javaTypeLocationToCanonicalName);
        });

        return javaTypeLocationToCanonicalName;
    }

    private Map<String, List<String>> extractParentToChildRelationsStructure(
            Map<String, String> javaTypeLocationToCanonicalName) {
        Map<String, List<String>> parentToChildrenRelation = new HashMap<>();

        javaTypeLocationToCanonicalName.forEach((javaTypeLocation, canonicalName) -> {
            var parentClassName = getClassNameFromString(javaTypeLocation, first);
            var childClassName = getClassNameFromString(canonicalName, last);

            List<String> childClasses = Optional.ofNullable(parentToChildrenRelation.get(parentClassName))
                    .orElseGet(ArrayList::new);

            childClasses.add(childClassName);
            parentToChildrenRelation.put(parentClassName, childClasses);
        });

        return parentToChildrenRelation;
    }

    private String getClassNameFromString(String fullName, BinaryOperator<String> operator) {
        return Arrays.stream(fullName.split(DOT_REGEX))
                .reduce(operator)
                .orElse(EMPTY_STRING);
    }

    private List<JsonSchemaWrapper> extractSchemaContentForClasses(List<String> baseClasses) {
        return baseClasses.stream()
                .map(baseClassName -> {
                    var jsonString = fileInteractor.readFile(pathResolver.getSchemasDirectory().resolve(baseClassName));
                    var jsonSchemaNode = getJsonNodeFromString(jsonString);
                    return new JsonSchemaWrapper(baseClassName, jsonString, jsonSchemaNode);
                }).collect(Collectors.toList());
    }

    private List<String> extractBaseClasses(JsonNode schemas) {
        List<String> baseClasses = new ArrayList<>();
        schemas.fieldNames().forEachRemaining(schemaName -> {
            if (mainEventsNames.get(schemaName) == null) { //exclude main event
                baseClasses.add(schemaName);
            }
        });
        return baseClasses;
    }

    private void generateMainEvents(Map<String, List<String>> mainEvents) {
        mainEvents.keySet()
                .forEach(this::generate);
    }

    private void prepareMainEventsFromChannels(final Map<String, ChannelDetails> channelDetails,
            Map<String, List<String>> mainEventsNameList) {
        channelDetails.forEach((channel, channelDetail) -> {
            Map.Entry<String, List<String>> eventNameWithSectionsLocations = channelDetail.getSectionsForChannelEvent();
            if (!eventNameWithSectionsLocations.getKey().isBlank()) {
                mainEventsNameList.put(eventNameWithSectionsLocations.getKey(), eventNameWithSectionsLocations.getValue());
            }
        });
    }

    private void skipOrGenerate(String className, List<String> alreadyGeneratedClasses) {
        if (!alreadyGeneratedClasses.contains(className)) {
            generate(className);
            alreadyGeneratedClasses.add(className);
        }
    }

    private JsonNode getJsonNodeFromString(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("Failed to parse json string!", e);
            throw new RuntimeException(e);
        }
    }

    private void createPathToJavaTypeProperty(final String currentPath, final JsonNode schema,
            final Map<String, String> classDependencies) {
        if (schema.isObject()) {
            ObjectNode objectNode = (ObjectNode) schema;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? EMPTY_STRING : currentPath + DOT;

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                createPathToJavaTypeProperty(pathPrefix + entry.getKey(), entry.getValue(), classDependencies);
            }
        } else if (schema.isArray()) {
            ArrayNode arrayNode = (ArrayNode) schema;
            for (int i = 0; i < arrayNode.size(); i++) {
                createPathToJavaTypeProperty(currentPath + "[" + i + "]", arrayNode.get(i), classDependencies);
            }
        } else if (schema.isValueNode()) {
            ValueNode valueNode = (ValueNode) schema;
            if (currentPath.contains(JAVA_TYPE))
                classDependencies.put(currentPath, valueNode.asText());
        }
    }

    private void generateFromFile() {
        Optional.of(resolveAsyncApiFilePath())
                .map(fileInteractor::readFile)
                .ifPresent(this::manipulateAndGenerateFromJson);

    }

    private void generateFromApiCurio(final String baseUrl, String serviceName, String artifactVersion) {

        String apicurioUrl = Optional.of(baseUrl)
                .filter(Predicate.not(String::isBlank))
                .orElse(defaultUrl);

        Optional.of(serviceName)
                .map(artifactName -> fileInteractor.readContentFromWeb(
                        String.format("%s/api/artifacts/%s:%s:json", apicurioUrl, artifactName, artifactVersion)))
                .filter(content -> !content.isBlank())
                .ifPresent(this::manipulateAndGenerateFromJson);
    }

}
