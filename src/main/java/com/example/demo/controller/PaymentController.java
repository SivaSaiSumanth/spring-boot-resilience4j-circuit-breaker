package com.example.demo.controller;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     *
     * Endpoint to simulate payments.
     * Pass ?fail=true to trigger simulated failures.
     */

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest paymentRequest,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail) {

        PaymentResponse response = paymentService.processPayment(paymentRequest, fail);
        return ResponseEntity.ok(response);

    }

}
