package com.lockedin.config;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions from any controller into RFC 7807 problem details (docs/API.md, "Errors").
 * {@code detail} is always a sentence the frontend can show the user as-is.
 *
 * <p>For 404 and 409, throw {@code new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found")}:
 * the base class already turns it into a problem whose {@code detail} is the reason.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String INVALID_REQUEST = "The request is not valid";
    static final String UNREADABLE_BODY = "The request body could not be read";
    static final String NOT_FOUND = "This item does not exist";
    static final String SERVER_ERROR = "Something went wrong on the server";

    /** Rules that mean "this field is missing"; their message wins over any other rule on the same field. */
    private static final Set<String> REQUIRED_RULES = Set.of("NotNull", "NotBlank", "NotEmpty");

    /**
     * Bean Validation reports a field's broken rules in no fixed order. Required rules come first,
     * then the rule name alphabetically, so the user always sees the same message.
     */
    private static final Comparator<FieldError> MESSAGE_PRIORITY = Comparator
        .comparing((FieldError e) -> !REQUIRED_RULES.contains(e.getCode()))
        .thenComparing(FieldError::getCode, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * A {@code @Valid} request body failed: 400, the first field's message as {@code detail}, all of them in
     * {@code errors}. A class-level rule (no field) only supplies {@code detail}.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().stream()
            .sorted(MESSAGE_PRIORITY)
            .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String detail = errors.values().stream().findFirst()
            .or(() -> ex.getBindingResult().getGlobalErrors().stream().map(ObjectError::getDefaultMessage).findFirst())
            .orElse(INVALID_REQUEST);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return ResponseEntity.badRequest().body(problem);
    }

    /** Malformed JSON, or a value of the wrong type (e.g. {@code "reps": "ten"}): 400. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, UNREADABLE_BODY);
    }

    /**
     * A path or query parameter that can't be converted. A malformed id in the path can't name anything,
     * so it's a 404 like any other missing resource; anything else (e.g. an unknown {@code ?type=}) is a 400.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            if (mismatch.getParameter().hasParameterAnnotation(PathVariable.class)) {
                return problem(HttpStatus.NOT_FOUND, NOT_FOUND);
            }
            return problem(HttpStatus.BAD_REQUEST, "'%s' is not a valid %s".formatted(mismatch.getValue(), mismatch.getName()));
        }
        return problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST);
    }

    /**
     * Anything no other handler claims: 500 with a generic message, so internals never reach the user.
     * Spring Security's exceptions are rethrown so its filters can still answer 401/403.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) throws Exception {
        if (ex instanceof AuthenticationException || ex instanceof AccessDeniedException) {
            throw ex;
        }
        logger.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR);
    }

    private static ResponseEntity<Object> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
