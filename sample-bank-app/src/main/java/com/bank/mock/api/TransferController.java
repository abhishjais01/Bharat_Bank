package com.bank.mock.api;

import com.bank.mock.cbs.CbsClient;
import com.bank.mock.dto.TransferRequest;
import com.bank.mock.dto.TransferResponse;
import com.bank.mock.exception.InsufficientFundsException;
import com.bank.mock.exception.InvalidBeneficiaryException;
import com.bank.mock.exception.LimitExceededException;
import com.npst.observability.aop.LogRegistry;
import com.npst.observability.context.AuditContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

// fund transfer endpoints
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private static final String UNKNOWN_BENEFICIARY = "000000000000";

    private final CbsClient cbs;
    private final BigDecimal perTransactionLimit;

    public TransferController(CbsClient cbs,
                              @Value("${mock.transfer.limit:200000.00}") BigDecimal limit) {
        this.cbs = cbs;
        this.perTransactionLimit = limit;
    }

    // IMPS transfer; every outcome is logged and audited
    @PostMapping("/imps")
    @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", entity = "TRANSFER",
            audit = true, logArguments = true,
            warnOn = {InsufficientFundsException.class,
                    LimitExceededException.class,
                    InvalidBeneficiaryException.class})
    public ResponseEntity<TransferResponse> imps(@Valid @RequestBody TransferRequest request,
                                                 @RequestParam(required = false) String simulate) {

        // transaction reference
        String reference = "IMPS" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        // audit details, set before any check so failed attempts are recorded too
        AuditContext.amount(request.amount(), "INR");
        AuditContext.businessRef(reference);
        AuditContext.entityId(reference);
        AuditContext.put("transferType", "IMPS");
        AuditContext.put("beneficiaryAccount", request.beneficiaryAccount());

        // business checks: these throw and are logged as WARN
        if (UNKNOWN_BENEFICIARY.equals(request.beneficiaryAccount())) {
            throw new InvalidBeneficiaryException(
                    "Beneficiary account is not registered or is still in its cooling-off period");
        }

        if (request.amount().compareTo(perTransactionLimit) > 0) {
            throw new LimitExceededException(
                    "Amount " + request.amount() + " exceeds the per-transaction limit of "
                            + perTransactionLimit);
        }

        // simulated pending transfer
        if ("PENDING".equals(simulate)) {
            return ResponseEntity.accepted().body(new TransferResponse(
                    reference, null, "PENDING", request.amount(), "INR",
                    "Transfer initiated. You will be notified once it completes."));
        }

        // debit the account in CBS
        String cbsReference = cbs.debit(request.debitAccount(), request.amount(), simulate);

        // success: add the CBS reference and return 201
        AuditContext.put("cbsReference", cbsReference);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(
                reference, cbsReference, "SUCCESS", request.amount(), "INR",
                "Transfer completed successfully"));
    }
}
