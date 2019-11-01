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
// ============================================================================
package org.talend.utils.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.talend.daikon.crypto.KeySource;
import org.talend.utils.StudioKeysFileCheck;


/*
* Created by bhe on Oct 30, 2019
*/
public class StudioKeySource implements KeySource {

    private static final Logger LOGGER = Logger.getLogger(StudioKeySource.class);

    public static final String KEY_SYSTEM_PREFIX = "system.encryption.key.v";

    public static final String KEY_FIXED = "routine.encryption.key";

    private static final String FIXED_ENCRYPTION_KEY_DATA = "Talend_TalendKey";

    private final String requestedKeyName;

    private final boolean isEncrypt;

    private final Properties availableKeys;

    private StudioKeySource(Properties allKeys, String keyName, boolean isMaxVersion) {
        this.availableKeys = allKeys;
        this.requestedKeyName = keyName;
        this.isEncrypt = isMaxVersion;
    }

    /**
     * <p>
     * always get encryption key, key name format: {keyname}.{version}
     * </p>
     * <p>
     * for example, system.encryption.key.v1
     * </p>
     * 
     * @param keyName requested encryption key name
     * @param isEncrypt indicate whether the encryption key is used for encryption
     */
    public static StudioKeySource key(Properties allKeys, String keyName, boolean isEncrypt) {
        return new StudioKeySource(allKeys, keyName, isEncrypt);
    }

    @Override
    public byte[] getKey() throws Exception {
        String keyToLoad = this.getKeyName();
        
        // load key
        String key = availableKeys.getProperty(keyToLoad);
        if (key == null) {
            LOGGER.warn("Can not load " + keyToLoad);
            throw new IllegalArgumentException("Invalid encryption key: " + keyToLoad);
        } else {
            LOGGER.debug("Loaded " + keyToLoad);
            return Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static int getVersion(String keyName) {
        String[] keyNameArray = keyName.split("\\.");
        if (keyNameArray[keyNameArray.length - 1].startsWith("v")
        ) {
            try {
                return Integer.parseInt(keyNameArray[keyNameArray.length - 1].substring(1));
            } catch (NumberFormatException e) {
                LOGGER.warn("Parse version of encryption key error, key: " + keyName);
            }
        }
        return 0;
    }

    private String getKeyPrefix() {
        int index = this.requestedKeyName.lastIndexOf('.');
        return this.requestedKeyName.substring(0, index);
    }

    public String getKeyName() {
        // decryption key
        if (!this.isEncrypt) {
            return this.requestedKeyName;
        }

        int keyVersion = getVersion(this.requestedKeyName);
        // No version
        if (keyVersion == 0) {
            return this.requestedKeyName;
        }

        String keyPrefix = this.getKeyPrefix();

        return availableKeys.stringPropertyNames().stream().filter(e -> e.startsWith(keyPrefix))
                .max(Comparator.comparing(e -> getVersion(e))).get();

    }

    public static Properties loadAllKeys() {
        Properties allKeys = new Properties();
        // load default keys from jar
        try (InputStream fi = StudioKeySource.class.getResourceAsStream(StudioKeysFileCheck.ENCRYPTION_KEY_FILE_NAME)) {
            allKeys.load(fi);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        // load from file set in system property, so as to override default keys
        String keyPath = System.getProperty(StudioKeysFileCheck.ENCRYPTION_KEY_FILE_SYS_PROP);
        if (keyPath != null) {
            File keyFile = new File(keyPath);
            if (keyFile.exists()) {
                try (InputStream fi = new FileInputStream(keyFile)) {
                    allKeys.load(fi);
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }

        // load system key data from System properties
        System.getProperties().forEach((k, v) -> {
            String key = String.valueOf(v);
            if (key.startsWith(KEY_SYSTEM_PREFIX)) {
                allKeys.put(key, v);
            }
        });

        // add fixed key
        allKeys.put(KEY_FIXED, Base64.getEncoder().encode(FIXED_ENCRYPTION_KEY_DATA.getBytes()));

        if (LOGGER.isDebugEnabled() || LOGGER.isTraceEnabled()) {
            allKeys.stringPropertyNames().forEach((src) -> LOGGER.info(src));
        }
        return allKeys;
    }

}
