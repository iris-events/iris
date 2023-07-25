package org.iris_events.auth;

import static io.quarkus.security.identity.SecurityIdentity.USER_ATTRIBUTE;
import static org.iris_events.common.MessagingHeaders.Message.JWT;

import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.iris_events.context.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.smallrye.jwt.runtime.auth.JsonWebTokenCredential;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;

@ApplicationScoped
public class IrisJwtValidator {
    private static final Logger log = LoggerFactory.getLogger(IrisJwtValidator.class);

    private final EventContext eventContext;
    final JWTParser parser;

    @Inject
    public IrisJwtValidator(final EventContext eventContext, final JWTParser parser) {
        this.eventContext = eventContext;
        this.parser = parser;
    }

    public SecurityIdentity authenticate(Set<String> rolesAllowed) {
        final var optionalToken = getToken();
        if (optionalToken.isPresent()) {
            final var token = optionalToken.get();
            final var securityIdentity = createSecurityIdentity(token);
            checkRolesAllowed(securityIdentity, rolesAllowed);

            return securityIdentity;
        }

        if (rolesAllowed.isEmpty()) {
            return null;
        }

        throw new UnauthorizedException("Client is not authorized");
    }

    private void checkRolesAllowed(final SecurityIdentity securityIdentity, final Set<String> rolesAllowed) {
        if (rolesAllowed.isEmpty()) {
            return;
        }

        final var isAnyRoleAllowed = rolesAllowed.stream()
                .anyMatch(role -> securityIdentity.hasRole(role) || "**".equals(role));

        if (!isAnyRoleAllowed) {
            throw new ForbiddenException("Role is not allowed");
        }
    }

    private SecurityIdentity createSecurityIdentity(final String jwtToken) {
        try {
            JsonWebTokenCredential jsonWebTokenCredential = new JsonWebTokenCredential(jwtToken);
            JsonWebToken jwtPrincipal = this.parser.parse(jwtToken);
            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(jwtPrincipal)
                    .addCredential(jsonWebTokenCredential)
                    .addRoles(jwtPrincipal.getGroups())
                    .addAttribute(USER_ATTRIBUTE, jwtPrincipal).build();
        } catch (ParseException e) {
            log.error("Authentication failed. Error message: " + e.getMessage(), e);
            throw new AuthenticationFailedException("Invalid authorization token", e);
        }
    }

    public Optional<String> getToken() {
        return Optional.ofNullable(eventContext.getHeaders().get(JWT))
                .map(Object::toString);
    }
}
