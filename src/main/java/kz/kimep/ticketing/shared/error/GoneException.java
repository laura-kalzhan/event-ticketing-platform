package kz.kimep.ticketing.shared.error;

import org.springframework.http.HttpStatus;

public class GoneException extends ApiException {

    public GoneException(String message) {
        super(HttpStatus.GONE, message);
    }
}
