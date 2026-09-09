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

/**
 * Funds transfer from the PRD, US-10.
 *
 * <p>The richest journey in the platform, because it produces every log level
 * the search API needs to be able to distinguish:
 *
 * <ul>
 *   <li>success - INFO, plus an audit record with the amount</li>
 *   <li>insufficient funds, limit exceeded, unknown beneficiary - WARN. The PRD
 *       requires failed transfers to be clearly explained, and these are the
 *       system refusing correctly, not the system breaking</li>
 *   <li>CBS unavailable - ERROR. Something is genuinely wrong</li>
 *   <li>pending - INFO. Shown immediately and reconciled asynchronously, so the
 *       customer is never left on a spinner</li>
 * </ul>
 */
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

    @PostMapping("/imps")
    @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", entity = "TRANSFER",
            audit = true, logArguments = true,
            warnOn = {InsufficientFundsException.class,
                    LimitExceededException.class,
                    InvalidBeneficiaryException.class})
    public ResponseEntity<TransferResponse> imps(@Valid @RequestBody TransferRequest request,
                                                 @RequestParam(required = false) String simulate) {

        String reference = "IMPS" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        // Declared before anything can fail, so a rejected transfer still
        // records what was attempted. An audit trail that only captures
        // successes is not much of an audit trail.
        AuditContext.amount(request.amount(), "INR");
        AuditContext.businessRef(reference);
        AuditContext.entityId(reference);
        AuditContext.put("transferType", "IMPS");
        AuditContext.put("beneficiaryAccount", request.beneficiaryAccount());

        if (UNKNOWN_BENEFICIARY.equals(request.beneficiaryAccount())) {
            throw new InvalidBeneficiaryException(
                    "Beneficiary account is not registered or is still in its cooling-off period");
        }

        if (request.amount().compareTo(perTransactionLimit) > 0) {
            throw new LimitExceededException(
                    "Amount " + request.amount() + " exceeds the per-transaction limit of "
                            + perTransactionLimit);
        }

        if ("PENDING".equals(simulate)) {
            // Settlement is not immediate. The customer is told so now and
            // notified when reconciliation resolves it.
            return ResponseEntity.accepted().body(new TransferResponse(
                    reference, null, "PENDING", request.amount(), "INR",
                    "Transfer initiated. You will be notified once it completes."));
        }

        String cbsReference = cbs.debit(request.debitAccount(), request.amount(), simulate);

        AuditContext.put("cbsReference", cbsReference);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(
                reference, cbsReference, "SUCCESS", request.amount(), "INR",
                "Transfer completed successfully"));
    }
}
