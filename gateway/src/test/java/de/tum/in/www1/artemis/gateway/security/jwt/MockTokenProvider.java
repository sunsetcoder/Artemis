package de.tum.in.www1.artemis.gateway.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.ObjectUtils;
import tech.jhipster.config.JHipsterProperties;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * This class implements token creation in order to test the token verification,
 * since it is not included in {@link TokenProvider} because the Gateway is not responsible for authenticating users.
 */
public class MockTokenProvider {
    private final Key key;

    private long tokenValidityInMilliseconds;

    private long tokenValidityInMillisecondsForRememberMe;

    private static final String AUTHORITIES_KEY = "auth";

    public MockTokenProvider(JHipsterProperties jHipsterProperties) {
        byte[] keyBytes;
        String secret = jHipsterProperties.getSecurity().getAuthentication().getJwt().getBase64Secret();
        if (!ObjectUtils.isEmpty(secret)) {
            keyBytes = Decoders.BASE64.decode(secret);
        }
        else {
            secret = jHipsterProperties.getSecurity().getAuthentication().getJwt().getSecret();
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        key = Keys.hmacShaKeyFor(keyBytes);
        tokenValidityInMilliseconds = jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSeconds();
        tokenValidityInMillisecondsForRememberMe = jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSecondsForRememberMe();
    }

    /**
     * Create JWT Token a fully populated <code>Authentication</code> object.
     * @param authentication Authentication Object
     * @param rememberMe Determines Token lifetime (30 minutes vs 30 days)
     * @return JWT Token
     */
    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + tokenValidityInMillisecondsForRememberMe);
        }
        else {
            validity = new Date(now + tokenValidityInMilliseconds);
        }

        return Jwts.builder().setSubject(authentication.getName()).claim(AUTHORITIES_KEY, authorities).signWith(key, SignatureAlgorithm.HS512).setExpiration(validity).compact();
    }
}
