package gcfv2;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.*;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

public class JwtValidator {

    private static final String ISSUER = "https://dev-jawlnaqsx3hptwx5.us.auth0.com/";
    private static final String AUDIENCE = "YOUR_API_IDENTIFIER";

    private static RemoteJWKSet<SecurityContext> jwkSet;

    static {
        try {
            jwkSet = new RemoteJWKSet<>(new URL(ISSUER + ".well-known/jwks.json"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JWTClaimsSet validate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            // Get matching key
            JWKSelector selector = new JWKSelector(
                    new JWKMatcher.Builder()
                            .keyID(jwt.getHeader().getKeyID())
                            .build()
            );

            var keys = jwkSet.get(selector, null);

            if (keys.isEmpty()) {
                throw new RuntimeException("No matching JWK found");
            }

            RSAPublicKey publicKey = keys.get(0).toRSAKey().toRSAPublicKey();

            // Verify signature
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!jwt.verify(verifier)) {
                throw new RuntimeException("Invalid signature");
            }

            // Validate claims
            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (!ISSUER.equals(claims.getIssuer())) {
                throw new RuntimeException("Invalid issuer");
            }

            if (!claims.getAudience().contains(AUDIENCE)) {
                throw new RuntimeException("Invalid audience");
            }

            if (claims.getExpirationTime().before(new Date())) {
                throw new RuntimeException("Token expired");
            }
            if (claims.getClaim("scope").equals("access:webhook")){
                throw new RuntimeException("Invalid scope");
            }

            return claims;

        } catch (Exception e) {
            throw new RuntimeException("Token validation failed", e);
        }
    }
}
