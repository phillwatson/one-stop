package com.hillayes.user.openid;

import com.hillayes.openid.AuthProvider;
import com.hillayes.openid.NamedAuthProvider;
import com.hillayes.openid.OpenIdAuth;
import com.hillayes.user.domain.OidcIdentity;
import com.hillayes.user.domain.User;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.UserRepository;
import com.hillayes.user.utils.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class OpenIdAuthenticationTest {
    @InjectMock
    UserRepository userRepository;

    @InjectMock
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdAuth openIdAuth;

    @InjectMock
    UserEventSender userEventSender;

    @Inject
    OpenIdAuthentication fixture;

    @Test
    public void testOauthLogin_ExistingUser() throws Exception {
        // given: and auth-provider identifier
        AuthProvider authProvider = AuthProvider.GOOGLE;
        when(openIdAuth.isFor(authProvider)).thenReturn(true);

        // and: an Open-ID auth-code
        String authCode = randomAlphanumeric(12);

        // and: the auth-provider exchanges the auth-code for ID-token (and auth-tokens)
        JwtClaims idToken = new JwtClaims();
        idToken.setIssuer("https://accounts.google.com/");
        idToken.setSubject(randomAlphanumeric(30));
        idToken.setStringClaim("email", randomAlphanumeric(30));
        when(openIdAuth.exchangeAuthToken(authCode)).thenReturn(idToken);

        // and: the auth-provider ID identifies a user
        User user = TestData.mockUser(UUID.randomUUID());
        user.addOidcIdentity(authProvider, idToken.getIssuer(), idToken.getSubject());
        when(userRepository.findByIssuerAndSubject(idToken.getIssuer(), idToken.getSubject()))
            .thenReturn(Optional.of(user));

        // when: the service is called
        User result = fixture.oauthExchange(authProvider, authCode);

        // then: the identified user is returned
        assertEquals(user, result);

        // and: the user's email address has been updated
        assertEquals(idToken.getClaimValueAsString("email").toLowerCase(), user.getEmail());
        verify(userEventSender).sendUserUpdated(user);
    }

    @Test
    public void testOauthLogin_ExistingUser_OidDisabled() throws Exception {
        // given: and auth-provider identifier
        AuthProvider authProvider = AuthProvider.GOOGLE;
        when(openIdAuth.isFor(authProvider)).thenReturn(true);

        // and: an Open-ID auth-code
        String authCode = randomAlphanumeric(12);

        // and: the auth-provider exchanges the auth-code for ID-token (and auth-tokens)
        JwtClaims idToken = new JwtClaims();
        idToken.setIssuer("https://accounts.google.com/");
        idToken.setSubject(randomAlphanumeric(30));
        idToken.setStringClaim("email", randomAlphanumeric(30));
        when(openIdAuth.exchangeAuthToken(authCode)).thenReturn(idToken);

        // and: the auth-provider ID identifies a user - with disabled OID identity
        User user = TestData.mockUser(UUID.randomUUID());
        user.addOidcIdentity(authProvider, idToken.getIssuer(), idToken.getSubject()).setDisabled(true);
        when(userRepository.findByIssuerAndSubject(idToken.getIssuer(), idToken.getSubject()))
            .thenReturn(Optional.of(user));

        // when: the service is called
        // then: authorisation fails
        assertThrows(NotAuthorizedException.class, () -> fixture.oauthExchange(authProvider, authCode));

        // and: no events are issued
        verifyNoInteractions(userEventSender);
    }

    @Test
    public void testOauthLogin_ExistingUser_UserBlocked() throws Exception {
        // given: and auth-provider identifier
        AuthProvider authProvider = AuthProvider.GOOGLE;
        when(openIdAuth.isFor(authProvider)).thenReturn(true);

        // and: an Open-ID auth-code
        String authCode = randomAlphanumeric(12);

        // and: the auth-provider exchanges the auth-code for ID-token (and auth-tokens)
        JwtClaims idToken = new JwtClaims();
        idToken.setIssuer("https://accounts.google.com/");
        idToken.setSubject(randomAlphanumeric(30));
        idToken.setStringClaim("email", randomAlphanumeric(30));
        when(openIdAuth.exchangeAuthToken(authCode)).thenReturn(idToken);

        // and: the auth-provider ID identifies a user - who is blocked
        User user = TestData.mockUser(UUID.randomUUID()).toBuilder()
            .dateBlocked(Instant.now().minusSeconds(200))
            .build();
        user.addOidcIdentity(authProvider, idToken.getIssuer(), idToken.getSubject());
        when(userRepository.findByIssuerAndSubject(idToken.getIssuer(), idToken.getSubject()))
            .thenReturn(Optional.of(user));

        // when: the service is called
        assertThrows(NotAuthorizedException.class, () -> fixture.oauthExchange(authProvider, authCode));

        // and: no events are issued
        verifyNoInteractions(userEventSender);
    }

    @Test
    public void testOauthLogin_ExistingUser_NewOid() throws Exception {
        // given: a user exists with links to OID account
        User user = TestData.mockUser(UUID.randomUUID());

        // and: and auth-provider identifier
        AuthProvider authProvider = AuthProvider.GOOGLE;
        when(openIdAuth.isFor(authProvider)).thenReturn(true);

        // and: an Open-ID auth-code
        String authCode = randomAlphanumeric(12);

        // and: the auth-provider exchanges the auth-code for ID-token (and auth-tokens)
        JwtClaims idToken = new JwtClaims();
        idToken.setIssuer("https://accounts.google.com/");
        idToken.setSubject(randomAlphanumeric(30));
        idToken.setStringClaim("email", user.getEmail()); // same as existing user
        when(openIdAuth.exchangeAuthToken(authCode)).thenReturn(idToken);

        // and: the auth-provider ID identifies NO existing user
        when(userRepository.findByIssuerAndSubject(idToken.getIssuer(), idToken.getSubject()))
            .thenReturn(Optional.empty());

        // and: the ID-token email address identifies the existing user
        when(userRepository.findByEmail(idToken.getClaimValueAsString("email").toLowerCase()))
            .thenReturn(Optional.of(user));

        // when: the service is called
        User result = fixture.oauthExchange(authProvider, authCode);

        // then: the user identified by the email address is returned
        assertEquals(user, result);

        // and: auth-provider's identifier is recorded against the user
        OidcIdentity oidcIdentity = result.getOidcIdentity(idToken.getIssuer()).orElse(null);
        assertNotNull(oidcIdentity);
        assertEquals(idToken.getSubject(), oidcIdentity.getSubject());

        // and: no events are issued
        verifyNoInteractions(userEventSender);
    }

    @Test
    public void testOauthLogin_ExistingUser_NewOid_UserBlocked() throws Exception {
        // given: a user exists with links to OID account
        User user = TestData.mockUser(UUID.randomUUID()).toBuilder()
            .dateBlocked(Instant.now().minusSeconds(3000))
            .build();

        // and: and auth-provider identifier
        AuthProvider authProvider = AuthProvider.GOOGLE;
        when(openIdAuth.isFor(authProvider)).thenReturn(true);

        // and: an Open-ID auth-code
        String authCode = randomAlphanumeric(12);

        // and: the auth-provider exchanges the auth-code for ID-token (and auth-tokens)
        JwtClaims idToken = new JwtClaims();
        idToken.setIssuer("https://accounts.google.com/");
        idToken.setSubject(randomAlphanumeric(30));
        idToken.setStringClaim("email", user.getEmail()); // same as existing user
        when(openIdAuth.exchangeAuthToken(authCode)).thenReturn(idToken);

        // and: the auth-provider ID identifies NO existing user
        when(userRepository.findByIssuerAndSubject(idToken.getIssuer(), idToken.getSubject()))
            .thenReturn(Optional.empty());

        // and: the ID-token email address identifies the existing user
        when(userRepository.findByEmail(idToken.getClaimValueAsString("email").toLowerCase()))
            .thenReturn(Optional.of(user));

        // when: the service is called
        // then: authorisation fails
        assertThrows(NotAuthorizedException.class, () -> fixture.oauthExchange(authProvider, authCode));

        // and: NO auth-provider's identifier is recorded against the user
        OidcIdentity oidcIdentity = user.getOidcIdentity(idToken.getIssuer()).orElse(null);
        assertNull(oidcIdentity);

        // and: no events are issued
        verifyNoInteractions(userEventSender);
    }

    @Test
    public void testOauthLogin_NewUser() throws Exception {
        // given: and auth-provider identifier
        AuthProvider authProvider = AuthProvider.GOOGLE;
        when(openIdAuth.isFor(authProvider)).thenReturn(true);

        // and: an Open-ID auth-code
        String authCode = randomAlphanumeric(12);

        // and: the auth-provider exchanges the auth-code for ID-token
        JwtClaims idToken = new JwtClaims();
        idToken.setIssuer("https://accounts.google.com/");
        idToken.setSubject(randomAlphanumeric(30));
        idToken.setStringClaim("email", randomAlphanumeric(30));
        idToken.setStringClaim("name", randomAlphanumeric(20));
        idToken.setStringClaim("given_name", randomAlphanumeric(20));
        idToken.setStringClaim("family_name", randomAlphanumeric(20));
        idToken.setStringClaim("locale", "en");

        when(openIdAuth.exchangeAuthToken(authCode)).thenReturn(idToken);

        // and: the auth-provider ID does NOT identify a user
        when(userRepository.findByIssuerAndSubject(idToken.getIssuer(), idToken.getSubject()))
            .thenReturn(Optional.empty());

        // when: the service is called
        User result = fixture.oauthExchange(authProvider, authCode);

        // then: the identified user is returned
        assertNotNull(result);

        // and: the user's properties are taken from ID token
        assertEquals(idToken.getClaimValueAsString("email").toLowerCase(), result.getEmail());
        assertEquals(idToken.getClaimValueAsString("email").toLowerCase(), result.getUsername());
        assertEquals(idToken.getClaimValueAsString("name"), result.getPreferredName());
        assertEquals(idToken.getClaimValueAsString("given_name"), result.getGivenName());
        assertEquals(idToken.getClaimValueAsString("family_name"), result.getFamilyName());
        assertEquals(idToken.getClaimValueAsString("locale"), result.getLocale().toLanguageTag());

        // and: user is onboarded
        assertNotNull(result.getDateCreated());
        assertNotNull(result.getDateOnboarded());

        // and: auth-provider's identifier is recorded against the user
        OidcIdentity oidcIdentity = result.getOidcIdentity(idToken.getIssuer()).orElse(null);
        assertNotNull(oidcIdentity);
        assertEquals(idToken.getSubject(), oidcIdentity.getSubject());

        // and: no events are issued
        verifyNoInteractions(userEventSender);
    }

    @Test
    public void testOauthLogin_NewUser_NoEmail() throws Exception {
        // given: and auth-provider identifier
        AuthProvider authProvider = AuthProvider.GOOGLE;
        when(openIdAuth.isFor(authProvider)).thenReturn(true);

        // and: an Open-ID auth-code
        String authCode = randomAlphanumeric(12);

        // and: the auth-provider exchanges the auth-code for ID-token
        JwtClaims idToken = new JwtClaims();
        idToken.setIssuer("https://accounts.google.com/");
        idToken.setSubject(randomAlphanumeric(30));
        idToken.setStringClaim("email", null);

        when(openIdAuth.exchangeAuthToken(authCode)).thenReturn(idToken);

        // and: the auth-provider ID does NOT identify a user
        when(userRepository.findByIssuerAndSubject(idToken.getIssuer(), idToken.getSubject()))
            .thenReturn(Optional.empty());

        // when: the service is called
        // then: an exception is raised
        assertThrows(NotAuthorizedException.class, () -> fixture.oauthExchange(authProvider, authCode));

        // and: no events are issued
        verifyNoInteractions(userEventSender);
    }
}
