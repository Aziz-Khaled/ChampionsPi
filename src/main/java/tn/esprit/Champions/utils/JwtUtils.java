package tn.esprit.Champions.utils;

import com.auth0.jwt.JWT;

import com.auth0.jwt.algorithms.Algorithm;
import tn.esprit.Champions.models.Utilisateur;
import java.util.Date;

public class JwtUtils {
    // In a real app, load this from a secure config file or environment variable
    private static final String SECRET_KEY = "Your_Super_Secret_Key_Change_Me";
    private static final String ISSUER = "Champions_Fintech_Platform";

    public static String generateToken(Utilisateur user) {
        // Token expires in 24 hours
        long expirationTime = 1000 * 60 * 60 * 24;
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(user.getId_user()))
                .withClaim("email", user.getEmail())
                .withClaim("nom", user.getNom())
                .withClaim("prenom", user.getPrenom())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .sign(algorithm);
    }
}