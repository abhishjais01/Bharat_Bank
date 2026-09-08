package com.npst.observability.masking;

import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.util.MaskingUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Scrubs sensitive banking data out of log metadata before it leaves the JVM.
 *
 * <p>This runs at the source rather than in a central processor. The
 * architecture note argues for masking centrally, and for an ELK pipeline that
 * is right - but there is no processing stage between Promtail and Loki, so an
 * OTP that reaches {@code application.log} is already on disk unmasked. Source
 * masking is the only control that exists today; a central rule set can be
 * layered on later without removing this one.
 *
 * <p>Matching is exact on the normalised key - lower-cased with {@code _} and
 * {@code -} stripped, so {@code accountNumber}, {@code account_number} and
 * {@code ACCOUNT-NUMBER} all match. Substring matching was rejected: "pin"
 * would have swallowed "shipping".
 */
public class MetadataMasker {

    private final ObservabilityProperties.Masking config;
    private final Set<String> redactKeys;
    private final Set<String> maskKeys;

    public MetadataMasker(ObservabilityProperties.Masking config) {
        this.config = config;
        this.redactKeys = normalise(config.getRedactKeys());
        this.maskKeys = normalise(config.getMaskKeys());
    }

    /**
     * Returns a masked copy. The caller's map is never modified - a logging
     * call must not mutate the business object it was handed.
     */
    public Map<String, Object> mask(Map<String, Object> metadata) {

        if (metadata == null || metadata.isEmpty() || !config.isEnabled()) {
            return metadata;
        }

        Map<String, Object> masked = new LinkedHashMap<>(metadata.size());

        metadata.forEach((key, value) -> masked.put(key, maskValue(key, value)));

        return masked;
    }

    private Object maskValue(String key, Object value) {

        String normalised = normalise(key);

        if (redactKeys.contains(normalised)) {
            // Redact regardless of type. A null here would look like "absent"
            // rather than "withheld", which reads as a bug during an incident.
            return config.getPlaceholder();
        }

        if (maskKeys.contains(normalised) && value != null) {
            return ruleFor(normalised).apply(String.valueOf(value));
        }

        return maskNested(key, value);
    }

    /**
     * Metadata is often a nested structure - a transfer's beneficiary block,
     * a list of accounts. Masking only the top level would leak everything one
     * level down.
     */
    @SuppressWarnings("unchecked")
    private Object maskNested(String key, Object value) {

        if (value instanceof Map<?, ?> nested) {
            Map<String, Object> copy = new LinkedHashMap<>();
            nested.forEach((k, v) -> copy.put(String.valueOf(k),
                    maskValue(String.valueOf(k), v)));
            return copy;
        }

        if (value instanceof Iterable<?> items) {
            List<Object> copy = new ArrayList<>();
            items.forEach(item -> copy.add(maskValue(key, item)));
            return copy;
        }

        return value;
    }

    private static UnaryOperator<String> ruleFor(String normalisedKey) {
        return switch (normalisedKey) {
            case "mobile", "mobilenumber", "phone", "phonenumber" -> MaskingUtil::maskMobile;
            case "pan" -> MaskingUtil::maskPan;
            case "aadhaar", "aadhar" -> MaskingUtil::maskAadhaar;
            case "email", "emailid" -> MaskingUtil::maskEmail;
            case "cardnumber", "cardno", "card" -> MaskingUtil::maskCardNumber;
            default -> MaskingUtil::maskAccountNumber;
        };
    }

    private static Set<String> normalise(List<String> keys) {
        return keys.stream().map(MetadataMasker::normalise).collect(Collectors.toSet());
    }

    private static String normalise(String key) {
        return key == null ? "" : key.toLowerCase().replace("_", "").replace("-", "");
    }
}
