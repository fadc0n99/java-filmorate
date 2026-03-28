package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.RecommendationService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RecommendationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    @Qualifier("userDbStorage")
    private UserStorage userStorage;

    @Autowired
    @Qualifier("filmDbStorage")
    private FilmStorage filmStorage;

    private User user1;
    private User user2;
    private User user3;
    private Film film1;
    private Film film2;
    private Film film3;
    private Film film4;
    private Film film5;
    private Film film6;
    private Film film7;
    private Film film8;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .email("user1@test.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .friendsIds(Set.of())
                .build();

        user2 = User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .friendsIds(Set.of())
                .build();

        user3 = User.builder()
                .email("user3@test.com")
                .login("user3")
                .name("User Three")
                .birthday(LocalDate.of(1994, 3, 3))
                .friendsIds(Set.of())
                .build();

        user1 = userStorage.save(user1);
        user2 = userStorage.save(user2);
        user3 = userStorage.save(user3);

        film1 = Film.builder()
                .name("Film 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1L).build())
                .genres(List.of(Genre.builder().id(1L).build()))
                .likedUsersFilms(Set.of())
                .build();

        film2 = Film.builder()
                .name("Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2020, 2, 1))
                .duration(130)
                .mpa(Mpa.builder().id(2L).build())
                .genres(List.of(Genre.builder().id(2L).build()))
                .likedUsersFilms(Set.of())
                .build();

        film3 = Film.builder()
                .name("Film 3")
                .description("Description 3")
                .releaseDate(LocalDate.of(2020, 3, 1))
                .duration(140)
                .mpa(Mpa.builder().id(3L).build())
                .genres(List.of(Genre.builder().id(3L).build()))
                .likedUsersFilms(Set.of())
                .build();

        film4 = Film.builder()
                .name("Film 4")
                .description("Description 4")
                .releaseDate(LocalDate.of(2020, 4, 1))
                .duration(150)
                .mpa(Mpa.builder().id(4L).build())
                .genres(List.of(Genre.builder().id(4L).build()))
                .likedUsersFilms(Set.of())
                .build();

        film5 = Film.builder()
                .name("Film 5")
                .description("Description 5")
                .releaseDate(LocalDate.of(2020, 5, 1))
                .duration(160)
                .mpa(Mpa.builder().id(5L).build())
                .genres(List.of(Genre.builder().id(5L).build()))
                .likedUsersFilms(Set.of())
                .build();

        film6 = Film.builder()
                .name("Film 6")
                .description("Description 6")
                .releaseDate(LocalDate.of(2020, 6, 1))
                .duration(170)
                .mpa(Mpa.builder().id(1L).build())
                .genres(List.of(
                        Genre.builder().id(1L).build(),
                        Genre.builder().id(2L).build()
                ))
                .likedUsersFilms(Set.of())
                .build();

        film7 = Film.builder()
                .name("Film 7")
                .description("Description 7")
                .releaseDate(LocalDate.of(2020, 7, 1))
                .duration(180)
                .mpa(Mpa.builder().id(2L).build())
                .genres(List.of(
                        Genre.builder().id(3L).build(),
                        Genre.builder().id(4L).build()
                ))
                .likedUsersFilms(Set.of())
                .build();

        film8 = Film.builder()
                .name("Film 8")
                .description("Description 8")
                .releaseDate(LocalDate.of(2020, 8, 1))
                .duration(190)
                .mpa(Mpa.builder().id(3L).build())
                .genres(List.of(
                        Genre.builder().id(5L).build(),
                        Genre.builder().id(6L).build()
                ))
                .likedUsersFilms(Set.of())
                .build();

        film1 = filmStorage.save(film1);
        film2 = filmStorage.save(film2);
        film3 = filmStorage.save(film3);
        film4 = filmStorage.save(film4);
        film5 = filmStorage.save(film5);
        film6 = filmStorage.save(film6);
        film7 = filmStorage.save(film7);
        film8 = filmStorage.save(film8);
    }

    @Test
    void getFilmRecommendations_ShouldReturnRecommendations_BasedOnSimilarUser() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film3.getId(), user1.getId());

        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film4.getId(), user2.getId());
        filmStorage.addLike(film5.getId(), user2.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .containsExactlyInAnyOrder(film4.getId(), film5.getId());

        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .doesNotContain(film1.getId(), film2.getId(), film3.getId());
    }

    @Test
    void getFilmRecommendations_ShouldReturnEmptyList_WhenUserHasNoLikes() {
        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user2.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).isEmpty();
    }

    @Test
    void getFilmRecommendations_ShouldReturnEmptyList_WhenNoSimilarUsers() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film3.getId(), user1.getId());

        filmStorage.addLike(film4.getId(), user2.getId());
        filmStorage.addLike(film5.getId(), user2.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).isEmpty();
    }

    @Test
    void getFilmRecommendations_ShouldReturnRecommendations_FromMostSimilarUser() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film3.getId(), user1.getId());
        filmStorage.addLike(film4.getId(), user1.getId());

        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film3.getId(), user2.getId());
        filmStorage.addLike(film6.getId(), user2.getId());
        filmStorage.addLike(film7.getId(), user2.getId());

        filmStorage.addLike(film1.getId(), user3.getId());
        filmStorage.addLike(film2.getId(), user3.getId());
        filmStorage.addLike(film3.getId(), user3.getId());
        filmStorage.addLike(film4.getId(), user3.getId());
        filmStorage.addLike(film5.getId(), user3.getId());
        filmStorage.addLike(film8.getId(), user3.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .containsExactlyInAnyOrder(film5.getId(), film8.getId());
    }

    @Test
    void getFilmRecommendations_ShouldThrowNotFoundException_WhenUserDoesNotExist() {
        long nonExistentUserId = 999L;

        assertThrows(RuntimeException.class, () ->
                recommendationService.getFilmRecommendationsByUserLikes(nonExistentUserId)
        );
    }

    @Test
    void getFilmRecommendations_ShouldReturnEmptyList_WhenOnlyOneUserExists() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).isEmpty();
    }

    @Test
    void getFilmRecommendations_ShouldHandleMultipleSimilarUsers() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());

        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user2.getId());

        filmStorage.addLike(film1.getId(), user3.getId());
        filmStorage.addLike(film2.getId(), user3.getId());
        filmStorage.addLike(film3.getId(), user3.getId());
        filmStorage.addLike(film4.getId(), user3.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .containsExactlyInAnyOrder(film3.getId(), film4.getId());
    }

    @Test
    void getFilmRecommendations_ShouldReturnEmptyList_WhenSimilarUserHasNoNewFilms() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film3.getId(), user1.getId());

        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film3.getId(), user2.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).isEmpty();
    }

    @Test
    void getFilmRecommendations_ShouldHandleUserWithManyLikes() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film3.getId(), user1.getId());
        filmStorage.addLike(film4.getId(), user1.getId());
        filmStorage.addLike(film5.getId(), user1.getId());

        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film3.getId(), user2.getId());
        filmStorage.addLike(film6.getId(), user2.getId());
        filmStorage.addLike(film7.getId(), user2.getId());
        filmStorage.addLike(film8.getId(), user2.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).hasSize(3);
        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .containsExactlyInAnyOrder(film6.getId(), film7.getId(), film8.getId());

        List<Long> userLikes = List.of(film1.getId(), film2.getId(), film3.getId(), film4.getId(), film5.getId());
        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .doesNotContainAnyElementsOf(userLikes);
    }

    @Test
    void getFilmRecommendations_ShouldWorkWithMixedRecommendations() {
        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());

        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film3.getId(), user2.getId());
        filmStorage.addLike(film4.getId(), user2.getId());

        List<FilmDto> recommendations = recommendationService.getFilmRecommendationsByUserLikes(user1.getId());

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .containsExactlyInAnyOrder(film3.getId(), film4.getId());

        assertThat(recommendations)
                .extracting(FilmDto::getId)
                .doesNotContain(film1.getId());
    }
}