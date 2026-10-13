package com.example.demo.service;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /**
     * Protected by Resilience Circuit Breaker ("paymentServiceCB").
     *
     * @param request  the payment payload
     * @param shouldFail  If true, simulates a downstream failure (e.g. 3rd-party Payment Gateway down)
     * return PaymentResponse
     */

    @CircuitBreaker(name = "paymentServiceCB", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest paymentRequest, boolean shouldFail){

        log.info("--> [CALLING PAYMENT GATEWAY] Processing transaction: {}", paymentRequest.getTransactionId());

        if(shouldFail){
            log.error("--> [GATEWAY ERROR] Downstream payment gateway is unreachable or returned error 500!");
            throw new RuntimeException("Downstream Payment Gateway connection timeout / internal error");
        }

        log.info("--> [SUCCESS] Payment proceeded successfully for transaction: {}", paymentRequest.getTransactionId());
        return new PaymentResponse("SUCCESS", "Payment of $"+paymentRequest.getAmount()+" processed successfully.");

    }

    /**
     * Fallback method executed when:
     *  1. An exception occurs in processPayment() while Circuit is CLOSED.
     *  2. Or instantly without invoking processPayment() when Circuit is OPEN.
     */

    public PaymentResponse paymentFallback(PaymentRequest paymentRequest, boolean shouldFail, Throwable throwable){

        log.warn("--> [CIRCUIT BREAKER FALLBACK] Invoked for txn: {}. Reason: {}",paymentRequest.getTransactionId(),throwable.getMessage());
        return new PaymentResponse("FALLBCK",
                "Payment service is currently unavailable. Your request [" + paymentRequest.getTransactionId()+
                        "] has been handled safely by fallback. Underlying issue: "+throwable.getMessage());

    }

}
