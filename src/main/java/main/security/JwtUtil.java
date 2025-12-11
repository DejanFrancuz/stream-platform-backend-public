package main.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    public SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    public String base64Key = Encoders.BASE64.encode(key.getEncoded());

    public Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(base64Key).parseClaimsJws(token).getBody();
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        List<String> authorities = (List<String>) extractAllClaims(token).get("roles");
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String generateToken(String username, Set<String> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", new ArrayList<>(authorities));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.HS512, base64Key)
                .compact();
    }

    public boolean validateToken(String token){
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}

