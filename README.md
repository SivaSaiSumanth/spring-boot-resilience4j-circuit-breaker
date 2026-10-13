# Spring Boot Resilience4j Circuit Breaker Demo

This project demonstrates how to implement and test the **Circuit Breaker Pattern** using **Spring Boot 3**, **Resilience4j**, and **Spring Boot Actuator**.

The demo simulates payment gateway failures and shows how the circuit breaker prevents repeated calls to an unavailable downstream service.

---

## 1. Circuit Breaker Configuration (`application.yml`)

The circuit breaker is configured using the instance name `paymentServiceCB`.

| Configuration | Value | Description |
|---|---|---|
| Sliding Window Type | `COUNT_BASED` | Counts calls within a fixed-size window |
| Sliding Window Size | `10` | Tracks the last 10 calls |
| Minimum Number of Calls | `10` | Evaluates the failure rate after at least 10 calls |
| Failure Rate Threshold | `50%` | Opens the circuit when the failure rate reaches or exceeds 50% |
| Slow Call Rate Threshold | `75%` | Opens the circuit if the slow-call rate reaches or exceeds 75% |
| Slow Call Duration Threshold | `2s` | Calls taking at least this duration are considered slow |
| Wait Duration in Open State | `60s` | Keeps the circuit OPEN for 60 seconds before allowing recovery testing |
| Permitted Calls in Half-Open State | `3` | Allows up to 3 probe calls to evaluate recovery |
| Automatic Transition | `true` | Automatically transitions from OPEN to HALF_OPEN after the wait duration |

### Circuit Breaker States

- **CLOSED:** Requests are allowed. Failures are recorded and evaluated.
- **OPEN:** Requests are rejected immediately by the circuit breaker, and the fallback method handles the rejection.
- **HALF_OPEN:** A limited number of probe calls are allowed to determine whether the downstream service has recovered.

### Expected State Transitions

`CLOSED → OPEN → HALF_OPEN → CLOSED`

If the permitted probe calls fail enough to exceed the configured failure threshold, the circuit transitions back to OPEN.

---

## 2. How to Run

From the project directory, execute:

```bash
mvn spring-boot:run
```

Alternatively, run `CircuitBreakerDemoApplication.java` from IntelliJ IDEA or Eclipse.

The application starts on port `8080`.

---

## 3. Testing with Postman

Use the following settings for all payment requests:

- **Method:** `POST`
- **Content-Type:** `application/json`
- **Base URL:** `http://localhost:8080/api/payments/process`

### Step 1: Normal Success Calls — CLOSED State

Send a request without the `fail` query parameter.

**URL:**

```text
http://localhost:8080/api/payments/process
```

**Body — Raw → JSON:**

```json
{
  "transactionId": "TXN-001",
  "amount": 250.00
}
```

**Expected response:**

```json
{
  "status": "SUCCESS",
  "message": "Payment of $250.0 processed successfully."
}
```

The payment service executes normally because the circuit is CLOSED.

---

### Step 2: Trigger Failures — Transition to OPEN

Send requests with the query parameter `fail=true`.

**URL:**

```text
http://localhost:8080/api/payments/process?fail=true
```

**Body:**

```json
{
  "transactionId": "TXN-002",
  "amount": 100.00
}
```

**Testing procedure:**

1. Restart the application to begin with a fresh circuit-breaker state.
2. Send 10 requests with `fail=true`.
3. Each request simulates a downstream payment gateway failure.
4. The service throws a `RuntimeException`, which Resilience4j records as a failure.
5. Once 10 calls have been recorded, the failure rate is evaluated.

If all 10 calls fail, the failure rate is 100%, exceeding the configured 50% threshold.

**Expected circuit state:** `OPEN`

Check the state using:

```text
GET http://localhost:8080/actuator/circuitbreakers
```

---

### Step 3: Verify the OPEN State

Immediately after confirming that the circuit is OPEN, send a normal request without `fail=true`.

**URL:**

```text
http://localhost:8080/api/payments/process
```

**Body:**

```json
{
  "transactionId": "TXN-003",
  "amount": 250.00
}
```

**Expected response:**

The response should have status `FALLBCK` and a message indicating that the payment service is unavailable because `paymentServiceCB` is OPEN.

The exact message depends on the exception supplied to the fallback method.

**Important observations:**

- The protected payment-processing method should not execute for a request rejected by an OPEN circuit.
- The fallback method returns a response instead of processing the payment.
- The circuit remains OPEN during the configured 60-second waiting period.

Repeat with a few normal requests during this period. They should continue to return the fallback response.

---

### Step 4: Verify HALF_OPEN and Recovery

The circuit is configured with:

```yaml
wait-duration-in-open-state: 60s
permitted-number-of-calls-in-half-open-state: 3
automatic-transition-from-open-to-half-open-enabled: true
```

**Testing procedure:**

1. Confirm that the circuit is OPEN.
2. Wait approximately 60 seconds.
3. Check the circuit state again. It should transition to `HALF_OPEN`.
4. Send successful requests without the `fail` query parameter.
5. Check the state after each probe request.

**Expected behavior:**

- Up to 3 probe calls are permitted in HALF_OPEN.
- Successful probe calls are recorded as successes.
- If the recovery criteria are satisfied, the circuit transitions to CLOSED.
- If the probe results exceed the configured failure threshold, the circuit transitions back to OPEN.

The state transition is based on the recorded results of the permitted calls. The circuit does not simply close because the third request was sent.

---

## 4. Monitoring Circuit Breaker State with Actuator

Spring Boot Actuator exposes endpoints for monitoring the circuit breaker.

### A. Health Endpoint

**Request:**

```text
GET http://localhost:8080/actuator/health
```

Displays application health and, when configured and available, circuit-breaker health details.

### B. Circuit Breakers Endpoint

**Request:**

```text
GET http://localhost:8080/actuator/circuitbreakers
```

Lists circuit-breaker instances and their metrics, including:

- Current state
- Failure rate
- Failure rate threshold
- Number of buffered calls
- Number of failed calls
- Number of calls rejected while OPEN

### C. Circuit Breaker Events Endpoint

**Request:**

```text
GET http://localhost:8080/actuator/circuitbreakerevents
```

Displays recorded circuit-breaker events, such as state transitions and call failures.

---

## 5. Quick Testing Checklist

- [ ] Start the application and verify normal payment processing.
- [ ] Send 10 failing requests using `?fail=true`.
- [ ] Confirm the circuit transitions to `OPEN`.
- [ ] Send a normal request while OPEN and verify the fallback response.
- [ ] Confirm the circuit remains OPEN during the 60-second waiting period.
- [ ] Wait for the circuit to transition to `HALF_OPEN`.
- [ ] Send successful probe requests and monitor the state.
- [ ] Confirm the circuit transitions back to `CLOSED` after successful recovery.

---

## 6. Key Interview Takeaway

The Circuit Breaker Pattern prevents repeated calls to a failing dependency. Resilience4j monitors calls, evaluates configured failure thresholds, rejects requests while OPEN, and permits a limited number of recovery probes in HALF_OPEN.

This helps improve application resilience and prevents an unhealthy downstream service from repeatedly affecting the calling application.
