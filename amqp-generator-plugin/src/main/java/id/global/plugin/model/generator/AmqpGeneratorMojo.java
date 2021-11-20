package id.global.plugin.model.generator;

import static id.global.plugin.model.generator.utils.StringConstants.COMMA;
import static id.global.plugin.model.generator.utils.StringConstants.DOT;
import static id.global.plugin.model.generator.utils.StringConstants.DOT_REGEX;
import static id.global.plugin.model.generator.utils.StringConstants.EMPTY_STRING;
import static id.global.plugin.model.generator.utils.StringConstants.FORWARD_SLASH;
import static id.global.plugin.model.generator.utils.StringConstants.JAVA_TYPE;
import static id.global.plugin.model.generator.utils.StringConstants.PAYLOAD;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import id.global.plugin.model.generator.utils.JsonUtils;
import id.global.plugin.model.generator.utils.PathResolver;
import id.global.plugin.model.generator.utils.SchemaFileGenerator;

@Mojo(name = "generate-amqp-models", defaultPhase = LifecyclePhase.COMPILE, requiresProject = false)
public class AmqpGeneratorMojo extends AbstractMojo {

    private static final String defaultUrl = "https://schema.internal.globalid.dev";
    public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    public static final String IS_GENERATED_EVENT = "isGeneratedEvent";

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

    private final Set<String> eventClassNames = new HashSet<>();
    private final Map<String, ChannelDetails> channelDetails = new HashMap<>();

    private SchemaFileGenerator schemaFileGenerator;
    private JsonUtils jsonUtils;

    public void execute() throws MojoExecutionException {
        this.log = getLog();

        if (skip) {
            log.info("Skipping model generation as skip flag is set to [true]");
            return;
        }

        this.schemaFileGenerator = new SchemaFileGenerator(this.log, this.pathResolver, this.fileInteractor, objectMapper);
        this.jsonUtils = new JsonUtils(this.objectMapper, this.log);

        modelName = getCleanModelName(modelName);
        fileInteractor.cleanUpDirectories(pathResolver.getWorkingDirectory());

        if (artifactSource == ArtifactSource.FILE) {
            generateFromFile();
        } else if (artifactSource == ArtifactSource.APICURIO) {
            generateFromApicurio(apicurioUrl, modelName, modelVersion);
        } else {
            throw new MojoExecutionException("Execute failed! Artifact source location not known!");
        }

        log.info("Models generated successfully!");
    }

    private void manipulateAndGenerateFromJson(final String jsonContent) {

        log.info("Parsing AsyncApi definition!");

        var asyncApiJsonNode = jsonUtils.getJsonNodeFromString(jsonContent);
        var channelsJsonNode = asyncApiJsonNode.get("channels");
        var componentsJsonNode = asyncApiJsonNode.get("components");
        var schemasJsonNode = removeGeneratedClassSchemas(componentsJsonNode.get("schemas"));

        fileInteractor.initializeDirectories();

        channelDetails.putAll(createChannelDetailsMap(channelsJsonNode));
        eventClassNames.addAll(findEventClassNames(channelDetails, schemasJsonNode));

        schemaFileGenerator.createSchemaFiles(schemasJsonNode, eventClassNames);

        rewriteRefsInSchemaFiles(pathResolver.getSchemasDirectory());

        //preparation of supporting structure for correct java classes generation

        List<String> baseClasses = extractBaseClasses(schemasJsonNode);

        List<JsonSchemaWrapper> baseClassesSchemasWithContent = extractSchemaContentForClasses(baseClasses);

        Map<String, String> javaTypeLocationToCanonicalName = extractJavaTypeLocationToCanonicalName(
                baseClassesSchemasWithContent);

        Map<String, List<String>> parentToChildClassesRelation
                = extractParentToChildRelationsStructure(javaTypeLocationToCanonicalName);

        List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount
                = extractParentToChildrenCount(baseClassesSchemasWithContent);

        List<String> classChains = GraphUtils.getClassChains(parentToChildClassesRelation);

        List<String> filteredClassChains = sortAndFilterClassChains(classChains);

        generateJavaClasses(parentToChildClassesRelation, parentToChildCount, filteredClassChains);
    }

    private Set<String> findEventClassNames(final Map<String, ChannelDetails> channelDetails,
            JsonNode schemasJsonNode) {
        var eventClassNames = new HashSet<String>();

        channelDetails.forEach((channel, channelDetail) -> {
            String eventName = getEventNameFromChannelDetails(channelDetail);
            if (schemasJsonNode.has(eventName)) {
                eventClassNames.add(eventName);
            }
        });

        return eventClassNames;
    }

    private SchemaMapper createSchemaMapper(final GenerationConfig config, final Jackson2Annotator annotator) {
        return new SchemaMapper(
                new RuleFactory(config, annotator, new SchemaStore()),
                new SchemaGenerator()
        );
    }

    private void generate(final String fileName, String subdirectory) {
        var classPackageName = generatePackageName(subdirectory);
        var annotator = getJackson2Annotator(fileName, eventClassNames);
        var config = getConfigurationByLocation(subdirectory);
        var schemaMapper = createSchemaMapper(config, annotator);
        var schemaPath = getSchemaFilePath(fileName, subdirectory);

        try {
            var schemaContent = fileInteractor.readFile(schemaPath);
            var codeModel = new JCodeModel();
            var destinationDirectory = pathResolver.getSourceDirectory().toFile();
            schemaMapper.generate(codeModel, fileName, classPackageName, schemaContent);
            codeModel.build(destinationDirectory);
        } catch (IOException e) {
            log.error("There was an error in Mapper or CodeModel.", e);
            throw new RuntimeException(e);
        }
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

    private Jackson2Annotator getJackson2Annotator(String fileName, Set<String> mainEventNames) {
        Jackson2Annotator annotator = new Jackson2Annotator(EventSchemaGeneratorConfig.classConfig);

        if (mainEventNames.contains(fileName)) {
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

    private Map<String, ChannelDetails> createChannelDetailsMap(JsonNode channelsJsonNode) {
        Map<String, ChannelDetails> channelDetailsMap = new HashMap<>();

        channelsJsonNode.fields().forEachRemaining(entry -> {
            String channelName = entry.getKey();
            JsonNode channelDetailsNode = entry.getValue();

            String subscribeEventName = getEventNameFromDetails(channelDetailsNode, SUBSCRIBE);
            String publishEventName = getEventNameFromDetails(channelDetailsNode, PUBLISH);

            channelDetailsMap.put(channelName,
                    new ChannelDetails(
                            channelName,
                            subscribeEventName,
                            publishEventName,
                            channelDetailsNode));
        });

        return channelDetailsMap;
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

    private String getEventNameFromChannelDetails(ChannelDetails channelDetails) {
        String eventName = channelDetails.subscribeEventName();
        if (eventName == null || eventName.isBlank()) {
            eventName = channelDetails.publishEventName();
        }
        return eventName;
    }

    private void rewriteRefsInSchemaFiles(Path schemasPath) {

        try {
            HashMap<String, String> schemaNamesToLocation = new HashMap<>();

            Files.list(schemasPath.resolve(PAYLOAD))
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(file -> schemaNamesToLocation.put(file.getName(), file.getAbsolutePath()));

            Files.list(schemasPath)
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (file.isFile()) {
                            String fileContent = fileInteractor.readFile(Path.of(file.toURI()));
                            schemaNamesToLocation.entrySet().stream()
                                    .filter(entry -> fileContent.contains(entry.getValue()))
                                    .forEach(entry -> replaceFileContent(file, fileContent, entry));
                        }
                    });

            //rewrite base schemas
            Stream.concat(Files.list(schemasPath), Files.list(schemasPath.resolve(PAYLOAD)))
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
                var name = Arrays.stream(matcher.group(2).split(FORWARD_SLASH))
                        .reduce((first, second) -> second).orElse(EMPTY_STRING);
                fileContent = fileContent.replace(toReplace, getReplacementForRef(name, packageName, modelName));
            }
        }
        fileInteractor.writeFile(Path.of(file.toURI()), jsonUtils.getFormattedJson(fileContent));
    }

    private void replaceFileContent(File file, String fileContent, Map.Entry<String, String> nameWithLocation) {
        String fileName = nameWithLocation.getKey();
        String replaceWith = getReplacementForRef(fileName, packageName, modelName);

        String fileLocation = nameWithLocation.getValue();
        String toReplace = getRefToBeReplaced(fileLocation);

        String replacementContent = fileContent.replace(toReplace, replaceWith);
        fileInteractor.writeFile(Path.of(file.toURI()), jsonUtils.getFormattedJson(replacementContent));
    }

    private JsonNode removeGeneratedClassSchemas(JsonNode schemas) {
        ObjectNode filteredSchemas = objectMapper.createObjectNode();

        if (schemas instanceof ObjectNode) {
            schemas.fields()
                    .forEachRemaining((schema) -> {
                        if (!schema.getValue().has(ADDITIONAL_PROPERTIES) ||
                                !schema.getValue().get(ADDITIONAL_PROPERTIES).has(IS_GENERATED_EVENT) ||
                                !schema.getValue().get(ADDITIONAL_PROPERTIES).get(IS_GENERATED_EVENT).booleanValue()) {
                            filteredSchemas.set(schema.getKey(), schema.getValue());
                        }
                    });
        }
        return filteredSchemas;
    }

    private void generateJavaClasses(Map<String, List<String>> parentToChildClassesRelation,
            List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount,
            List<String> filteredClassChains) {

        List<String> alreadyGeneratedClasses = new ArrayList<>();

        generateMainEvents(eventClassNames);

        generateChildClasses(parentToChildClassesRelation, parentToChildCount);

        generateChildClassesByChain(filteredClassChains, alreadyGeneratedClasses);

        generateLeafChildClasses(parentToChildCount);

        generatePom();
    }

    private void generateLeafChildClasses(List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount) {
        parentToChildCount.stream()
                .filter(entry -> entry.getValue() == 0)
                .map(AbstractMap.SimpleEntry::getKey)
                .forEach(entry -> {
                    generate(entry, PAYLOAD);
                });
    }

    private void generateChildClassesByChain(List<String> filteredClassChains, List<String> alreadyGeneratedClasses) {
        filteredClassChains.forEach(classChain ->
                Arrays.stream(classChain.split(COMMA))
                        .forEach(className -> {
                            if (!alreadyGeneratedClasses.contains(className)) {
                                generate(className, PAYLOAD);
                                alreadyGeneratedClasses.add(className);
                            }
                        }));
    }

    private void generateChildClasses(Map<String, List<String>> parentToChildClassesRelation,
            List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount) {

        parentToChildCount.stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {

                    generate(entry.getKey(), PAYLOAD);

                    List<String> childClasses = Optional.ofNullable(parentToChildClassesRelation.get(entry.getKey()))
                            .orElseGet(Collections::emptyList)
                            .stream()
                            .distinct()
                            .collect(Collectors.toList());

                    if (!childClasses.isEmpty())
                        childClasses.forEach(childClass -> {
                            generate(childClass, PAYLOAD);
                        });
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
                    var jsonString = fileInteractor.readFile(pathResolver.getSchemaPayloadsDirectory().resolve(baseClassName));
                    var jsonSchemaNode = jsonUtils.getJsonNodeFromString(jsonString);
                    return new JsonSchemaWrapper(baseClassName, jsonString, jsonSchemaNode);
                }).collect(Collectors.toList());
    }

    private List<String> extractBaseClasses(JsonNode schemas) {
        List<String> baseClasses = new ArrayList<>();
        schemas.fieldNames().forEachRemaining(schemaName -> {
            if (!eventClassNames.contains(schemaName)) {
                baseClasses.add(schemaName);
            }
        });
        return baseClasses;
    }

    private void generateMainEvents(Set<String> mainEvents) {
        mainEvents.forEach(event -> {
            generate(event, "");
        });
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
        Optional.of(pathResolver.resolveAsyncApiFilePath(asyncApiDirectory, asyncApiFilename))
                .map(fileInteractor::readFile)
                .ifPresent(this::manipulateAndGenerateFromJson);
    }

    private void generateFromApicurio(final String baseUrl, String serviceName, String artifactVersion) {
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
