package org.iris_events.asyncapi.runtime.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import org.iris_events.asyncapi.runtime.json.IrisObjectMapper;
import org.iris_events.asyncapi.runtime.scanner.model.ClientDefinitions;

class ClientDefinitionParserTest {
    private static final String ASYNCAPI_FE_FILE = "src/test/resources/asyncapi_fe_test.json";

    @Test
    @DisplayName("Test extracting channels and schemas in [frontend, user, session, broadcast] scopes from asyncapi document.")
    void parse() throws IOException {
        File file = new File(ASYNCAPI_FE_FILE);
        JsonNode rootNode = IrisObjectMapper.getObjectMapper().readTree(file);

        ClientDefinitionParser definitionParser = new ClientDefinitionParser();
        ClientDefinitions parsedFeDefs = definitionParser.parse(rootNode);

        assertThat(parsedFeDefs.getServiceName(), is("eventhandlersapp"));

        Map<String, JsonNode> feScopeChannelNodes = parsedFeDefs.getClientChannelNodes();
        assertThat(feScopeChannelNodes.size(), is(4));
        assertThat(feScopeChannelNodes.containsKey("test-event-v1/default-test-event-v1"), is(true));
        assertThat(feScopeChannelNodes.containsKey("test-event-v2/test-event-v2"), is(true));
        assertThat(feScopeChannelNodes.containsKey("frontend-test-event-v1/fe-test-event-v1"), is(true));
        assertThat(feScopeChannelNodes.containsKey("test-topic-exchange/*.*.rabbit,fast.orange.*"), is(true));

        Map<String, JsonNode> schemasNodes = parsedFeDefs.getSchemasNodes();
        assertThat(schemasNodes.size(), is(9));
        assertThat(schemasNodes.containsKey("TestEventV1"), is(true));
        assertThat(schemasNodes.containsKey("TestEventV2"), is(true));
        assertThat(schemasNodes.containsKey("FrontendTestEventV1"), is(true));
        assertThat(schemasNodes.containsKey("TopicTestEventV1"), is(true));
        assertThat(schemasNodes.containsKey("User"), is(true));
        assertThat(schemasNodes.containsKey("Status"), is(true));
        assertThat(schemasNodes.containsKey("Multilevel"), is(true));
        assertThat(schemasNodes.containsKey("Multilevel1"), is(true));
        assertThat(schemasNodes.containsKey("Multilevel2"), is(true));

        // All channels have bindings
        Set<Map.Entry<String, JsonNode>> withBindings = feScopeChannelNodes.entrySet().stream()
                .filter(stringJsonNodeEntry -> stringJsonNodeEntry.getValue().findValue("bindings") != null)
                .collect(Collectors.toSet());
        assertThat(withBindings.size(), is(feScopeChannelNodes.size()));

        // All channels have message node with payload
        Set<Map.Entry<String, JsonNode>> withMessage = feScopeChannelNodes.entrySet().stream()
                .filter(stringJsonNodeEntry -> stringJsonNodeEntry.getValue().findValue("message").findValue("payload") != null)
                .collect(Collectors.toSet());
        assertThat(withMessage.size(), is(feScopeChannelNodes.size()));
    }
}
