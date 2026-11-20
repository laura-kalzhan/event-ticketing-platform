package kz.kimep.ticketing.payment.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final String DECLINE_PREFIX = "decline";

    @Override
    public GatewayResult charge(ChargeRequest request) {
        if (request.cardToken().startsWith(DECLINE_PREFIX)) {
            return GatewayResult.declined("CARD_DECLINED");
        }
        return GatewayResult.approved("SIM-" + UUID.randomUUID());
    }
}
