package org.iris_events.plugin.model.generator.utils;

import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CustomDependenciesTest {

    @ParameterizedTest
    @MethodSource("dependencyArguments")
    void getDependenciesValue(String dependenciesArgument, String expectedResult) {
        final var customDependencies = new CustomDependencies(dependenciesArgument);
        final var dependenciesValue = customDependencies.getDependenciesValue();
        MatcherAssert.assertThat(dependenciesValue, CoreMatchers.is(expectedResult));
    }

    public static Stream<Arguments> dependencyArguments() {
        return Stream.of(
                Arguments.arguments("", ""),
                Arguments.arguments("com.group.id:artefact-id:1.0.0",
                        "<dependency>\n" +
                                "<groupId>com.group.id</groupId>\n" +
                                "<artifactId>artefact-id</artifactId>\n" +
                                "<version>1.0.0</version>\n" +
                                "</dependency>\n"),

                Arguments.arguments("com.group.id:artefact-id:1.0.0-SNAPSHOT",
                        "<dependency>\n" +
                                "<groupId>com.group.id</groupId>\n" +
                                "<artifactId>artefact-id</artifactId>\n" +
                                "<version>1.0.0-SNAPSHOT</version>\n" +
                                "</dependency>\n"),

                Arguments.arguments("com.group1.id:artefact-id-1:1.0.0,com.group2.id:artefact-id-2:1.0.0",
                        "<dependency>\n" +
                                "<groupId>com.group1.id</groupId>\n" +
                                "<artifactId>artefact-id-1</artifactId>\n" +
                                "<version>1.0.0</version>\n" +
                                "</dependency>\n" +
                                "<dependency>\n" +
                                "<groupId>com.group2.id</groupId>\n" +
                                "<artifactId>artefact-id-2</artifactId>\n" +
                                "<version>1.0.0</version>\n" +
                                "</dependency>\n"));
    }

}
