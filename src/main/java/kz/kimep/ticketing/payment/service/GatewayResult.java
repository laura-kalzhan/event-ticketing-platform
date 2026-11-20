package kz.kimep.ticketing.payment.service;

public record GatewayResult(boolean approved, String transactionId, String failureCode) {

    public static GatewayResult approved(String transactionId) {
        return new GatewayResult(true, transactionId, null);
    }

    public static GatewayResult declined(String failureCode) {
        return new GatewayResult(false, null, failureCode);
    }
}
