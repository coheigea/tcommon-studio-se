package org.talend.utils.security;

// ============================================================================
//
// Copyright (C) 2006-2019 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StudioEncryptionTest {

    private String input1 = "Talend";

    private String input2 = "123456";

    private String input3 = "Talend_123456";

    private StudioEncryption se = StudioEncryption.getStudioEncryption(StudioEncryption.EncryptionKeyName.ROUTINE);

    @Test
    public void testDecryptPassword() throws Exception {
        assertNotEquals(input1, se.encrypt(input1));
        assertEquals(input1, se.decrypt(se.encrypt(input1)));

        assertNotEquals(input2, se.encrypt(input2));
        assertEquals(input2, se.decrypt(se.encrypt(input2)));

        assertNotEquals(input3, se.encrypt(input3));
        assertEquals(input3, se.decrypt(se.encrypt(input3)));
    }

    @Test
    public void testAESEncrypt() throws Exception {

        // always encrypt data by highest version of system key
        String encrypted = StudioEncryption.getStudioEncryption(StudioEncryption.EncryptionKeyName.SYSTEM).encrypt(input1);
        assertNotEquals(input1, encrypted);
        assertTrue(StudioEncryption.hasEncryptionSymbol(encrypted));
        assertTrue(encrypted.startsWith("enc:" + StudioEncryption.EncryptionKeyName.SYSTEM.toString()));
        assertEquals(input1,
                StudioEncryption.getStudioEncryption(StudioEncryption.EncryptionKeyName.SYSTEM).decrypt(encrypted));

        encrypted = StudioEncryption.getStudioEncryption(StudioEncryption.EncryptionKeyName.MIGRATION_TOKEN).encrypt(input2);
        assertNotEquals(input2, encrypted);
        assertTrue(StudioEncryption.hasEncryptionSymbol(encrypted));
        assertTrue(encrypted.startsWith("enc:" + StudioEncryption.EncryptionKeyName.MIGRATION_TOKEN.toString()));
        assertEquals(input2,
                StudioEncryption.getStudioEncryption(StudioEncryption.EncryptionKeyName.MIGRATION_TOKEN).decrypt(encrypted));

        // ensure negative case
        assertEquals(null, StudioEncryption.getStudioEncryption(StudioEncryption.EncryptionKeyName.SYSTEM).encrypt(null));
    }

    @Test
    public void testGetStudioEncryption() throws Exception {

        assertNotNull(StudioEncryption.getStudioEncryption(StudioEncryption.EncryptionKeyName.SYSTEM));

    }
}
