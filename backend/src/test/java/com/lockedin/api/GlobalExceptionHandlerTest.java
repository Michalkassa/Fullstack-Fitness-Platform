package com.lockedin.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.lockedin.config.SecurityConfig;
import com.lockedin.models.Workout;

/**
 * Error responses must follow docs/API.md, "Errors": RFC 7807 problem details whose
 * {@code detail} is a sentence the frontend can show as-is.
 */
@WebMvcTest(GlobalExceptionHandlerTest.ErrorTestController.class)
@WithMockUser // the test routes aren't public; signing in keeps the focus on the handler
@Import({SecurityConfig.class, GlobalExceptionHandlerTest.ErrorTestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    record SampleRequest(
        @NotBlank(message = "Enter a name and a description")
        @Size(max = 40, message = "Name must be at most 40 characters") String name,
        @Max(value = 36, message = "At most 36 reps per set") Integer reps) {
    }

    @RestController
    static class ErrorTestController {

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody SampleRequest body) {
        }

        @GetMapping("/test/items/{id}")
        void item(@PathVariable UUID id) {
        }

        @GetMapping("/test/chart")
        void chart(@RequestParam ChartType type) {
        }

        @GetMapping("/test/missing")
        void missing() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("connection pool exhausted");
        }

        @GetMapping("/test/denied")
        void denied() {
            throw new AccessDeniedException("nope");
        }
    }

    enum ChartType { VOLUME, WEIGHT }

    @Test
    void invalidBodyIsA400ProblemWithTheFieldMessage() throws Exception {
        mvc.perform(post("/test/validated").contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Bench", "reps": 40}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("At most 36 reps per set"))
            .andExpect(jsonPath("$.errors.reps").value("At most 36 reps per set"));
    }

    @Test
    void aBlankNameThatIsAlsoTooLongReportsTheRequiredMessage() throws Exception {
        String fortyOneSpaces = " ".repeat(41);

        mvc.perform(post("/test/validated").contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "%s"}
                """.formatted(fortyOneSpaces)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Enter a name and a description"))
            .andExpect(jsonPath("$.errors.name").value("Enter a name and a description"));
    }

    @Test
    void requiredRulesWinWhateverOrderTheValidatorReportsThemIn() throws Exception {
        var result = new BeanPropertyBindingResult(new SampleRequest(" ".repeat(41), null), "sampleRequest");
        result.addError(fieldError("name", "Size", "Name must be at most 40 characters"));
        result.addError(fieldError("name", "NotBlank", "Enter a name and a description"));
        result.addError(fieldError("reps", "Positive", "Reps must be positive"));
        result.addError(fieldError("reps", "Max", "At most 36 reps per set"));

        ProblemDetail problem = handle(result);

        assertThat(problem.getDetail()).isEqualTo("Enter a name and a description");
        assertThat(problem.getProperties()).containsEntry("errors", Map.of(
            "name", "Enter a name and a description",
            // No required rule broken: ties go to the rule name in alphabetical order.
            "reps", "At most 36 reps per set"));
    }

    @Test
    void aClassLevelRuleBecomesTheDetailWithoutAnErrorsMap() throws Exception {
        var result = new BeanPropertyBindingResult(new SampleRequest("Bench", 5), "sampleRequest");
        result.addError(new ObjectError("sampleRequest", "Enter a weight and the reps"));

        ProblemDetail problem = handle(result);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Enter a weight and the reps");
        assertThat(problem.getProperties()).isNull();
    }

    @Test
    void malformedJsonIsA400Problem() throws Exception {
        mvc.perform(post("/test/validated").contentType(MediaType.APPLICATION_JSON).content("{\"reps\": \"ten\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.UNREADABLE_BODY));
    }

    @Test
    void aMalformedIdInThePathIsA404() throws Exception {
        mvc.perform(get("/test/items/not-a-uuid"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.NOT_FOUND));
    }

    @Test
    void anUnknownQueryValueIsA400NamingIt() throws Exception {
        mvc.perform(get("/test/chart").param("type", "calories"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("'calories' is not a valid type"));
    }

    @Test
    void responseStatusExceptionsKeepTheirStatusAndReason() throws Exception {
        mvc.perform(get("/test/missing"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Workout not found"));
        mvc.perform(get("/test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
    }

    @Test
    void anUnexpectedExceptionIsA500ThatHidesTheCause() throws Exception {
        mvc.perform(get("/test/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.SERVER_ERROR))
            .andExpect(content().string(not(containsString("connection pool"))));
    }

    @Test
    void securityExceptionsAreLeftToSpringSecurity() throws Exception {
        mvc.perform(get("/test/denied"))
            .andExpect(status().isForbidden());
    }

    private static FieldError fieldError(String field, String rule, String message) {
        return new FieldError("sampleRequest", field, null, false, new String[] {rule}, null, message);
    }

    private static ProblemDetail handle(BeanPropertyBindingResult result) throws NoSuchMethodException {
        var parameter = new MethodParameter(
            ErrorTestController.class.getDeclaredMethod("validated", SampleRequest.class), 0);
        var response = new GlobalExceptionHandler().handleMethodArgumentNotValid(
            new MethodArgumentNotValidException(parameter, result), new HttpHeaders(), HttpStatus.BAD_REQUEST, null);
        return (ProblemDetail) response.getBody();
    }
}
