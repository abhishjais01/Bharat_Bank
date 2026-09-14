# Integrating with the Observability Platform

> For a team building a Bharat Bank microservice. Spring Boot takes a
> dependency, a few YAML lines and a Logback include. NestJS takes about fifty
> lines you own, because the backend is shared but the *producer* is yours.
>
> The code is the source of truth; this guide was corrected against it on
> 2026-09-13.

---

## Contents

1. [What you get](#1-what-you-get)
2. [Spring Boot](#2-spring-boot)
3. [NestJS and anything else](#3-nestjs-and-anything-else)
4. [The headers your gateway must send](#4-the-headers-your-gateway-must-send)
5. [What is masked, and what is not](#5-what-is-masked-and-what-is-not)
6. [Searching logs](#6-searching-logs)
7. [Configuration reference](#7-configuration-reference)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. What you get

Your service writes **no logging code**. You annotate a method:

```java
@LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", audit = true)
public TransferResponse transfer(TransferRequest request) { ... }
```

and the platform records this:

```json
{
  "traceId":     "a4f1c9e2-...",        ← from the gateway, or minted here
  "bankCode":    "NPST",
  "environment": "DEV",
  "service":     "accounts-service",    ← your spring.application.name
  "channel":     "MOBILE",              ← from X-Channel
  "deviceId":    "pixel-8-abc123",
  "ipAddress":   "49.36.180.22",        ← real client, not the load balancer
  "customerId":  "CIF-99001",
  "level":       "INFO",
  "message":     "FUND_TRANSFER completed",
  "metadata": {
    "action": "FUND_TRANSFER", "module": "PAYMENTS",
    "durationMs": 118, "outcome": "SUCCESS", "statusCode": 201
  }
}
```

Two guarantees worth knowing before you rely on it:

- **Logging cannot slow your service down.** The record leaves on a background
  thread with a bounded queue. With the platform completely down, a request
  still returns in single-digit milliseconds.
- **Logging cannot break your service.** A failure in the platform never
  propagates to your caller, and never changes what your method returns or
  throws.

---

## 2. Spring Boot

### Step 1 - the dependency

```xml
<dependency>
    <groupId>com.npst.observability</groupId>
    <artifactId>observability-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2 - configuration

```yaml
observability:
  environment: ${OBSERVABILITY_ENVIRONMENT:DEV}
  bank:
    code: ${OBSERVABILITY_BANK_CODE:NPST}
  sink:
    endpoint: ${OBSERVABILITY_LOGGING_ENDPOINT:http://logging-api:8090/api/v1/logs}
    audit-endpoint: ${OBSERVABILITY_AUDIT_ENDPOINT:http://logging-api:8090/api/v1/audit}
```

There is deliberately no `observability.service` - it defaults to
`spring.application.name`, so your service names itself once.

`audit-endpoint` is only needed if you use `audit = true` - but without it,
audit records are silently not sent. For the pipeline counters in Prometheus,
also add `io.micrometer:micrometer-registry-prometheus`, expose
`management.endpoints.web.exposure.include: health,prometheus`, and add your
service as a target in `infrastructure/prometheus.yml`.

### Step 3 - logging

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <include resource="observability-logback.xml"/>
</configuration>
```

This gives you the console appender and a rolling JSON file named after your
service, which Promtail ships to Loki.

### Step 4 - annotate what matters

```java
@PostMapping("/imps")
@LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", entity = "TRANSFER",
        audit = true, logArguments = true,
        warnOn = {InsufficientFundsException.class, LimitExceededException.class})
public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {

    AuditContext.amount(request.amount(), "INR");
    AuditContext.businessRef(reference);

    return ResponseEntity.status(CREATED).body(...);
}
```

| Attribute | Use it for |
|---|---|
| `action`, `module` | What the operation is, and which domain owns it |
| `entity` | The record type acted on |
| `audit = true` | Money movement, credential changes, anything a regulator asks about. **Not** balance enquiries |
| `warnOn` | Business rejections. These log at WARN; everything else that throws logs at ERROR |
| `logArguments` | When the arguments help diagnose. They are masked first |
| `level` | Severity for the success path, default INFO |

**`AuditContext`** is how a method contributes what only it knows - the amount,
the reference, the entity id, before/after state. It is cleared automatically,
including when your method throws.

### When you need to log directly

The annotation does not cover everything. Inject `CommonLogger`:

```java
commonLogger.logApplication(LogLevel.WARN, "Retrying CBS call",
        Map.of("attempt", 2, "reason", "timeout"));
```

### Things to know

- **Return `ResponseEntity`** if you want `statusCode` recorded. From anything
  else the status is not settled when the aspect runs, so it is left null
  rather than guessed.
- **Spring AOP is proxy based.** A call from one method of a bean to another
  method *on the same bean* does not pass through the proxy and is not
  intercepted. Annotate the method called from outside.
- **Validation and security failures are not logged by the annotation.** A
  request that fails `@Valid`, or is rejected by a security filter, never
  reaches your method, so `@LogRegistry` never sees it - no application log, no
  audit. Log those explicitly if you need them (failed logins and OTP attempts
  are a PRD requirement).
- **Jobs and listeners need their own trace id.** Outside an HTTP request MDC
  is empty, the record goes out with `traceId: null`, and logging-api rejects it
  with 400. Put an id into MDC for each run:
  ```java
  MDC.put("traceId", "JOB-" + UUID.randomUUID());
  try { ... } finally { MDC.remove("traceId"); }
  ```
- **Log degraded answers yourself.** A Resilience4j fallback returns normally,
  so the annotation records SUCCESS at INFO. Add
  `commonLogger.logApplication(LogLevel.WARN, "CBS fallback used", ...)`.
- **Build HTTP clients so the trace travels.** Use `RestTemplateBuilder`,
  OpenFeign, or a WebClient with the `observabilityWebClientTraceFilter` bean. A
  plain `new RestTemplate()` does not propagate `X-Trace-Id`.

---

## 3. NestJS and anything else

`logging-api` is HTTP and JSON. Nothing about it is Java-specific, so there is
no second backend and no separate module for NestJS. What your service owns is
the **producer**: enriching the payload and posting it.

### The contract

```
POST http://logging-api:8090/api/v1/logs
Content-Type: application/json
```

```json
{
  "schemaVersion": "1.0",
  "traceId":       "a4f1c9e2-...",
  "bankCode":      "NPST",
  "environment":   "DEV",
  "service":       "bill-payment-service",
  "eventType":     "APPLICATION",
  "level":         "INFO",
  "message":       "Biller registered successfully",
  "timestamp":     "2026-09-10T06:24:33.542Z",
  "channel":       "MOBILE",
  "deviceId":      "pixel-8-abc123",
  "ipAddress":     "49.36.180.22",
  "customerId":    "CIF-99001",
  "metadata":      { "billerId": "MSEB", "module": "BILLPAY" }
}
```

**Required**: `schemaVersion`, `traceId`, `bankCode`, `environment`, `service`,
`eventType`, `level`, `message`, `timestamp`. Miss one and you get a 400 naming
exactly which.

| Response | Meaning | What to do |
|---|---|---|
| 201 | Stored | Nothing |
| 400 | Malformed | Fix the producer; retrying will not help |
| 422 | Valid, but not an APPLICATION event | Fix your configuration |
| 500 | Our fault | Retry |

`timestamp` must be **ISO-8601**, not an epoch number.

### A NestJS producer sketch

> There is no NestJS code in this repository, so this sketch has not been run.
> It mirrors the Java starter's behaviour: bounded queue, drop-oldest, short
> timeouts, never throw, mask before anything is written.

```typescript
// observability.service.ts
import { Injectable, Logger } from '@nestjs/common';
import { randomUUID } from 'crypto';

// Entries are lower-case with '_' and '-' removed, because keys are normalised
// the same way before comparison. Mirrors the Java defaults in
// ObservabilityProperties.Masking.
const REDACT = new Set([
  'otp', 'mpin', 'tpin', 'pin', 'atmpin', 'cardpin',
  'password', 'passwd', 'pwd', 'newpassword', 'oldpassword',
  'token', 'accesstoken', 'refreshtoken', 'idtoken',
  'authorization', 'secret', 'clientsecret', 'apikey',
  'cvv', 'cvv2', 'biometric', 'devicesignature',
]);
const MASK = new Set([
  'accountnumber', 'accountno', 'account', 'beneficiaryaccount', 'debitaccount',
  'cardnumber', 'cardno', 'card', 'pan', 'aadhaar', 'aadhar',
  'mobile', 'mobilenumber', 'phone', 'phonenumber', 'email', 'emailid',
]);

@Injectable()
export class ObservabilityService {
  private readonly logger = new Logger(ObservabilityService.name);
  private readonly queue: object[] = [];
  private draining = false;

  constructor() {
    // Never await the send on the request path, and never let the queue grow
    // without bound - same reasoning as the Java sink.
    setInterval(() => this.drain(), 1000).unref();
  }

  log(level: string, message: string, metadata: Record<string, unknown> = {}) {
    const store = requestContext() ?? {};    // your AsyncLocalStorage store

    if (this.queue.length >= 10_000) this.queue.shift();   // drop oldest

    this.queue.push({
      schemaVersion: '1.0',
      // Required by logging-api. Outside a request (jobs, consumers) mint one,
      // or the record is rejected with 400.
      traceId:     store.traceId ?? randomUUID(),
      bankCode:    process.env.OBSERVABILITY_BANK_CODE ?? 'NPST',
      environment: process.env.OBSERVABILITY_ENVIRONMENT ?? 'DEV',
      service:     process.env.SERVICE_NAME,
      eventType:   'APPLICATION',
      level,
      message,
      timestamp:   new Date().toISOString(),
      channel:     store.channel,
      deviceId:    store.deviceId,
      ipAddress:   store.ipAddress,
      customerId:  store.customerId,
      metadata:    mask(metadata),
    });
  }

  private async drain() {
    if (this.draining) return;               // one drain loop at a time
    this.draining = true;
    try {
      while (this.queue.length) {
        const entry = this.queue.shift();
        try {
          const response = await fetch(`${process.env.OBSERVABILITY_LOGGING_ENDPOINT}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(entry),
            signal: AbortSignal.timeout(1000),
          });
          if (!response.ok) {
            this.logger.warn(`logging-api rejected log: HTTP ${response.status}`);
          }
        } catch (error) {
          // Log it, never throw it. A logging failure is not a banking failure.
          this.logger.warn(`Failed to ship log: ${error}`);
        }
      }
    } finally {
      this.draining = false;
    }
  }
}

// Mask before anything is written, not just before it is sent.
function mask(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(mask);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, inner]) => {
      const normalised = key.toLowerCase().replace(/[-_]/g, '');
      if (REDACT.has(normalised)) return [key, '***REDACTED***'];
      if (MASK.has(normalised) && inner != null) {
        const text = String(inner);
        return [key, 'X'.repeat(Math.max(0, text.length - 4)) + text.slice(-4)];
      }
      return [key, mask(inner)];
    }));
  }
  return value;
}
```

Plus a middleware that reads the headers in section 4 into `AsyncLocalStorage`,
and forwards `X-Trace-Id` on every outbound call.

What this sketch does **not** cover yet:

- **Audit.** Post `AuditIngestRequest`-shaped JSON to `/api/v1/audit`. Required:
  `schemaVersion`, `traceId`, `bankCode`, `environment`, `service`, `actorId`,
  `actorType`, `action`, `entity`, `timestamp`. The planned `audit-outbox` relay
  is the natural sender. For admin actions, `actorType` is `EMPLOYEE` and
  `actorId` comes from the validated Keycloak token, not a header.
- **Loki.** Promtail reads `/var/log/bank/*.log`. Nest's `Logger` writes to
  stdout, so NestJS logs will not appear in Grafana unless the service also
  writes `{service}.log` JSON lines to that volume, or Promtail is given a
  scrape config for container output.
- **Exact Java parity.** Mobile, PAN and email use simpler masks than the Java
  `MaskingUtil`.

---

## 4. The headers your gateway must send

All optional - an internal call or a scheduled job simply carries fewer.

| Header | Example | Notes |
|---|---|---|
| `X-Trace-Id` | `a4f1c9e2-...` | **Generated once at the gateway** and forwarded unchanged. A service only mints one when absent |
| `X-Channel` | `MOBILE` | MOBILE, WEB, BRANCH, ATM, API |
| `X-Device-Id` | `pixel-8-abc123` | Device fingerprint |
| `X-Customer-Id` | `CIF-99001` | The customer the request acts for |
| `X-Forwarded-For` | `49.36.180.22, 10.0.0.1` | First entry wins - behind a balancer the socket address is the balancer |

**Propagating the trace onward is what makes this work.** Spring services get it
free on `RestTemplate` (built via `RestTemplateBuilder`), Feign, and WebClient.
Other stacks must forward `X-Trace-Id` explicitly, or the trace stops at your
service and the journey can no longer be reconstructed.

Only `X-Trace-Id` is forwarded automatically. The context headers are not, so a
downstream service sees no channel, device or customer unless you forward them
yourself.

---

## 5. What is masked, and what is not

Two stores, two rules. This is deliberate.

| | `application_logs` + log file + Loki | `audit_logs` |
|---|---|---|
| OTP, PIN, password, token, CVV | `***REDACTED***` | not written by the annotation - but see the warning below |
| Account, card, PAN, Aadhaar | `XXXXXXXX5510` when the key is in the mask list | **as supplied** in `business_context`, `before_state`, `after_state` |
| Mobile number | `98XXXX3210` | **unmasked** |
| Customer id | as supplied | **unmasked** |
| Retention | 7 days file, 30 days Loki, 90 days MySQL once the purge job is enabled (off by default) | never purged |

> **Warning - the audit copy is not masked at all.** It is taken before masking
> runs, so whatever you put into `AuditContext` (`put`, `beforeState`,
> `afterState`) is stored verbatim. Never put an OTP, PIN or full card number
> there, and treat account numbers there as a policy decision.

An audit trail that cannot identify the customer is not an audit trail - a
regulator asking who moved money cannot work with `XXXXXXXX5510`. An
application log has no such need, and the file feeding Loki has no access
control of its own, so it stays masked.

**Because `audit_logs` holds unmasked identity, it is append-only**: `@Immutable`
in JPA and a database trigger that rejects `UPDATE` and `DELETE` outright. The
INSERT/SELECT-only grant is designed but **not yet applied** - the application
user still holds ALL PRIVILEGES. Each row is also hash
chained to its predecessor, so tampering is detectable even by someone who
drops the triggers.

Add your own sensitive keys. **Setting a list replaces the default list**, so
copy the full defaults from `ObservabilityProperties.Masking` and add to them -
a short list would silently drop keys such as `pin`, `authorization` and
`refreshtoken`:

```yaml
observability:
  masking:
    redact-keys: [otp, mpin, tpin, pin, atmpin, cardpin, password, passwd, pwd,
                  newpassword, oldpassword, token, accesstoken, refreshtoken,
                  idtoken, authorization, secret, clientsecret, apikey, cvv,
                  cvv2, biometric, devicesignature, myCustomSecret]
    mask-keys:   [accountnumber, accountno, account, beneficiaryaccount,
                  cardnumber, cardno, card, pan, aadhaar, aadhar, mobile,
                  mobilenumber, phone, phonenumber, email, emailid, debitaccount]
```

Matching is **exact** on the normalised key - lower-cased with `_` and `-`
removed. So `accountNumber`, `account_number` and `ACCOUNT-NUMBER` all match,
but `pin` does not swallow `shipping`. The flip side: an unlisted name is not
masked at all. The defaults do not include `debitAccount`, so the sample's
`TransferRequest.debitAccount` is logged in full unless you add it as above.
Only `metadata` is masked - not the `message` text, plain SLF4J lines, or
request URIs such as `/api/v1/accounts/{accountNumber}/balance`.

---

## 6. Searching logs

```bash
# The support path: one trace id, the whole customer journey
curl http://localhost:8090/api/v1/logs/a4f1c9e2-...

# Everything this customer's mobile app did, at WARN or above
curl "http://localhost:8090/api/v1/logs?customerId=CIF-99001&channel=MOBILE&level=WARN"

# What did this customer do, per the audit trail
curl "http://localhost:8090/api/v1/audit?customerId=CIF-99001&from=2026-09-01T00:00:00Z"

# Every transfer above a threshold, by reference
curl "http://localhost:8090/api/v1/audit?action=FUND_TRANSFER&businessRef=IMPS21BB3C77"
```

Full contract at **http://localhost:8090/swagger-ui.html**.

In Grafana, the *Layer 1 - Application Logging* dashboard is provisioned
automatically (the Compose stack has not been run yet). Promtail sets the Loki
labels `job`, `bank_code`, `service`, `environment`, `channel`, `event_type`
and `event_level` - the last six only on SDK lines, because they are parsed
from the structured event nested inside `message`. `trace_id` deliberately is
not a label - it is unique per request, and labelling it would create a stream
per request and bring Loki down. It is carried as structured metadata:

```
{job="observability", service="sample-bank-app"} | trace_id="RUN-TXN-OK"
{job="observability"} |= "RUN-TXN-OK"      # also finds plain, non-SDK lines
```

---

## 7. Configuration reference

```yaml
observability:
  enabled: true                    # false contributes no beans at all
  environment: DEV
  service:                         # defaults to spring.application.name

  bank:
    code: NPST
    name: Bharat Bank
    region: IN

  sink:
    enabled: true                  # false = file and Loki only
    endpoint: http://logging-api:8090/api/v1/logs
    audit-endpoint: http://logging-api:8090/api/v1/audit
    connect-timeout: 500ms
    read-timeout: 1s
    async:
      enabled: true                # false ships inline - tests only
      queue-capacity: 10000
      workers: 1
      shutdown-timeout: 5s

  trace:
    header: X-Trace-Id
    mdc-key: traceId

  context:
    channel-header: X-Channel
    device-header: X-Device-Id
    customer-header: X-Customer-Id
    ip-headers: [X-Forwarded-For, X-Real-IP]

  masking:
    enabled: true
    placeholder: "***REDACTED***"

  aop:
    enabled: true                  # false disables @LogRegistry

  exception-handler:
    enabled: false                 # opt-in; off so the SDK never swallows
                                   # your own error handling
```

Every bean is `@ConditionalOnMissingBean`, so you can replace any single piece -
`BankResolver`, `LogSink`, `MetadataMasker` - by declaring your own, without
forking the starter.

---

## 8. Troubleshooting

**No `CommonLogger` bean.** Check `observability.enabled` is not false. The
starter is found through auto-configuration, not component scanning, so your
package name is irrelevant - `com.bank.accounts` works exactly as well as
`com.npst.observability`.

**Logs reach the file but not MySQL.** Check the pipeline counters:

```bash
curl -s localhost:8080/actuator/prometheus | grep observability_logs
```

| Metric | Meaning |
|---|---|
| `submitted` | Handed to the sink |
| `sent` | Accepted by logging-api |
| `failed` | Rejected or unreachable - the WARN in your log names the reason |
| `dropped` | Queue was full. The platform shedding load rather than taking your service down |

**`400 validation failed`.** The response names every missing field. If it lists
`bankCode`, `environment` or `service`, your `observability.*` config is not
being read. If it lists only `traceId`, the record was written outside an HTTP
request - a job or a listener - see "Things to know" in section 2.

**`422 unsupported event type`.** You sent AUDIT or ERROR to `/api/v1/logs`.
Audit goes to `/api/v1/audit`; error events are not persisted in Layer 1.

**`@LogRegistry` does nothing.** Either the method is called from within the
same bean (Spring AOP is proxy based), or `observability.aop.enabled` is false,
or `spring-boot-starter-aop` was excluded, or the request failed `@Valid` or a
security check before the method ran.

**Nothing in Grafana.** Confirm Promtail can see the file - it reads
`/var/log/bank/*.log`, which is the `app_logs` volume. If your service writes
elsewhere, the `logback-spring.xml` include in step 3 is missing.
