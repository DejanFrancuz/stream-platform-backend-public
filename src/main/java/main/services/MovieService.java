package main.services;

import jakarta.persistence.criteria.Predicate;
import main.interfaces.IService;
import main.models.Movie;
import main.models.MovieFilterDto;
import main.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MovieService implements IService<Movie, Long> {

    private MovieRepository movieRepository;

    public Page<Movie> filterMovies(boolean myMovies, List<Long> likedMovies, List<Long> ownedMovies, MovieFilterDto filter, Pageable pageable) {
        Specification<Movie> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.isLike()) {
                predicates.add(root.get("movieId").in(likedMovies));
            }

            if (filter.getGenre() != null && !filter.getGenre().isBlank()) {
                predicates.add(cb.equal(root.get("genre"), filter.getGenre()));
            }

            if (filter.getDecade() != null && filter.getDecade().matches("\\d{4}s")) {
                int startYear = Integer.parseInt(filter.getDecade().substring(0, 4));
                int endYear = startYear + 9;
                predicates.add(cb.between(root.get("year"), startYear, endYear));
            }

                if(!myMovies){
                    predicates.add(cb.not(root.get("movieId").in(ownedMovies)));
                } else {
                    predicates.add(root.get("movieId").in(ownedMovies));
                }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return movieRepository.findAll(spec, pageable);
    }

    @Autowired
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public Movie save(Movie movie) {
        return  movieRepository.save(movie);
    }

    @Override
    public Optional<Movie> findById(Long id) {
        return movieRepository.findById(id);
    }

    @Override
    public List<Movie> findAll() {
        return movieRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        movieRepository.deleteById(id);
    }

}
