package middleware.security;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class CustomAuthFilter extends AuthFilter<CustomCredentials, CustomAuthUser> {
  private CustomAuthenticator authenticator;

  public CustomAuthFilter(CustomAuthenticator authenticator) {
    this.authenticator = authenticator;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    Optional<CustomAuthUser> authenticatedUser;

    try {
      CustomCredentials credentials = getCredentials(requestContext);
      authenticatedUser = authenticator.authenticate(credentials);
    } catch (AuthenticationException e) {
      throw new WebApplicationException("Unable to validate credentials", Response.Status.UNAUTHORIZED);
    }

    if (authenticatedUser.isPresent()) {
      Long tenantId = parseTenantId(requestContext);
      SecurityContext securityContext = new CustomSecurityContext(authenticatedUser.get(), tenantId, requestContext.getSecurityContext());
      requestContext.setSecurityContext(securityContext);
    } else {
      throw new WebApplicationException("Credentials not valid", Response.Status.UNAUTHORIZED);
    }
  }

  private Long parseTenantId(ContainerRequestContext requestContext) {
    try {
      return Long.valueOf(requestContext.getUriInfo().getPathParameters().getFirst("tenantId"));
    } catch (Exception e) {
      throw new WebApplicationException("No tenant ID in path", Response.Status.FORBIDDEN);
    }
  }

  private CustomCredentials getCredentials(ContainerRequestContext requestContext) {
    CustomCredentials credentials = new CustomCredentials();

    try {
      String rawToken = requestContext
        .getCookies()
        .get("auth_token")
        .getValue();

      String rawUserId = requestContext
        .getCookies()
        .get("auth_user")
        .getValue();

      credentials.setToken(UUID.fromString(rawToken));
      credentials.setUserId(Long.valueOf(rawUserId));
    } catch (Exception e) {
      throw new WebApplicationException("Unable to parse credentials", Response.Status.UNAUTHORIZED);
    }


    return credentials;
  }
}
