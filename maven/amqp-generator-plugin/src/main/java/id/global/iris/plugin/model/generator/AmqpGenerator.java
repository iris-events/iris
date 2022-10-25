package id.global.iris.plugin.model.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.sun.codemodel.JCodeModel;

import id.global.iris.plugin.model.generator.annotators.MetadataAnnotator;
import id.global.iris.plugin.model.generator.configs.EventSchemaGeneratorConfig;
import id.global.iris.plugin.model.generator.exception.AmqpGeneratorException;
import id.global.iris.plugin.model.generator.graph.GraphUtils;
import id.global.iris.plugin.model.generator.models.ArtifactSource;
import id.global.iris.plugin.model.generator.models.ChannelDetails;
import id.global.iris.plugin.model.generator.models.JsonSchemaWrapper;
import id.global.iris.plugin.model.generator.utils.AmqpStringUtils;
import id.global.iris.plugin.model.generator.utils.ExistingJavaTypeProcessor;
import id.global.iris.plugin.model.generator.utils.FileInteractor;
import id.global.iris.plugin.model.generator.utils.JsonUtils;
import id.global.iris.plugin.model.generator.utils.PathResolver;
import id.global.iris.plugin.model.generator.utils.SchemaFileGenerator;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;

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

import static id.global.iris.plugin.model.generator.utils.AmqpStringUtils.getRefToBeReplaced;
import static id.global.iris.plugin.model.generator.utils.AmqpStringUtils.getReplacementForRef;
import static id.global.iris.plugin.model.generator.utils.StringConstants.COMMA;
import static id.global.iris.plugin.model.generator.utils.StringConstants.DOT;
import static id.global.iris.plugin.model.generator.utils.StringConstants.DOT_REGEX;
import static id.global.iris.plugin.model.generator.utils.StringConstants.EMPTY_STRING;
import static id.global.iris.plugin.model.generator.utils.StringConstants.FORWARD_SLASH;
import static id.global.iris.plugin.model.generator.utils.StringConstants.JAVA_TYPE;
import static id.global.iris.plugin.model.generator.utils.StringConstants.PAYLOAD;
import static id.global.iris.plugin.model.generator.utils.StringConstants.POM_TEMPLATE_XML;
import static id.global.iris.plugin.model.generator.utils.StringConstants.POM_XML;
import static id.global.iris.plugin.model.generator.utils.StringConstants.PUBLISH;
import static id.global.iris.plugin.model.generator.utils.StringConstants.REF;
import static id.global.iris.plugin.model.generator.utils.StringConstants.REF_REGEX;
import static id.global.iris.plugin.model.generator.utils.StringConstants.SUBSCRIBE;
import static java.util.stream.Collectors.joining;

public class AmqpGenerator {
    private static final String defaultUrl = "https://schema.tools.global.id";
    public static final String X_IRIS_GENERATED_PROPERTY = "x-iris-generated";

    private final Pattern REF_PATTERN = Pattern.compile(REF_REGEX);
    private final Map<String, ChannelDetails> channelDetails;
    private final Set<String> eventClassNames;
    private final SchemaFileGenerator schemaFileGenerator;
    private final PathResolver pathResolver;
    private final ObjectMapper objectMapper;
    private final FileInteractor fileInteractor;
    private final JsonUtils jsonUtils;
    private final Log log;

    private final String packageName;
    private final String modelVersion;
    private final String modelName;
    private final String asyncApiFilename;
    private final String asyncApiDirectory;
    private final String apicurioUrl;

    private final BinaryOperator<String> first = (first, second) -> first;
    private final BinaryOperator<String> last = (first, second) -> second;
    private final ExistingJavaTypeProcessor existingJavaTypeProcessor;

    public AmqpGenerator(SchemaFileGenerator schemaFileGenerator, ObjectMapper objectMapper, PathResolver pathResolver,
            FileInteractor fileInteractor, Log log, String packageName, String modelVersion, String modelName,
            String asyncApiFilename, String asyncApiDirectory, String apicurioUrl) {
        this.schemaFileGenerator = schemaFileGenerator;
        this.log = log;
        this.packageName = packageName;
        this.modelVersion = modelVersion;
        this.modelName = modelName;
        this.asyncApiFilename = asyncApiFilename;
        this.asyncApiDirectory = asyncApiDirectory;
        this.apicurioUrl = apicurioUrl;

        this.eventClassNames = new HashSet<>();
        this.channelDetails = new HashMap<>();

        this.pathResolver = pathResolver;
        this.fileInteractor = fileInteractor;
        this.objectMapper = objectMapper;

        this.jsonUtils = new JsonUtils(this.objectMapper, this.log);
        this.existingJavaTypeProcessor = new ExistingJavaTypeProcessor(objectMapper);
    }

    public void generate(ArtifactSource artifactSource) throws AmqpGeneratorException {
        if (artifactSource == ArtifactSource.FILE) {
            generateFromFile(pathResolver, fileInteractor, asyncApiDirectory, asyncApiFilename);
        } else if (artifactSource == ArtifactSource.APICURIO) {
            generateFromApicurio(fileInteractor, apicurioUrl, modelName, modelVersion);
        } else {
            // TODO new AmqpGeneratorException
            throw new AmqpGeneratorException("Execute failed! Artifact source location not known!");
        }
    }

    private void generateFromFile(PathResolver pathResolver, FileInteractor fileInteractor, String asyncApiDirectory,
            String asyncApiFilename) {
        Optional.of(pathResolver.resolveAsyncApiFilePath(asyncApiDirectory, asyncApiFilename))
                .map(fileInteractor::readFile)
                .ifPresent(content -> manipulateAndGenerateFromJson(jsonUtils, fileInteractor, content));
    }

    private void generateFromApicurio(FileInteractor fileInteractor, final String baseUrl, String serviceName,
            String artifactVersion) {
        String apicurioUrl = Optional.of(baseUrl)
                .filter(Predicate.not(String::isBlank))
                .orElse(defaultUrl);

        Optional.of(serviceName)
                .map(artifactName -> fileInteractor.readContentFromWeb(
                        String.format("%s/api/artifacts/%s:%s:json", apicurioUrl, artifactName, artifactVersion)))
                .filter(content -> !content.isBlank())
                .ifPresent(content -> manipulateAndGenerateFromJson(jsonUtils, fileInteractor, content));
    }

    public void manipulateAndGenerateFromJson(JsonUtils jsonUtils, FileInteractor fileInteractor, final String jsonContent) {

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

        Map<String, List<String>> parentToChildClassesRelation = extractParentToChildRelationsStructure(
                javaTypeLocationToCanonicalName);

        List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount = extractParentToChildrenCount(
                baseClassesSchemasWithContent);

        List<String> classChains = GraphUtils.getClassChains(parentToChildClassesRelation);

        List<String> filteredClassChains = sortAndFilterClassChains(classChains);

        generateJavaClasses(parentToChildClassesRelation, parentToChildCount, filteredClassChains);
    }

    private JsonNode removeGeneratedClassSchemas(JsonNode schemas) {
        ObjectNode filteredSchemas = objectMapper.createObjectNode();

        if (schemas instanceof ObjectNode) {
            schemas.fields()
                    .forEachRemaining((schema) -> {
                        if (!schema.getValue().has(X_IRIS_GENERATED_PROPERTY)
                                || !schema.getValue().get(X_IRIS_GENERATED_PROPERTY).booleanValue()) {
                            filteredSchemas.set(schema.getKey(), schema.getValue());
                        }
                    });
        }
        return filteredSchemas;
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

    private void rewriteRefsInSchemaFiles(Path schemasPath) {

        try {
            HashMap<String, String> schemaNamesToLocation = new HashMap<>();

            try (var payload = Files.list(schemasPath.resolve(PAYLOAD))) {
                payload.map(Path::toFile)
                        .filter(File::isFile)
                        .forEach(file -> schemaNamesToLocation.put(file.getName(), file.getAbsolutePath()));
            }

            try (var schemas = Files.list(schemasPath)) {
                schemas.map(Path::toFile)
                        .forEach(file -> {
                            if (file.isFile()) {
                                String fileContent = fileInteractor.readFile(Path.of(file.toURI()));
                                schemaNamesToLocation.entrySet().stream()
                                        .filter(entry -> fileContent.contains(entry.getValue()))
                                        .forEach(entry -> replaceFileContent(file, fileContent, entry));
                            }
                        });
            }

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

    private List<String> extractBaseClasses(JsonNode schemas) {
        List<String> baseClasses = new ArrayList<>();
        schemas.fieldNames().forEachRemaining(schemaName -> {
            if (!eventClassNames.contains(schemaName)) {
                baseClasses.add(schemaName);
            }
        });
        return baseClasses;
    }

    private List<JsonSchemaWrapper> extractSchemaContentForClasses(List<String> baseClasses) {
        return baseClasses.stream()
                .map(baseClassName -> {
                    var jsonString = fileInteractor.readFile(pathResolver.getSchemaPayloadsDirectory().resolve(baseClassName));
                    var jsonSchemaNode = jsonUtils.getJsonNodeFromString(jsonString);
                    return new JsonSchemaWrapper(baseClassName, jsonString, jsonSchemaNode);
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

    private List<AbstractMap.SimpleEntry<String, Integer>> extractParentToChildrenCount(
            List<JsonSchemaWrapper> baseClassesSchemasWithContent) {
        return baseClassesSchemasWithContent.stream()
                .map(schemaWithContent -> {
                    var javaTypeCount = StringUtils.countMatches(schemaWithContent.getSchemaContent(), JAVA_TYPE);
                    return new AbstractMap.SimpleEntry<>(schemaWithContent.getClassName(), javaTypeCount);
                }).collect(Collectors.toList());
    }

    private List<String> sortAndFilterClassChains(final List<String> lst) {
        List<String> uniq = new ArrayList<>(lst);
        lst.stream()
                .sorted((classChainFirst, classChainSecond) -> Integer.compare(classChainSecond.split(COMMA).length,
                        classChainFirst.split(COMMA).length))
                .collect(Collectors.toCollection(ArrayList::new))
                .forEach(elem -> uniq.removeIf(x -> !x.equals(elem) && elem.contains(x)));
        return uniq;
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

    private void replaceFileContent(File file, String fileContent, Map.Entry<String, String> nameWithLocation) {
        String fileName = nameWithLocation.getKey();
        String replaceWith = getReplacementForRef(fileName, packageName, AmqpStringUtils.getPackageName(modelName));

        String fileLocation = nameWithLocation.getValue();
        String toReplace = getRefToBeReplaced(fileLocation);

        String replacementContent = fileContent.replace(toReplace, replaceWith);
        fileInteractor.writeFile(Path.of(file.toURI()), jsonUtils.getFormattedJson(replacementContent));
    }

    private void replaceAllRefs(File file) {
        String fileContent = fileInteractor.readFile(Path.of(file.toURI()));
        while (fileContent.contains(REF)) {
            Matcher matcher = REF_PATTERN.matcher(fileContent);

            if (matcher.find()) {
                var toReplace = matcher.group(0);
                var name = Arrays.stream(matcher.group(2).split(FORWARD_SLASH))
                        .reduce((first, second) -> second).orElse(EMPTY_STRING);
                fileContent = fileContent.replace(toReplace,
                        getReplacementForRef(name, packageName, AmqpStringUtils.getPackageName(modelName)));
            }
        }
        fileInteractor.writeFile(Path.of(file.toURI()), jsonUtils.getFormattedJson(fileContent));
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

    private String getClassNameFromString(String fullName, BinaryOperator<String> operator) {
        return Arrays.stream(fullName.split(DOT_REGEX))
                .reduce(operator)
                .orElse(EMPTY_STRING);
    }

    private void generateMainEvents(Set<String> mainEvents) {
        mainEvents.forEach(event -> generate(event, ""));
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
                            .distinct().toList();

                    if (!childClasses.isEmpty())
                        childClasses.forEach(childClass -> generate(childClass, PAYLOAD));
                });
    }

    private void generateChildClassesByChain(List<String> filteredClassChains, List<String> alreadyGeneratedClasses) {
        filteredClassChains.forEach(classChain -> Arrays.stream(classChain.split(COMMA))
                .forEach(className -> {
                    if (!alreadyGeneratedClasses.contains(className)) {
                        generate(className, PAYLOAD);
                        alreadyGeneratedClasses.add(className);
                    }
                }));
    }

    private void generateLeafChildClasses(List<AbstractMap.SimpleEntry<String, Integer>> parentToChildCount) {
        parentToChildCount.stream()
                .filter(entry -> entry.getValue() == 0)
                .map(AbstractMap.SimpleEntry::getKey)
                .forEach(entry -> generate(entry, PAYLOAD));
    }

    private void generatePom() {
        var pomPath = pathResolver.getWorkingDirectory().resolve(POM_XML);
        var pomTemplate = preparePomTemplate();
        fileInteractor.writeFile(pomPath, pomTemplate);

    }

    private void generate(final String fileName, String subdirectory) {
        var classPackageName = generatePackageName(subdirectory);
        var annotator = getJackson2Annotator(fileName, eventClassNames);
        var config = getConfigurationByLocation(subdirectory);
        var schemaMapper = createSchemaMapper(config, annotator);
        var schemaPath = getSchemaFilePath(fileName, subdirectory);

        try {
            var schemaContent = existingJavaTypeProcessor.fixExistingType(fileInteractor.readFile(schemaPath));
            var codeModel = new JCodeModel();
            var destinationDirectory = pathResolver.getSourceDirectory().toFile();
            schemaMapper.generate(codeModel, fileName, classPackageName, schemaContent);
            codeModel.build(destinationDirectory);
        } catch (IOException e) {
            log.error("There was an error in Mapper or CodeModel.", e);
            throw new RuntimeException(e);
        }
    }

    private String preparePomTemplate() {
        var pomTemplate = fileInteractor.readResourceFileContent(POM_TEMPLATE_XML);
        return pomTemplate
                .replace("APPLICATION_NAME", AmqpStringUtils.getPomArtifactId(modelName))
                .replace("APPLICATION_VERSION", modelVersion)
                .replace("PACKAGE_NAME", packageName);

    }

    private String generatePackageName(String locationForGeneratedClass) {
        String modelPackage = AmqpStringUtils.getPackageName(modelName);
        return Stream.of(packageName, modelPackage, locationForGeneratedClass)
                .filter(s -> !s.isBlank())
                .collect(joining(DOT));
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
                            .getValue());

            annotator = new MetadataAnnotator(ch.orElseThrow().node(), EventSchemaGeneratorConfig.eventConfig);
        }
        return annotator;
    }

    private GenerationConfig getConfigurationByLocation(String locationForGeneratedClass) {
        return locationForGeneratedClass.isBlank() ? EventSchemaGeneratorConfig.classConfig
                : EventSchemaGeneratorConfig.eventConfig;
    }

    private SchemaMapper createSchemaMapper(final GenerationConfig config, final Jackson2Annotator annotator) {
        return new SchemaMapper(
                new RuleFactory(config, annotator, new SchemaStore()),
                new SchemaGenerator());
    }

    private Path getSchemaFilePath(String fileName, String locationForGeneratedClass) {
        return Optional.of(locationForGeneratedClass)
                .filter(m -> !m.isBlank())
                .map(m -> pathResolver.getSchemasDirectory().resolve(m).resolve(fileName))
                .orElseGet(() -> pathResolver.getSchemasDirectory().resolve(fileName));
    }

    private Predicate<Map.Entry<String, ChannelDetails>> nonEmptyChannelSection() {
        return entry -> !entry.getValue().getSectionsForChannelEvent().getValue().isEmpty();
    }

    private Predicate<Map.Entry<String, ChannelDetails>> hasMainEvent(String fileName) {
        return entry -> entry.getValue().publishEventName().equalsIgnoreCase(fileName)
                || entry.getValue().subscribeEventName().equalsIgnoreCase(fileName);
    }

}
