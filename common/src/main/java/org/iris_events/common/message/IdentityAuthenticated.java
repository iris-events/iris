package org.iris_events.common.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.iris_events.annotations.Message;

import java.util.UUID;

import static org.iris_events.common.Exchanges.Constants;

@Message(name = Constants.IDENTITY_AUTHENTICATED)
public record IdentityAuthenticated(@JsonProperty("gid_uuid") UUID gid_uuid) {
}
