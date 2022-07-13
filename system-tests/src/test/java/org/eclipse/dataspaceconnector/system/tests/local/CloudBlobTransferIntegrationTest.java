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
 *       Microsoft Corporation - initial API and implementation
 *       ZF Friedrichshafen AG - add management api configurations
 *       Fraunhofer Institute for Software and Systems Engineering - added IDS API context
 *
 */

package org.eclipse.dataspaceconnector.system.tests.local;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.system.tests.utils.TestUtils.requiredPropOrEnv;

@EnabledIfEnvironmentVariable(named = "CLOUD_BLOB_TRANSFER_TEST", matches = "true")
public class CloudBlobTransferIntegrationTest extends AbstractBlobTransferTest {
    public static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    public static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";

    @Test
    public void transferBlob_success() {
        var destinationKeyVaultName = requiredPropOrEnv("consumer.eu.key.vault", null);
        var blobAccountDetails = blobAccount(destinationKeyVaultName);
        var storageAccountName = blobAccountDetails.get(0);
        var storageAccountKey = blobAccountDetails.get(1);
        var dstBlobServiceClient = getBlobServiceClient(
                format(BLOB_STORE_ENDPOINT_TEMPLATE, storageAccountName),
                storageAccountName,
                storageAccountKey
        );

        initiateTransfer(dstBlobServiceClient);
    }

    /**
     * Provides Blob storage account name and key.
     *
     * @param keyVaultName Key Vault name. This key vault must have storage account key secrets.
     * @return storage account name and account key on first and second position of list.
     */
    private List<String> blobAccount(String keyVaultName) {
        var credential = new DefaultAzureCredentialBuilder().build();
        var vault = new SecretClientBuilder()
                .vaultUrl(format(KEY_VAULT_ENDPOINT_TEMPLATE, keyVaultName))
                .credential(credential)
                .buildClient();
        // Find the first account with a key in the key vault
        var accountKeySecret = vault.listPropertiesOfSecrets().stream().filter(s -> s.getName().endsWith("-key1")).findFirst().orElseThrow(
                () -> new AssertionError("Key vault " + keyVaultName + " should contain the storage account key")
        );
        var accountKey = vault.getSecret(accountKeySecret.getName());
        var accountName = accountKeySecret.getName().replaceFirst("-key1$", "");

        return List.of(accountName, accountKey.getValue());
    }

}
