package kz.kimep.ticketing.identity.api;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {

    @Override
    public String toString() {
        return "RefreshRequest[hidden]";
    }
}
