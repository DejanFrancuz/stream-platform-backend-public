package main.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Entity
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> permissions = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "user_movies", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "movie_id")
    private List<Long> ownedMovies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "liked_movies", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "movie_id")
    private List<Long> likedMovies = new ArrayList<>();

    @Column
    private String password;

    @Column
    private Boolean deleted;


    public User() {
    }

    public User(String firstName,String lastName, String email, Set<String> permissions){
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.permissions =  new HashSet<>(permissions);
    }

    public User(String firstName, String username, String password, String lastName, String email, Set<String> permissions){
        this.firstName = firstName;
        this.username = username;
        this.password = password;
        this.lastName = lastName;
        this.email = email;
        this.permissions =  new HashSet<>(permissions);
    }

    public void addPermission(String permission) {
        if( permission == null){
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }
}
