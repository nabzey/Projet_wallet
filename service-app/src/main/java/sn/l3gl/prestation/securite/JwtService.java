package sn.l3gl.prestation.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Ne signe aucun token ici : service-app est un simple Resource Server qui
 * valide les JWT émis par wallet-service (secret HMAC partagé via APP_JWT_SECRET).
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims extraireClaims(String token) {
        return Jwts.parser()
                .verifyWith(cle())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean estValide(String token) {
        try {
            extraireClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extraireUtilisateurId(String token) {
        Object valeur = extraireClaims(token).get("utilisateurId");
        return valeur instanceof Integer ? ((Integer) valeur).longValue() : (Long) valeur;
    }
}
