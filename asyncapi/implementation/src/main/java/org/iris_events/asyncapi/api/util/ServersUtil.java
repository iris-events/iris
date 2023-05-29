/**
 * Copyright 2019 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iris_events.asyncapi.api.util;

import java.util.Set;

import org.iris_events.asyncapi.api.AsyncApiConfig;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.core.models.Document;

/**
 * Used to configure server information from config properties.
 *
 * @author eric.wittmann@gmail.com
 */
public class ServersUtil {

    /**
     * Constructor.
     */
    private ServersUtil() {
    }

    public static final void configureServers(AsyncApiConfig config, Document doc) {
        AaiDocument aaiDoc = (AaiDocument) doc;
        // Start with the global servers.
        Set<String> servers = config.servers();
        if (servers != null && !servers.isEmpty()) {
            int counter = 1;
            for (String server : servers) {
                // Note: in AsyncAPI it's a map of servers, so each server now needs a name.  We should
                // consider changing the config parameter to support named servers.  For now, we'll invent
                // a name for each.
                String serverName = "server-" + counter++;
                aaiDoc.addServer(serverName, aaiDoc.createServer(serverName, server, null));
            }
        }
    }

}
