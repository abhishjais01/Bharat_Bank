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

// beneficiary endpoints
@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {

    private final Duration coolingOff;

    public BeneficiaryController(
            @Value("${mock.beneficiary.cooling-off:PT30M}") Duration coolingOff) {
        this.coolingOff = coolingOff;
    }

    // add a beneficiary; logged and audited
    @PostMapping
    @LogRegistry(action = "ADD_BENEFICIARY", module = "PAYMENTS", entity = "BENEFICIARY",
            audit = true, logArguments = true)
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody BeneficiaryRequest request) {

        // new id for the beneficiary
        String beneficiaryId = "BEN" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        // details for the audit record (masked before storing)
        AuditContext.entityId(beneficiaryId);
        AuditContext.businessRef(beneficiaryId);
        AuditContext.mobileNumber(request.mobile());
        AuditContext.put("ifsc", request.ifsc());
        AuditContext.afterState(java.util.Map.of(
                "beneficiaryName", request.name(),
                "accountNumber", request.accountNumber(),
                "status", "PENDING_ACTIVATION"));

        // the response shows only the masked account number
        return ResponseEntity.status(HttpStatus.CREATED).body(new BeneficiaryResponse(
                beneficiaryId,
                request.name(),
                MaskingUtil.maskAccountNumber(request.accountNumber()),
                "PENDING_ACTIVATION",
                Instant.now().plus(coolingOff)));
    }
}
