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
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.talend.daikon.crypto.KeySource;
import org.talend.utils.StudioKeysFileCheck;


/*
* Created by bhe on Oct 30, 2019
*/
public class StudioKeySource implements KeySource {

    private static final Logger LOGGER = Logger.getLogger(StudioEncryption.class);

    public static final String KEY_SYSTEM_PREFIX = "system.encryption.key.v";

    public static final String KEY_FIXED = "routine.encryption.key";

    private static final String FIXED_ENCRYPTION_KEY_DATA = "Talend_TalendKey";

    private final String keyName;

    private final boolean isMaxVersion;

    private String targetKeyName;

    private final Properties availableKeys = new Properties();

    private StudioKeySource(String keyName, boolean isMaxVersion) {
        this.keyName = keyName;
        this.isMaxVersion = isMaxVersion;

        // load default keys from jar
        try (InputStream fi = StudioKeySource.class.getResourceAsStream(StudioKeysFileCheck.ENCRYPTION_KEY_FILE_NAME)) {
            availableKeys.load(fi);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        // load from file set in system property, so as to override default keys
        String keyPath = System.getProperty(StudioKeysFileCheck.ENCRYPTION_KEY_FILE_SYS_PROP);
        if (keyPath != null) {
            File keyFile = new File(keyPath);
            if (keyFile.exists()) {
                try (InputStream fi = new FileInputStream(keyFile)) {
                    availableKeys.load(fi);
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }

        // load system key data from System properties
        System.getProperties().forEach((k,v)->{
            String key = String.valueOf(v);
            if (key.startsWith(KEY_SYSTEM_PREFIX)) {
                availableKeys.put(key, v);
            }
        });
        
        // add fixed key
        availableKeys.put(KEY_FIXED, Base64.getEncoder().encode(FIXED_ENCRYPTION_KEY_DATA.getBytes()));

        if (LOGGER.isDebugEnabled() || LOGGER.isTraceEnabled()) {
            availableKeys.stringPropertyNames().forEach((src) -> LOGGER.info(src));
        }
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
    public static StudioKeySource key(String keyName, boolean isEncrypt) {
        return new StudioKeySource(keyName, isEncrypt);
    }

    @Override
    public byte[] getKey() throws Exception {
        targetKeyName = this.getKeyName();
        
        // load key
        String key = availableKeys.getProperty(targetKeyName);
        if (key == null) {
            LOGGER.warn("Can not load " + targetKeyName);
            throw new IllegalArgumentException("Invalid encryption key: " + targetKeyName);
        } else {
            LOGGER.debug("Loaded " + targetKeyName);
            return Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8));
        }
    }

    private int getVersion(String keyName) {
        String[] keyNameArray = keyName.split("\\.");
        if (keyNameArray[keyNameArray.length - 1].startsWith("v")
        ) {
            try {
                return Integer.parseInt(keyNameArray[keyNameArray.length - 1].substring(1));
            } catch (NumberFormatException e) {
                LOGGER.warn("Parse version of encryption key error, key: " + targetKeyName);
            }
        }
        return 0;
    }

    private String getKeyPrefix() {
        int index = this.keyName.lastIndexOf('.');
        return this.keyName.substring(0, index);
    }

    public String getKeyName() {
        // decryption key
        if (!this.isMaxVersion) {
            return this.keyName;
        }

        int keyVersion = this.getVersion(this.keyName);
        // No version
        if (keyVersion == 0) {
            return this.keyName;
        }
        // already computed
        if (this.targetKeyName != null) {
            return this.targetKeyName;
        }

        String keyPrefix = this.getKeyPrefix();

        SortedSet<Integer> versions = new TreeSet<Integer>();

        availableKeys.stringPropertyNames().forEach((src)->{
            if (src.startsWith(keyPrefix)) {
                versions.add(this.getVersion(src));
            }
        });

        return keyPrefix + ".v" + versions.last();
    }

}
