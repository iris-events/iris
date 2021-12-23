package id.global.event.messaging.runtime.auth;

import static id.global.common.headers.amqp.MessageHeaders.JWT;

import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.global.event.messaging.runtime.context.EventContext;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.smallrye.jwt.runtime.auth.JsonWebTokenCredential;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;

public class GidJwtValidator {
    private static final Logger log = LoggerFactory.getLogger(GidJwtValidator.class);

    private final EventContext eventContext;
    final JWTParser parser;

    @Inject
    public GidJwtValidator(final EventContext eventContext, final JWTParser parser) {
        this.eventContext = eventContext;
        this.parser = parser;
    }

    public SecurityIdentity authenticate() {
        final var optionalToken = getToken();
        return optionalToken.map(this::createSecurityIdentity)
                .orElse(null);
    }

    private SecurityIdentity createSecurityIdentity(final String jwtToken) {
        try {
            JsonWebTokenCredential jsonWebTokenCredential = new JsonWebTokenCredential(jwtToken);
            JsonWebToken jwtPrincipal = this.parser.parse(jwtToken);
            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(jwtPrincipal)
                    .addCredential(jsonWebTokenCredential)
                    .addRoles(jwtPrincipal.getGroups())
                    .addAttribute("quarkus.user", jwtPrincipal).build();
        } catch (ParseException var3) {
            log.debug("Authentication failed", var3);
            throw new AuthenticationFailedException(var3);
        }
    }

    public Optional<String> getToken() {
        return Optional.ofNullable(eventContext.getHeaders().get(JWT))
                .map(Object::toString);
    }
}
