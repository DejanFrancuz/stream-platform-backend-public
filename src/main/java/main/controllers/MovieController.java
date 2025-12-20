package main.controllers;

import main.config.VideoConfig;
import main.models.Movie;
import main.models.MovieFilterDto;
import main.models.User;
import main.services.MovieService;
import main.services.UserService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    private final UserService userService;

    private static final String VIDEO_PATH = "C:/Users/Dejan/Desktop/stream-backend/media/videos/";

    private final VideoConfig videoConfig;

    public MovieController(MovieService movieService, UserService userService, VideoConfig videoConfig) {
        this.movieService = movieService;
        this.userService = userService;
        this.videoConfig = videoConfig;
    }

    @GetMapping(value = "/all",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Movie> getAllMovie(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("myMovies") boolean myMovies,
            @RequestParam(name = "like", required = false) boolean like,
            @RequestParam(name = "sort", required = false) boolean sort,
            @RequestParam(name = "genre", required = false) String genre,
            @RequestParam(name = "decade", required = false) String decade
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.loadUserByEmail(username);

        MovieFilterDto filter = new MovieFilterDto();
        filter.setGenre(genre);
        filter.setDecade(decade);
        if (like) filter.setLike(true);

        Sort sortType;
        if (sort) {
            sortType = Sort.by(Sort.Direction.ASC, "year");
        } else sortType = Sort.by(Sort.Direction.DESC, "year");

        Pageable pageable = PageRequest.of(page, size, sortType);
        return movieService.filterMovies(myMovies, user.getLikedMovies(), user.getOwnedMovies(), filter, pageable);
    }

    @GetMapping(value = "/watch/{movieId}", produces = "video/mp4")
    public ResponseEntity<?> watchMovie(
            @PathVariable (value = "movieId") final Long movieId,
            @RequestHeader HttpHeaders headers
    ) throws IOException {

        Optional<Movie> optionalMovie = movieService.findById(movieId);

        if (optionalMovie.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Movie movie = optionalMovie.get();

//        if (videoConfig.isCloudFront()) {
            return redirectToCloudFront(movie);
//        }

//        return streamLocalVideo(movie, headers);
    }

    private ResponseEntity<Void> redirectToCloudFront(Movie movie) {

        URI uri = URI.create(
                videoConfig.getCloudfrontBaseUrl() + "/media/videos/" + movie.getVideoPreviewUrl()
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }

    public ResponseEntity<ResourceRegion> streamLocalVideo(Movie movie, HttpHeaders headers) throws IOException {

        FileSystemResource videoResource = new FileSystemResource(VIDEO_PATH + movie.getVideoPreviewUrl());
        long contentLength = videoResource.contentLength();

        MediaType mediaType = MediaTypeFactory.getMediaType(videoResource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        ResourceRegion region = getResourceRegion(videoResource, headers, contentLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .body(region);
    }

    private ResourceRegion getResourceRegion(Resource video, HttpHeaders headers, long contentLength) {
        long chunkSize = 1_000_000;

        if (headers.getRange().isEmpty()) {
            return new ResourceRegion(video, 0, Math.min(chunkSize, contentLength));
        } else {
            HttpRange range = headers.getRange().get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(chunkSize, end - start + 1);
            return new ResourceRegion(video, start, rangeLength);
        }
    }

    @GetMapping(value = "/get-one",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<Movie> getMovie(@RequestParam("movieId") Long id) {
        return movieService.findById(id);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping(value = "/update",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMovie(@RequestBody Movie movieDto) {
        Optional<Movie> optionalMovie = movieService.findById(movieDto.getMovieId());

        if (optionalMovie.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Movie movie = optionalMovie.get();

        movie.setTitle(movieDto.getTitle());
        movie.setYear(movieDto.getYear());
        movie.setDescription(movieDto.getDescription());
        movie.setGenre(movieDto.getGenre());

        return ResponseEntity.ok(movieService.save(movie));

    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/add",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Movie createMovie(@RequestBody Movie movie) {
        return movieService.save(movie);
    }

    @PostMapping(value = "/add-movie-for-person",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addMovieForPerson(@RequestBody long[] movieIds) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userService.loadUserByEmail(username);


        for(Long movieId : movieIds) {
            Optional<Movie> optionalMovie = movieService.findById(movieId);

            if (optionalMovie.isEmpty()) return ResponseEntity.notFound().build();

            Movie movie = optionalMovie.get();

            user.getOwnedMovies().add(movie.getMovieId());
            userService.save(user);

        }
        return ResponseEntity.ok().build();

    }

    @PostMapping(value = "/like-movie-for-person",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> likeMovieForPerson(@RequestBody long movieId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userService.loadUserByEmail(username);


        Optional<Movie> optionalMovie = movieService.findById(movieId);

        if (optionalMovie.isEmpty()) return ResponseEntity.notFound().build();

        Movie movie = optionalMovie.get();

        if (!user.getLikedMovies().contains(movie.getMovieId())) {
            user.getLikedMovies().add(movie.getMovieId());
        } else user.getLikedMovies().remove(movie.getMovieId());
        userService.save(user);

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "/delete")
    public ResponseEntity<?> deleteUser(@RequestParam("id") Long id) {
        this.movieService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
