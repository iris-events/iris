package id.global.iris.messaging.runtime.auth;

import static id.global.iris.common.constants.MessagingHeaders.Message.JWT;
import static io.quarkus.security.identity.SecurityIdentity.USER_ATTRIBUTE;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.common.auth.jwt.Role;
import id.global.iris.messaging.runtime.context.EventContext;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.smallrye.jwt.runtime.auth.JsonWebTokenCredential;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;

@ApplicationScoped
public class GidJwtValidator {
    private static final Logger log = LoggerFactory.getLogger(GidJwtValidator.class);

    private final EventContext eventContext;
    final JWTParser parser;

    @Inject
    public GidJwtValidator(final EventContext eventContext, final JWTParser parser) {
        this.eventContext = eventContext;
        this.parser = parser;
    }

    public SecurityIdentity authenticate(Set<Role> rolesAllowed) {
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

    private void checkRolesAllowed(final SecurityIdentity securityIdentity, final Set<Role> rolesAllowed) {
        if (rolesAllowed.isEmpty()) {
            return;
        }

        final var isAnyRoleAllowed = rolesAllowed.stream()
                .map(Role::value)
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
