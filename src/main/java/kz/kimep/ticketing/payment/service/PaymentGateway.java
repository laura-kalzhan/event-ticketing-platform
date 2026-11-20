package kz.kimep.ticketing.payment.service;

public interface PaymentGateway {

    GatewayResult charge(ChargeRequest request);
}
