package sn.l3gl.wallet.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String genererToken(Long utilisateurId, String telephone) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + expirationMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(telephone)
                .claim("utilisateurId", utilisateurId)
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(cle())
                .compact();
    }

    public Claims extraireClaims(String token) {
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

    public String extraireTelephone(String token) {
        return extraireClaims(token).getSubject();
    }
}
