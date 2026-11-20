package kz.kimep.ticketing.identity.api;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    @Override
    public String toString() {
        return "TokenResponse[hidden]";
    }
}
