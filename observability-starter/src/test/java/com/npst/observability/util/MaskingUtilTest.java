package com.npst.observability.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// checks the individual masking rules
class MaskingUtilTest {

    @Test
    void keepsTheFirstLetterOfEachWordOfAName() {
        assertThat(MaskingUtil.maskName("Rajesh Amin")).isEqualTo("RXXXXX AXXX");
        assertThat(MaskingUtil.maskName("Rajesh")).isEqualTo("RXXXXX");
        assertThat(MaskingUtil.maskName(null)).isNull();
    }

    @Test
    void masksAccountAndCardNumbersInsideAPath() {
        assertThat(MaskingUtil.maskIdentifiersInPath("/api/v1/accounts/918273645510/balance"))
                .isEqualTo("/api/v1/accounts/XXXXXXXX5510/balance");
        assertThat(MaskingUtil.maskIdentifiersInPath("/api/v1/cards/4111111111111111"))
                .isEqualTo("/api/v1/cards/XXXXXXXXXXXX1111");
        assertThat(MaskingUtil.maskIdentifiersInPath("/api/v1/transfers/imps"))
                .isEqualTo("/api/v1/transfers/imps");
        assertThat(MaskingUtil.maskIdentifiersInPath("/api/v1/users/42"))
                .isEqualTo("/api/v1/users/42");
    }

    @Test
    void maskingAnAlreadyMaskedValueChangesNothing() {
        String mobile = MaskingUtil.maskMobile("9876543210");
        String account = MaskingUtil.maskAccountNumber("918273645510");
        String name = MaskingUtil.maskName("Rajesh Amin");
        String path = MaskingUtil.maskIdentifiersInPath("/api/v1/accounts/918273645510/balance");

        assertThat(MaskingUtil.maskMobile(mobile)).isEqualTo(mobile);
        assertThat(MaskingUtil.maskAccountNumber(account)).isEqualTo(account);
        assertThat(MaskingUtil.maskName(name)).isEqualTo(name);
        assertThat(MaskingUtil.maskIdentifiersInPath(path)).isEqualTo(path);
    }
}
