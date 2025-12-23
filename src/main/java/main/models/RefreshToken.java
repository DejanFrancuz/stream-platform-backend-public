package main.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
public class RefreshToken {
    @Id
    @GeneratedValue
    private Long id;

    private String email;

    private String tokenHash;

    private Instant expiresAt;

    private boolean revoked;
}
