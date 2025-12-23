package main.services;

import main.exception.RefreshTokenNotFoundException;
import main.models.RefreshToken;
import main.repositories.RefreshTokenRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateRefreshToken(String email) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String hash = hash(rawToken);

        RefreshToken token = new RefreshToken();
        token.setEmail(email);
        token.setTokenHash(hash);
        token.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
        token.setRevoked(false);

        refreshTokenRepository.save(token);

        return rawToken;
    }

    public RefreshToken validate(String rawToken) {
        String hash = hash(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenNotFoundException("Refresh token expired or revoked");
        }

        return token;
    }

    public void revokeByRawToken(String rawToken) {
        String hash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }


    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private String hash(String value) {
        return DigestUtils.sha256Hex(value);
    }
}
