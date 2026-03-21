package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaRequestDto;
import ru.yandex.practicum.filmorate.dto.review.CreateReviewDto;
import ru.yandex.practicum.filmorate.dto.review.UpdateReviewDto;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId1;
    private Long userId2;
    private Long userId3;
    private Long userId4;
    private Long filmId1;
    private Long filmId2;
    private Long filmId3;

    private CreateReviewDto createReviewDto;
    private UpdateReviewDto updateReviewDto;

    @BeforeEach
    void setUp() throws Exception {
        userId1 = createUser("user1@test.com", "user1", "User One", LocalDate.of(1990, 1, 1));
        userId2 = createUser("user2@test.com", "user2", "User Two", LocalDate.of(1991, 2, 2));
        userId3 = createUser("user3@test.com", "user3", "User Three", LocalDate.of(1992, 3, 3));
        userId4 = createUser("user4@test.com", "user4", "User Four", LocalDate.of(1993, 4, 4));

        filmId1 = createFilm("Film 1", "Description 1", LocalDate.of(2020, 1, 1), 120, 1);
        filmId2 = createFilm("Film 2", "Description 2", LocalDate.of(2021, 2, 2), 130, 2);
        filmId3 = createFilm("Film 3", "Description 3", LocalDate.of(2022, 3, 3), 140, 3);

        createReviewDto = new CreateReviewDto();
        createReviewDto.setContent("Good movie!");
        createReviewDto.setIsPositive(true);
        createReviewDto.setUserId(userId1);
        createReviewDto.setFilmId(filmId1);

        updateReviewDto = new UpdateReviewDto();
        updateReviewDto.setReviewId(1L);
        updateReviewDto.setContent("Updated review content");
        updateReviewDto.setIsPositive(false);
        updateReviewDto.setUserId(userId1);
        updateReviewDto.setFilmId(filmId1);
    }

    private Long createUser(String email, String login, String name, LocalDate birthday) throws Exception {
        User user = User.builder()
                .email(email)
                .login(login)
                .name(name)
                .birthday(birthday)
                .build();

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    private Long createFilm(String name, String description, LocalDate releaseDate, int duration, long mpaId) throws Exception {
        CreateFilmDto film = CreateFilmDto.builder()
                .name(name)
                .description(description)
                .releaseDate(releaseDate)
                .duration(duration)
                .mpa(MpaRequestDto.of(mpaId))
                .build();

        MvcResult result = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    private Long createReview(String content, boolean isPositive, Long userId, Long filmId) throws Exception {
        CreateReviewDto dto = new CreateReviewDto();
        dto.setContent(content);
        dto.setIsPositive(isPositive);
        dto.setUserId(userId);
        dto.setFilmId(filmId);

        MvcResult result = mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("reviewId").asLong();
    }

    @Test
    void saveReview_ShouldReturnCreatedReview() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReviewDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").exists())
                .andExpect(jsonPath("$.content").value("Good movie!"))
                .andExpect(jsonPath("$.isPositive").value(true))
                .andExpect(jsonPath("$.userId").value(userId1))
                .andExpect(jsonPath("$.filmId").value(filmId1))
                .andExpect(jsonPath("$.useful").value(0));
    }

    @Test
    void saveReview_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        createReviewDto.setUserId(999L);

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReviewDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void saveReview_WithNonExistentFilm_ShouldReturnNotFound() throws Exception {
        createReviewDto.setFilmId(999L);

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReviewDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void saveReview_WithDuplicateReview_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReviewDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReviewDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReview_ShouldReturnUpdatedReview() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);
        updateReviewDto.setReviewId(reviewId);

        mockMvc.perform(put("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReviewDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(reviewId))
                .andExpect(jsonPath("$.content").value("Updated review content"))
                .andExpect(jsonPath("$.isPositive").value(false))
                .andExpect(jsonPath("$.userId").value(userId1))
                .andExpect(jsonPath("$.filmId").value(filmId1));
    }

    @Test
    void updateReview_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        updateReviewDto.setReviewId(999L);

        mockMvc.perform(put("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReviewDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReview_ShouldReturnNoContent() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(delete("/reviews/{id}", reviewId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReview_ShouldReturnReview() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(reviewId))
                .andExpect(jsonPath("$.content").value("Good movie!"))
                .andExpect(jsonPath("$.userId").value(userId1))
                .andExpect(jsonPath("$.filmId").value(filmId1));
    }

    @Test
    void getReview_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/reviews/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReviewList_WithoutParams_ShouldReturnAllReviews() throws Exception {
        createReview("Review 1", true, userId1, filmId1);
        createReview("Review 2", true, userId2, filmId1);

        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content", is("Review 1")))
                .andExpect(jsonPath("$[1].content", is("Review 2")));
    }

    @Test
    void getReviewList_WithFilmId_ShouldReturnFilmReviews() throws Exception {
        createReview("Film 1 Review", true, userId1, filmId1);
        createReview("Film 2 Review", true, userId2, filmId2);

        mockMvc.perform(get("/reviews")
                        .param("filmId", filmId1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("Film 1 Review")));
    }

    @Test
    void getReviewList_WithCount_ShouldReturnLimitedReviews() throws Exception {
        createReview("Review 1", true, userId1, filmId1);
        createReview("Review 2", true, userId2, filmId1);
        createReview("Review 3", true, userId3, filmId1);

        mockMvc.perform(get("/reviews")
                        .param("count", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void addLike_ShouldIncreaseUseful() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(1));
    }

    @Test
    void addDislike_ShouldDecreaseUseful() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(put("/reviews/{id}/dislike/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(-1));
    }

    @Test
    void changeLikeToDislike_ShouldUpdateUseful() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(put("/reviews/{id}/dislike/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(-1));
    }

    @Test
    void deleteLike_ShouldDecreaseUseful() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(0));
    }

    @Test
    void addDuplicateLike_ShouldReturnBadRequest() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteNonExistentVote_ShouldReturnNotFound() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(delete("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isNotFound());
    }

    @Test
    void addLikeWithWrongVoteType_ShouldReturnBadRequest() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/reviews/{id}/dislike/{userId}", reviewId, userId2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void multipleUsersVoting_ShouldCalculateCorrectUseful() throws Exception {
        Long reviewId = createReview("Good movie!", true, userId1, filmId1);

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, userId3))
                .andExpect(status().isOk());

        mockMvc.perform(put("/reviews/{id}/dislike/{userId}", reviewId, userId4))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(1));
    }
}