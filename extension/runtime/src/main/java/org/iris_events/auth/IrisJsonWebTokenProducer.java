package org.iris_events.auth;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.cdi.NullJsonWebToken;

@Priority(5)
@Alternative
@RequestScoped
public class IrisJsonWebTokenProducer {
    private static final Logger LOG = Logger.getLogger(IrisJsonWebTokenProducer.class);

    @Inject
    SecurityIdentity identity;

    /**
     * The producer method for the current access token
     *
     * @return the access token
     */
    @Produces
    @RequestScoped
    JsonWebToken currentAccessToken() {
        if (identity.isAnonymous()) {
            return new NullJsonWebToken();
        }
        if (identity.getPrincipal() instanceof JsonWebToken) {
            return (JsonWebToken) identity.getPrincipal();
        }
        throw new IllegalStateException("Current principal " + identity.getPrincipal() + " is not a JSON web token");
    }

}
