package main.controllers;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import main.models.LoginRequest;
import main.models.User;
import main.services.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Set;

@RestController
@CrossOrigin
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<User> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        User userDto = authService.login(request);
        String username = userDto.getEmail();

        Set<String> perms = userDto.getPermissions();
        String jwt = authService.generateToken(username, perms);

        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofHours(2))
                .sameSite("Strict")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(userDto);
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
