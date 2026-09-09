package com.bank.mock.api;

import com.bank.mock.dto.BeneficiaryRequest;
import com.bank.mock.dto.BeneficiaryResponse;
import com.npst.observability.aop.LogRegistry;
import com.npst.observability.context.AuditContext;
import com.npst.observability.util.MaskingUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Beneficiary management from the PRD, US-09.
 *
 * <p>This is the platform's masking gate. The request carries an OTP and a full
 * account number, {@code logArguments} is on, and the requirement is that
 * neither value appears in the log file, in Loki, or in MySQL. If masking ever
 * regresses, this endpoint is where it shows.
 */
@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {

    private final Duration coolingOff;

    public BeneficiaryController(
            @Value("${mock.beneficiary.cooling-off:PT30M}") Duration coolingOff) {
        this.coolingOff = coolingOff;
    }

    /**
     * Adding a beneficiary is audited: it changes who the customer can send
     * money to, which is exactly the kind of change a regulator asks about.
     */
    @PostMapping
    @LogRegistry(action = "ADD_BENEFICIARY", module = "PAYMENTS", entity = "BENEFICIARY",
            audit = true, logArguments = true)
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody BeneficiaryRequest request) {

        String beneficiaryId = "BEN" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        // What only this method knows. The mobile number is stored unmasked in
        // the audit table and masked in the log file - two audiences, two rules.
        AuditContext.entityId(beneficiaryId);
        AuditContext.businessRef(beneficiaryId);
        AuditContext.mobileNumber(request.mobile());
        AuditContext.put("ifsc", request.ifsc());
        AuditContext.afterState(java.util.Map.of(
                "beneficiaryName", request.name(),
                "accountNumber", request.accountNumber(),
                "status", "PENDING_ACTIVATION"));

        return ResponseEntity.status(HttpStatus.CREATED).body(new BeneficiaryResponse(
                beneficiaryId,
                request.name(),
                MaskingUtil.maskAccountNumber(request.accountNumber()),
                "PENDING_ACTIVATION",
                Instant.now().plus(coolingOff)));
    }
}
