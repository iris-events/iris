package id.global.iris.asyncapi.runtime.scanner.validator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

import id.global.iris.asyncapi.runtime.exception.AnnotationValidationException;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;

class MessageAnnotationValidatorTest {

    public static final String TEST_PROJECT_NAME = "testProjectName";
    public static final String TEST_GROUP_ID = "id.global.iris";
    public static final List<String> RESERVED_NAMES = ReservedAmqpNamesProvider.getReservedNames();

    @Test
    void validateReservedNames() {
        MessageAnnotationValidator validator = new MessageAnnotationValidator();
        List<AnnotationValue> values = createAllowedValues();
        AnnotationInstance instance = AnnotationInstance.create(
                DotName.createSimple(Message.class.getName()),
                getClassTarget(),
                values);

        List<AnnotationInstance> annotations = new ArrayList<>();
        annotations.add(instance);

        assertDoesNotThrow(() -> validator.validateReservedNames(annotations, TEST_PROJECT_NAME, TEST_GROUP_ID));
    }

    @Test
    void validateNonClassAnnotations() {
        MessageAnnotationValidator validator = new MessageAnnotationValidator();
        List<AnnotationValue> values = createAllowedValues();
        AnnotationInstance instance = AnnotationInstance.create(
                DotName.createSimple(Message.class.getName()),
                getMethodTarget(),
                values);

        AnnotationValidationException annotationValidationException = assertThrows(AnnotationValidationException.class,
                () -> validator.validateReservedNames(List.of(instance), TEST_PROJECT_NAME, TEST_GROUP_ID));
        assertThat(annotationValidationException.getMessage(), is(notNullValue()));
        assertThat(annotationValidationException.getMessage(), startsWith("Annotation target is not class. Annotations:"));
        assertThat(annotationValidationException.getFaultyAnnotations().size(), is(1));
    }

    @Test
    void validateNonMessageAnnotations() {
        MessageAnnotationValidator validator = new MessageAnnotationValidator();
        List<AnnotationValue> values = createAllowedValues();
        AnnotationInstance instance = AnnotationInstance.create(
                DotName.createSimple(MessageHandler.class.getName()),
                getClassTarget(),
                values);

        AnnotationValidationException annotationValidationException = assertThrows(AnnotationValidationException.class,
                () -> validator.validateReservedNames(List.of(instance), TEST_PROJECT_NAME, TEST_GROUP_ID));
        assertThat(annotationValidationException.getMessage(), is(notNullValue()));
        assertThat(annotationValidationException.getMessage(), startsWith("Annotation not of Message class. Annotations:"));
        assertThat(annotationValidationException.getFaultyAnnotations().size(), is(1));
    }

    @Test
    void validateReservedAnnotationValues() {
        MessageAnnotationValidator validator = new MessageAnnotationValidator();
        AnnotationValidationException annotationValidationException = assertThrows(AnnotationValidationException.class,
                () -> validator.validateReservedNames(createAnnotationInstancesUsingReservedValues(), TEST_PROJECT_NAME,
                        TEST_GROUP_ID));
        assertThat(annotationValidationException.getMessage(), is(notNullValue()));
        assertThat(annotationValidationException.getMessage(), startsWith("Annotations use reserved names. Annotations:"));
        assertThat(annotationValidationException.getFaultyAnnotations(), hasSize(RESERVED_NAMES.size()));
    }

    @Test
    void validateReservedAnnotationValuesWithAllowedService() {
        MessageAnnotationValidator validator = new MessageAnnotationValidator();
        assertDoesNotThrow(
                () -> validator.validateReservedNames(createAnnotationInstancesUsingReservedValues(), "iris-subscription",
                        TEST_GROUP_ID));
        assertDoesNotThrow(
                () -> validator.validateReservedNames(createAnnotationInstancesUsingReservedValues(), "iris-manager",
                        TEST_GROUP_ID));
    }

    private ClassInfo getClassTarget() {
        return ClassInfo.create(
                DotName.createSimple(AnnotationTestParent.class.getName()),
                DotName.createSimple(Object.class.getName()),
                (short) 0,
                new DotName[0],
                new HashMap<>(),
                true);
    }

    private MethodInfo getMethodTarget() {
        ClassInfo clazz = getClassTarget();
        Type returnType = PrimitiveType.INT;
        return MethodInfo.create(
                clazz,
                "method",
                new Type[0],
                returnType,
                (short) 0);
    }

    private List<AnnotationValue> createAllowedValues() {
        List<AnnotationValue> values = new ArrayList<>();
        values.add(AnnotationValue.createStringValue("name", "exchange-name"));
        values.add(AnnotationValue.createStringValue("routingKey", "routing-key"));
        return values;
    }

    private List<AnnotationValue> createReservedValues(String reservedValue) {
        List<AnnotationValue> values = new ArrayList<>();
        values.add(AnnotationValue.createStringValue("name", reservedValue));
        values.add(AnnotationValue.createStringValue("routingKey", reservedValue));
        return values;
    }

    private List<AnnotationInstance> createAnnotationInstancesUsingReservedValues() {
        DotName messageDotName = DotName.createSimple(Message.class.getName());

        return RESERVED_NAMES.stream()
                .map(this::createReservedValues)
                .map(annotationValues -> AnnotationInstance.create(messageDotName, getClassTarget(), annotationValues))
                .collect(Collectors.toList());
    }

    static class AnnotationTestParent {

    }
}
