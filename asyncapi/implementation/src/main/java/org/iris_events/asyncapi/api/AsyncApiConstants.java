package org.iris_events.asyncapi.api;

/**
 * Copyright 2019 Red Hat, Inc, and individual contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public final class AsyncApiConstants {

    public static final String PROJECT_VERSION = "mp.asyncapi.project.version";
    public static final String SCAN_DEPENDENCIES_DISABLE = "mp.asyncapi.extensions.scan-dependencies.disable";
    public static final String SCAN_DEPENDENCIES_JARS = "mp.asyncapi.extensions.scan-dependencies.jars";
    public static final String SCHEMA_REFERENCES_ENABLE = "mp.asyncapi.extensions.schema-references.enable";
    public static final String CUSTOM_SCHEMA_REGISTRY_CLASS = "mp.asyncapi.extensions.custom-schema-registry.class";

    public static final String EXCLUDE_FROM_SCHEMAS = "mp.asyncapi.exclude-from-schemas";
    public static final String REF = "ref";

    public static final String REF_NODE = "$ref";
    public static final String CHANNELS_NODE = "channels";
    public static final String COMPONENTS_NODE = "components";
    public static final String SCHEMAS_NODE = "schemas";
    public static final String INFO_NODE = "info";
    public static final String TITLE_NODE = "title";
    public static final String PROPERTIES_NODE = "properties";
    public static final String PAYLOAD_NODE = "payload";
    public static final String HEADERS_NODE = "headers";
    public static final String X_SCOPE_NODE = "x-scope";
    public static final String VALUE_NODE = "value";
    public static final String COMPONENT_SCHEMAS_PREFIX = "#/components/schemas/";
}
