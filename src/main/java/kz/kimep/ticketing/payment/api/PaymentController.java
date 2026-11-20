package kz.kimep.ticketing.payment.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        return paymentService.pay(user, request);
    }
}
