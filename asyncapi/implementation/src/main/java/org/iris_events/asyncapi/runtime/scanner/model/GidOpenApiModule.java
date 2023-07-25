/*
 * Copyright 2019 VicTools.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iris_events.asyncapi.runtime.scanner.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.iris_events.asyncapi.spec.annotations.media.Schema;
import org.iris_events.asyncapi.spec.annotations.media.SchemaProperty;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.TypeScope;

/**
 * Gid Open Api Schema Generator Module inspired by
 * <a href="https://github.com/victools/jsonschema-generator/tree/main/jsonschema-module-swagger-1.5">Java JSON Schema Generator
 * – Module Swagger (1.5)</a>.
 */
public class GidOpenApiModule implements Module {

    private final List<GidOpenApiOption> options;
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+\\.?[0-9]*");

    /**
     * Constructor.
     *
     * @param options features to enable
     */
    public GidOpenApiModule(GidOpenApiOption... options) {
        this.options = options == null ? Collections.emptyList() : Arrays.asList(options);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = builder.forFields();
        this.applyToConfigPart(fieldConfigPart);
        if (this.options.contains(GidOpenApiOption.ENABLE_PROPERTY_NAME_OVERRIDES)) {
            fieldConfigPart
                    .withPropertyNameOverrideResolver(this::resolvePropertyNameOverride);
        }
        this.applyToConfigPart(builder.forMethods());

        if (!this.options.contains(GidOpenApiOption.NO_APIMODEL_TITLE)) {
            builder.forTypesInGeneral()
                    .withTitleResolver(this::resolveTitleForType);
        }
        if (!this.options.contains(GidOpenApiOption.NO_APIMODEL_DESCRIPTION)) {
            builder.forTypesInGeneral()
                    .withDescriptionResolver(this::resolveDescriptionForType);
        }
    }

    /**
     * Apply configurations that are part of this module to the given configuration part – expectation being that fields and
     * methods get the same.
     *
     * @param configPart configuration instance to add configurations too
     */
    private void applyToConfigPart(SchemaGeneratorConfigPart<?> configPart) {
        if (this.options.contains(GidOpenApiOption.IGNORING_HIDDEN_PROPERTIES)) {
            configPart.withIgnoreCheck(this::shouldIgnore);
        }
        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withNumberExclusiveMinimumResolver(this::resolveNumberExclusiveMinimum);
        configPart.withNumberInclusiveMinimumResolver(this::resolveNumberInclusiveMinimum);
        configPart.withNumberExclusiveMaximumResolver(this::resolveNumberExclusiveMaximum);
        configPart.withNumberInclusiveMaximumResolver(this::resolveNumberInclusiveMaximum);
        configPart.withEnumResolver(this::resolveEnumerationValues);
    }

    /**
     * Determine whether a given member should be ignored, i.e. excluded from the generated schema.
     *
     * @param member targeted field/method
     * @return whether to ignore the given field/method
     */
    protected boolean shouldIgnore(MemberScope<?, ?> member) {
        SchemaProperty annotation = member.getAnnotationConsideringFieldAndGetter(SchemaProperty.class);
        return annotation != null && annotation.hidden();
    }

    /**
     * Look-up name override for a given field or its associated getter method from the {@link SchemaProperty} annotation's
     * {@code name}.
     *
     * @param field targeted field
     * @return applicable name override (or {@code null})
     */
    protected String resolvePropertyNameOverride(FieldScope field) {
        return Optional.ofNullable(field.getAnnotationConsideringFieldAndGetter(SchemaProperty.class))
                .map(SchemaProperty::name)
                .filter(name -> !name.isEmpty() && !name.equals(field.getName()))
                .orElse(null);
    }

    /**
     * Look-up a "description" for the given member or its associated getter/field from the {@link SchemaProperty} annotation's
     * {@code value}.
     *
     * @param member targeted field/method
     * @return description (or {@code null})
     */
    protected String resolveDescription(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetterIfSupported(SchemaProperty.class))
                .map(SchemaProperty::description)
                .filter(value -> !value.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up a "description" from the given type's {@link Schema} annotation's {@code description}.
     *
     * @param scope targeted type
     * @return description (or {@code null})
     */
    protected String resolveDescriptionForType(TypeScope scope) {
        return Optional.ofNullable(scope.getType())
                .map(type -> type.getErasedType().getAnnotation(Schema.class))
                .map(Schema::description)
                .filter(description -> !description.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up a "title" for the given member or its associated getter/field from the member type's {@link Schema} annotation's
     * {@code value}.
     *
     * @param scope targeted type
     * @return title (or {@code null})
     */
    protected String resolveTitleForType(TypeScope scope) {
        return Optional.ofNullable(scope.getType())
                .map(type -> type.getErasedType().getAnnotation(Schema.class))
                .map(Schema::title)
                .filter(title -> !title.isEmpty())
                .orElse(null);
    }

    /**
     * Retrieve the given member's (or its associated getter/field's) {@link SchemaProperty} annotation and extract its
     * {@code minimum}.
     *
     * @param member targeted field/method
     * @return {@link SchemaProperty} annotation's non-empty {@code minimum} (or {@code null})
     */
    private Optional<BigDecimal> findModelPropertyMinimum(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetterIfSupported(SchemaProperty.class))
                .map(SchemaProperty::minimum)
                .filter(minimum -> !minimum.isEmpty())
                .filter(minimum -> NUMBER.matcher(minimum).matches())
                .map(BigDecimal::new);
    }

    private Optional<BigDecimal> findModelPropertyMaximum(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetterIfSupported(SchemaProperty.class))
                .map(SchemaProperty::maximum)
                .filter(maximum -> !maximum.isEmpty())
                .filter(maximum -> NUMBER.matcher(maximum).matches())
                .map(BigDecimal::new);
    }

    private Optional<Boolean> findModelPropertyExclusiveMinimum(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetterIfSupported(SchemaProperty.class))
                .map(SchemaProperty::exclusiveMinimum);
    }

    private Optional<Boolean> findModelPropertyExclusiveMaximum(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetterIfSupported(SchemaProperty.class))
                .map(SchemaProperty::exclusiveMaximum);
    }

    private Optional<List<String>> findModelPropertyEnumeration(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetterIfSupported(SchemaProperty.class))
                .map(SchemaProperty::enumeration)
                .map(Arrays::asList);
    }

    /**
     * Determine (inclusive) numeric minimum for the given member or its associated getter/field from the {@link SchemaProperty}
     * annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return inclusive numeric minimum (or {@code null})
     */
    protected BigDecimal resolveNumberInclusiveMinimum(MemberScope<?, ?> member) {
        final var modelPropertyExclusiveMinimum = this.findModelPropertyExclusiveMinimum(member);
        final var isMinimumExclusive = modelPropertyExclusiveMinimum.orElse(false);
        if (isMinimumExclusive) {
            return null;
        }

        final var modelPropertyMinimum = this.findModelPropertyMinimum(member);
        return modelPropertyMinimum.orElse(null);
    }

    /**
     * Determine (exclusive) numeric minimum for the given member or its associated getter/field from the {@link SchemaProperty}
     * annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return exclusive numeric minimum (or {@code null})
     */
    protected BigDecimal resolveNumberExclusiveMinimum(MemberScope<?, ?> member) {
        final var modelPropertyExclusiveMinimum = this.findModelPropertyExclusiveMinimum(member);
        final var isMinimumExclusive = modelPropertyExclusiveMinimum.orElse(false);
        if (!isMinimumExclusive) {
            return null;
        }

        final var modelPropertyMinimum = this.findModelPropertyMinimum(member);
        return modelPropertyMinimum.orElse(null);
    }

    /**
     * Determine (inclusive) numeric maximum for the given member or its associated getter/field from the
     * {@link SchemaProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return inclusive numeric maximum (or {@code null})
     */
    protected BigDecimal resolveNumberInclusiveMaximum(MemberScope<?, ?> member) {
        final var modelPropertyExclusiveMaximum = this.findModelPropertyExclusiveMaximum(member);
        final var isMaximumExclusive = modelPropertyExclusiveMaximum.orElse(false);
        if (isMaximumExclusive) {
            return null;
        }

        final var modelPropertyMaximum = this.findModelPropertyMaximum(member);
        return modelPropertyMaximum.orElse(null);
    }

    /**
     * Determine (exclusive) numeric maximum for the given member or its associated getter/field from the
     * {@link SchemaProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return exclusive numeric maximum (or {@code null})
     */
    protected BigDecimal resolveNumberExclusiveMaximum(MemberScope<?, ?> member) {
        final var modelPropertyExclusiveMaximum = this.findModelPropertyExclusiveMaximum(member);
        final var isMaximumExclusive = modelPropertyExclusiveMaximum.orElse(false);
        if (!isMaximumExclusive) {
            return null;
        }

        final var modelPropertyMaximum = this.findModelPropertyMaximum(member);
        return modelPropertyMaximum.orElse(null);
    }

    /**
     * Look-up a "const"/"enum" for the given member or its associated getter/field from the {@link SchemaProperty} annotation's
     * {@code enumeration}.
     *
     * @param member targeted field/method
     * @return list of enumeration values (or {@code null})
     */
    protected List<String> resolveEnumerationValues(MemberScope<?, ?> member) {
        return this.findModelPropertyEnumeration(member).orElse(null);
    }
}
