package main.controllers;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import main.models.*;
import main.security.JwtUtil;
import main.services.AuthService;
import main.services.RefreshTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Set;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<User> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        User userDto = authService.login(request);
        String username = userDto.getEmail();

        Set<String> perms = userDto.getPermissions();
        String accessToken = authService.generateToken(username, perms);

        String refreshToken = refreshTokenService.generateRefreshToken(username);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(2))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(2))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/refresh")
        public ResponseEntity<User> refresh(@CookieValue(value = "refresh_token", required = false) String refreshToken,
                                                      HttpServletResponse response){

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RefreshToken stored = refreshTokenService.validate(refreshToken);

        refreshTokenService.revoke(stored);
        String newRefresh = refreshTokenService.generateRefreshToken(stored.getUsername());

        ResponseCookie cookie = ResponseCookie.from("refresh_token", newRefresh)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(2))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        User user = authService.getPersonData(stored.getUsername());
        String newAccess = authService.generateToken(user.getUsername(), user.getPermissions());

        ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccess)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(2))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        User userDto = authService.login(request);

        if(userDto != null){
            return  ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/get-person")
    public ResponseEntity<User> getCurrentUser(Authentication auth) {
        String username = auth.getName();

        User user = authService.getPersonData(username);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }
}
