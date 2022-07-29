/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MockCredentialsVerifierTest {
    private static final String VERIFIABLE_CREDENTIAL_KEY = "vc";
    private static final String CREDENTIAL_SUBJECT_KEY = "credentialSubject";
    MockCredentialsVerifier verifier = new MockCredentialsVerifier(new ConsoleMonitor());
    PublicKeyWrapper wrapper = mock(PublicKeyWrapper.class);

    @Test
    void verifyCredentials() {
        Result<Map<String, Object>> actual = verifier.verifyCredentials("http://dummy.site/foo?region=us&tier=GOLD", wrapper);
        assertThat(actual.succeeded()).isTrue();
        assertThat(actual.getContent())
                .extracting(c -> c.values().stream().findFirst().get())
                .extracting(VERIFIABLE_CREDENTIAL_KEY)
                .extracting(CREDENTIAL_SUBJECT_KEY)
                .isEqualTo(Map.of("region", "us", "tier", "GOLD"));
    }

    @Test
    void verifyCredentials_failsOnMalformedUrl() {
        assertThatThrownBy(() -> verifier.verifyCredentials("malformed_url", wrapper))
                .isInstanceOf(EdcException.class);
    }

    private Map<String, Object> extractClaims(Map<String, Object> verifiableCredentials) {
        var vcObject = (Map<String, Object>) verifiableCredentials.values().stream().findFirst().get();
        var vc = (Map<String, Object>) vcObject.get(VERIFIABLE_CREDENTIAL_KEY);
        return (Map<String, Object>) vc.get(CREDENTIAL_SUBJECT_KEY);
    }
}