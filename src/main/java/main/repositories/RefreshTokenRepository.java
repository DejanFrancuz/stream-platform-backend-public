package main.repositories;


import main.models.Movie;
import main.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>, JpaSpecificationExecutor<Movie> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
