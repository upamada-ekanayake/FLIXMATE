package com.flixmate.core.service;

import com.flixmate.core.model.Movie;
import com.flixmate.core.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final Bucket tmdbRateLimiter = Bucket.builder()
            .addLimit(Bandwidth.classic(40, Refill.intervally(40, Duration.ofMinutes(1))))
            .build();

    @Value("${flixmate.tmdb.api-key}")
    private String tmdbApiKey;

    @Value("${flixmate.tmdb.base-url}")
    private String tmdbBaseUrl;

    public List<Movie> getAllMovies() {
        List<Movie> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            // Seed database with mock blockbusters if empty
            seedInitialMovies();
            return movieRepository.findAll();
        }
        return movies;
    }

    public Movie getMovieById(UUID id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found."));
    }

    public Movie syncMovieFromTmdb(String tmdbId) {
        Optional<Movie> existing = movieRepository.findByTmdbId(tmdbId);
        if (existing.isPresent()) {
            return existing.get();
        }

        if ("mock-key".equals(tmdbApiKey) || tmdbApiKey == null || tmdbApiKey.isBlank()) {
            log.info("TMDB key not provided, skipping API sync and initializing a dummy movie.");
            Movie dummy = Movie.builder()
                    .title("TMDB Synchronized Film")
                    .description("High-end thriller fetched dynamically via movie catalog sync.")
                    .genre("Sci-Fi / Thriller")
                    .durationMinutes(132)
                    .releaseDate(LocalDate.now().minusDays(15))
                    .averageRating(8.4)
                    .tmdbId(tmdbId)
                    .posterUrl("https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop")
                    .trailerUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                    .build();
            return movieRepository.save(dummy);
        }

        int maxRetries = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (!tmdbRateLimiter.tryConsume(1)) {
                log.warn("TMDB client-side rate limit hit. Waiting 1 second before retry (attempt {})...", attempt);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                continue;
            }

            try {
                log.info("Calling TMDB API for movie sync (ID: {}). Attempt {}/{}", tmdbId, attempt, maxRetries);
                String url = String.format("%s/movie/%s?api_key=%s&append_to_response=videos", tmdbBaseUrl, tmdbId, tmdbApiKey);
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map body = response.getBody();
                    String title = (String) body.get("title");
                    String overview = (String) body.get("overview");
                    int runtime = body.get("runtime") != null ? (Integer) body.get("runtime") : 120;
                    double voteAverage = body.get("vote_average") != null ? ((Number) body.get("vote_average")).doubleValue() : 7.0;
                    String releaseStr = (String) body.get("release_date");
                    LocalDate releaseDate = releaseStr != null ? LocalDate.parse(releaseStr) : LocalDate.now();

                    // Get genres
                    List<Map> genresList = (List<Map>) body.get("genres");
                    String genre = "Drama";
                    if (genresList != null && !genresList.isEmpty()) {
                        genre = (String) genresList.get(0).get("name");
                    }

                    // Get poster
                    String posterPath = (String) body.get("poster_path");
                    String posterUrl = posterPath != null ? "https://image.tmdb.org/t/p/w500" + posterPath : null;

                    // Get trailer from videos
                    String trailerUrl = null;
                    Map videosMap = (Map) body.get("videos");
                    if (videosMap != null) {
                        List<Map> results = (List<Map>) videosMap.get("results");
                        if (results != null && !results.isEmpty()) {
                            for (Map v : results) {
                                if ("Trailer".equals(v.get("type")) && "YouTube".equals(v.get("site"))) {
                                    trailerUrl = "https://www.youtube.com/watch?v=" + v.get("key");
                                    break;
                                }
                            }
                        }
                    }

                    Movie movie = Movie.builder()
                            .title(title)
                            .description(overview)
                            .genre(genre)
                            .durationMinutes(runtime)
                            .releaseDate(releaseDate)
                            .averageRating(voteAverage)
                            .tmdbId(tmdbId)
                            .posterUrl(posterUrl)
                            .trailerUrl(trailerUrl)
                            .build();

                    log.info("Successfully synced movie '{}' (TMDB ID: {}) from API.", title, tmdbId);
                    return movieRepository.save(movie);
                }
            } catch (Exception e) {
                log.error("TMDB API call failed on attempt {}/{} with error: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    break;
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                backoffMs *= 2; // exponential backoff
            }
        }

        return null;
    }

    private void seedInitialMovies() {
        log.info("Seeding initial movie catalog into PostgreSQL database...");
        List<Movie> list = List.of(
            Movie.builder()
                .title("Interstellar")
                .description("A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.")
                .genre("Sci-Fi / Adventure")
                .durationMinutes(169)
                .releaseDate(LocalDate.of(2014, 11, 7))
                .averageRating(8.7)
                .posterUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop")
                .trailerUrl("https://www.youtube.com/watch?v=zSWdZAZE3Lk")
                .tmdbId("157336")
                .build(),
            Movie.builder()
                .title("Inception")
                .description("A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.")
                .genre("Action / Sci-Fi")
                .durationMinutes(148)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .averageRating(8.8)
                .posterUrl("https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop")
                .trailerUrl("https://www.youtube.com/watch?v=YoHD9XEInc0")
                .tmdbId("27205")
                .build(),
            Movie.builder()
                .title("The Dark Knight")
                .description("When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.")
                .genre("Action / Crime / Drama")
                .durationMinutes(152)
                .releaseDate(LocalDate.of(2008, 7, 18))
                .averageRating(9.0)
                .posterUrl("https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=600&auto=format&fit=crop")
                .trailerUrl("https://www.youtube.com/watch?v=EXeTwQWrcwY")
                .tmdbId("155")
                .build(),
            Movie.builder()
                .title("Gladiator")
                .description("A former Roman General sets out to exact vengeance against the corrupt emperor who murdered his family and sent him into slavery.")
                .genre("Action / Adventure")
                .durationMinutes(155)
                .releaseDate(LocalDate.of(2000, 5, 5))
                .averageRating(8.5)
                .posterUrl("https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=600&auto=format&fit=crop")
                .trailerUrl("https://www.youtube.com/watch?v=P5ieIbInFpg")
                .tmdbId("98")
                .build()
        );
        movieRepository.saveAll(list);
    }
}
