package kz.kimep.ticketing.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import kz.kimep.ticketing.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

    @Test
    void tokensStartingWithDeclineAreDeclined() {
        GatewayResult result = gateway.charge(charge("decline_insufficient_funds"));

        assertThat(result.approved()).isFalse();
        assertThat(result.failureCode()).isEqualTo("CARD_DECLINED");
    }

    @Test
    void otherTokensAreApprovedWithATransactionId() {
        GatewayResult result = gateway.charge(charge("tok_visa"));

        assertThat(result.approved()).isTrue();
        assertThat(result.transactionId()).startsWith("SIM-");
    }

    private ChargeRequest charge(String token) {
        return new ChargeRequest("B-1", new BigDecimal("100"), "KZT", PaymentMethod.CARD, token);
    }
}
