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

import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

/**
 * Mock credentials verifier that simply returns claims parsed from the URL configured for the identity hub.
 * It will be replaced soon by the IdentityHubCredentialsVerifier.
 */
public class MockCredentialsVerifier implements CredentialsVerifier {
    private static final String VERIFIABLE_CREDENTIAL_KEY = "vc";
    private static final String CREDENTIAL_SUBJECT_KEY = "credentialSubject";
    private final Monitor monitor;

    public MockCredentialsVerifier(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Returns claims parsed from the query string of the URL configured for the identity hub.
     * <p>
     * The URL is not accessed, and URL parts other than the query string are unimportant.
     * <p>
     * For example, if {@code hubBaseUrl} is {@code http://dummy.site/foo?region=us&tier=GOLD}, the verifier
     * returns {@code
     * Map.of("vcId, Map.of("vc", Map.of("credentialSubject", Map.of("region", "us", "tier", "GOLD")),
     *                            Map.of("iss", "someIssuer")))}
     *
     * @param hubBaseUrl      the URL used to parse the query string.
     * @param othersPublicKey unused.
     * @return claims as defined in query string parameters.
     */
    @Override
    public Result<Map<String, Object>> verifyCredentials(String hubBaseUrl, PublicKeyWrapper othersPublicKey) {
        monitor.debug("Starting (mock) credential verification against hub URL " + hubBaseUrl);

        try {
            var url = new URL(hubBaseUrl);
            var claims = Pattern.compile("&")
                    .splitAsStream(url.getQuery())
                    .map(s -> Arrays.copyOf(s.split("=", 2), 2))
                    .collect(toMap(
                            s -> decode(s[0], UTF_8),
                            s -> (Object) decode(s[1], UTF_8)
                    ));
            monitor.debug("Completing (mock) credential verification. Claims: " + claims);
            return Result.success(toMappedVerifiableCredentials(claims));
        } catch (MalformedURLException e) {
            throw new EdcException(e);
        }
    }

    private Map<String, Object> toMappedVerifiableCredentials(Map<String, Object> regionClaims) {

        var vcId = UUID.randomUUID().toString();
        return Map.of(vcId,
                Map.of(VERIFIABLE_CREDENTIAL_KEY, Map.of(CREDENTIAL_SUBJECT_KEY, regionClaims),
                        // issuer will be ignored when applying policies for now.
                        "iss", String.join("did:web:", UUID.randomUUID().toString())));
    }
}
