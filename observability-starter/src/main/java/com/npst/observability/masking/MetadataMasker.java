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

// hides sensitive values in maps, based on the key name
public class MetadataMasker {

    private final ObservabilityProperties.Masking config;
    private final Set<String> redactKeys;
    private final Set<String> maskKeys;

    public MetadataMasker(ObservabilityProperties.Masking config) {
        this.config = config;
        this.redactKeys = normalise(config.getRedactKeys());
        this.maskKeys = normalise(config.getMaskKeys());
    }

    // returns a masked copy; the original map is not changed
    public Map<String, Object> mask(Map<String, Object> metadata) {

        if (metadata == null || metadata.isEmpty() || !config.isEnabled()) {
            return metadata;
        }

        Map<String, Object> masked = new LinkedHashMap<>(metadata.size());

        metadata.forEach((key, value) -> masked.put(key, maskValue(key, value)));

        return masked;
    }

    // decide what to do with one key and value
    private Object maskValue(String key, Object value) {

        // exact match on the normalised key (lower case, without _ and -)
        String normalised = normalise(key);

        // secrets are replaced completely
        if (redactKeys.contains(normalised)) {
            return config.getPlaceholder();
        }

        // identifiers keep only a few characters
        if (maskKeys.contains(normalised) && value != null) {
            return ruleFor(normalised).apply(String.valueOf(value));
        }

        // otherwise look inside nested maps and lists
        return maskNested(key, value);
    }

    // masks values inside nested maps and lists
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

    // pick the masking style for a key
    private static UnaryOperator<String> ruleFor(String normalisedKey) {
        return switch (normalisedKey) {
            case "mobile", "mobilenumber", "phone", "phonenumber" -> MaskingUtil::maskMobile;
            case "pan" -> MaskingUtil::maskPan;
            case "aadhaar", "aadhar" -> MaskingUtil::maskAadhaar;
            case "email", "emailid" -> MaskingUtil::maskEmail;
            case "cardnumber", "cardno", "card" -> MaskingUtil::maskCardNumber;
            case "name", "fullname", "firstname", "lastname", "customername",
                 "beneficiaryname", "accountholdername", "nomineename" -> MaskingUtil::maskName;
            default -> MaskingUtil::maskAccountNumber;
        };
    }

    // key helpers: lower case, no _ or -
    private static Set<String> normalise(List<String> keys) {
        return keys.stream().map(MetadataMasker::normalise).collect(Collectors.toSet());
    }

    private static String normalise(String key) {
        return key == null ? "" : key.toLowerCase().replace("_", "").replace("-", "");
    }
}
