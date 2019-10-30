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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.talend.daikon.crypto.CipherSource;
import org.talend.daikon.crypto.CipherSources;
import org.talend.daikon.crypto.Encryption;
import org.talend.utils.StudioKeysFileCheck;

public class StudioEncryption {

    private static final Logger LOGGER = Logger.getLogger(StudioEncryption.class);

    private static final String ENCRYPTION_KEY_FILE_NAME = StudioKeysFileCheck.ENCRYPTION_KEY_FILE_NAME;

    private static final String ENCRYPTION_KEY_FILE_SYS_PROP = StudioKeysFileCheck.ENCRYPTION_KEY_FILE_SYS_PROP;

    private static final String PREFIX_PASSWORD = "enc:"; //$NON-NLS-1$

    // Encryption key property names
    private static final String KEY_SYSTEM = StudioKeySource.KEY_SYSTEM_PREFIX + "1";

    private static final String KEY_MIGRATION_TOKEN = "migration.token.encryption.key";

    private static final String KEY_ROUTINE = StudioKeySource.KEY_FIXED;

    private EncryptionKeyName requestKeyName;

    private String requestEncryptionProvider;

    public enum EncryptionKeyName {
        SYSTEM(KEY_SYSTEM),
        ROUTINE(KEY_ROUTINE),
        MIGRATION_TOKEN(KEY_MIGRATION_TOKEN);

        private final String name;

        EncryptionKeyName(String name) {
            this.name = name;
        }
    }

    static {
        // set up key file
        updateConfig();
    }

    private static final ThreadLocal<Map<String, StudioKeySource>> LOCALCACHEDKEYSOURCES = ThreadLocal.withInitial(() -> {
        Map<String, StudioKeySource> cachedKeySources = new HashMap<String, StudioKeySource>();
        EncryptionKeyName[] keyNames = { EncryptionKeyName.SYSTEM, EncryptionKeyName.MIGRATION_TOKEN };
        for (EncryptionKeyName keyName : keyNames) {
            StudioKeySource ks = loadKeySource(keyName, false);
            if (ks != null) {
                cachedKeySources.put(keyName.name, ks);
            }
        }
        cachedKeySources.put(EncryptionKeyName.ROUTINE.name, StudioKeySource.keyForDecryption(KEY_ROUTINE));
        return cachedKeySources;
    });

    private StudioEncryption(EncryptionKeyName encryptionKeyName, String providerName) {
        this.requestKeyName = encryptionKeyName;
        this.requestEncryptionProvider = providerName;
    }

    private Encryption getEncryption(boolean encrypt) {
        if (this.requestKeyName == null) {
            this.requestKeyName = EncryptionKeyName.SYSTEM;
        }

        StudioKeySource ks = LOCALCACHEDKEYSOURCES.get().get(this.requestKeyName);

        if (ks == null) {
            ks = loadKeySource(this.requestKeyName, encrypt);
            if (ks != null) {
                LOCALCACHEDKEYSOURCES.get().put(ks.getKeyName(), ks);
            }
        }
        if (ks == null) {
            RuntimeException e = new IllegalArgumentException("Can not load encryption key data: " + this.requestKeyName.name);
            LOGGER.error(e);
            throw e;
        }

        CipherSource cs = null;
        if (this.requestEncryptionProvider != null && !this.requestEncryptionProvider.isEmpty()) {
            Provider p = Security.getProvider(this.requestEncryptionProvider);
            cs = CipherSources.aesGcm(12, 16, p);
        }

        if (cs == null) {
            cs = CipherSources.getDefault();
        }

        return new Encryption(ks, cs);
    }

    private static StudioKeySource loadKeySource(EncryptionKeyName encryptionKeyName, boolean isEncrypt) {
        StudioKeySource ks = null;

        if (isEncrypt) {
            ks = StudioKeySource.keyForEncryption(encryptionKeyName.name);
        } else {
            ks = StudioKeySource.keyForDecryption(encryptionKeyName.name);
        }

        try {
            if (ks.getKey() != null) {
                return ks;
            }
        } catch (Exception e) {
            LOGGER.warn("Can not load encryption key from file", e);
        }

        return null;
    }

    public String encrypt(String src) {
        // backward compatibility
        if (src == null) {
            return src;
        }
        try {
            if (!hasEncryptionSymbol(src)) {
                return PREFIX_PASSWORD + this.getEncryption(true).encrypt(src);
            }
        } catch (Exception e) {
            // backward compatibility
            LOGGER.error("encrypt error", e);
            return null;
        }
        return src;
    }

    public String decrypt(String src) {
        // backward compatibility
        if (src == null || src.isEmpty()) {
            return src;
        }
        try {
            if (hasEncryptionSymbol(src)) {
                return this.getEncryption(false)
                        .decrypt(src.substring(PREFIX_PASSWORD.length(), src.length()));
            }
        } catch (Exception e) {
            // backward compatibility
            LOGGER.error("decrypt error", e);
            return null;
        }

        return src;
    }


    /**
     * Get instance of StudioEncryption with given encryption key name
     * 
     * keyName - see {@link StudioEncryption.EncryptionKeyName}, {@link StudioEncryption.EncryptionKeyName.SYSTEM} by
     * default
     */
    public static StudioEncryption getStudioEncryption(EncryptionKeyName keyName) {
        return new StudioEncryption(keyName, null);
    }

    /**
     * Get instance of StudioEncryption with given encryption key name, security provider is "BC"
     * 
     * keyName - see {@link StudioEncryption.EncryptionKeyName}
     */
    public static StudioEncryption getStudioBCEncryption(EncryptionKeyName keyName) {
        return new StudioEncryption(keyName, "BC");
    }

    public static boolean hasEncryptionSymbol(String input) {
        if (input == null || input.length() == 0) {
            return false;
        }
        return input.startsWith(PREFIX_PASSWORD);
    }

    private static void updateConfig() {
        String keyPath = System.getProperty(ENCRYPTION_KEY_FILE_SYS_PROP);
        if (keyPath != null) {
            File keyFile = new File(keyPath);
            if (!keyFile.exists()) {
                if (isStudio()) {
                    // load all keys
                    Properties p = new Properties();
                    try (InputStream fi = StudioEncryption.class.getResourceAsStream(ENCRYPTION_KEY_FILE_NAME)) {
                        p.load(fi);
                    } catch (IOException e) {
                        LOGGER.error("load encryption keys error", e);
                    }
                    // EncryptionKeyName.MIGRATION_TOKEN are not allowed to be updated
                    p.remove(EncryptionKeyName.MIGRATION_TOKEN.name);

                    // persist keys to ~configuration/studio.keys
                    try (OutputStream fo = new FileOutputStream(keyFile)) {
                        p.store(fo, "studio encryption keys");
                    } catch (IOException e) {
                        LOGGER.error("persist encryption keys error", e);
                    }
                    LOGGER.info("updateConfig, studio environment, key file setup completed");
                } else {
                    LOGGER.info("updateConfig, non studio environment, skip setup of key file");
                }
            }
        }
    }

    private static boolean isStudio() {
        String osgiFramework = System.getProperty("osgi.framework");
        return osgiFramework != null && osgiFramework.contains("eclipse");
    }
}
