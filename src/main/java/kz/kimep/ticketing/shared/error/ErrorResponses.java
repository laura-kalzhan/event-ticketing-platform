package kz.kimep.ticketing.shared.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErrorResponses {

    private final ObjectMapper objectMapper;

    public static ApiError build(HttpStatus status, String message, String path, List<ApiError.FieldViolation> fields) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fields);
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), build(status, message, request.getRequestURI(), List.of()));
    }
}
