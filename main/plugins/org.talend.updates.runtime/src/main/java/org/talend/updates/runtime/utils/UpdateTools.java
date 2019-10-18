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
package org.talend.updates.runtime.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.runtime.utils.io.IOUtils;
import org.talend.commons.utils.resource.UpdatesHelper;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.services.ICoreTisService;
import org.talend.updates.runtime.service.ITaCoKitUpdateService;
import org.talend.utils.io.FilesUtils;

public class UpdateTools {

    public static String FILE_EXTRA_FEATURE_INDEX = "extra_feature.index"; //$NON-NLS-1$

    public static String FILE_PATCH_PROPERTIES = "patch.properties"; //$NON-NLS-1$

    public static String PATCH_PROPUCT_VERSION = "product.version"; //$NON-NLS-1$

    public static void backupConfigFile() throws IOException {
        File configurationFile = getConfigurationFile();
        File tempFile = getTempConfigurationFile();
        if (tempFile.exists()) {
            tempFile.delete();
        }
        FilesUtils.copyFile(new FileInputStream(configurationFile), tempFile);
    }

    public static void restoreConfigFile() throws IOException {
        File tempFile = getTempConfigurationFile();
        if (!tempFile.exists()) {
            return;
        }
        try {
            File configurationFile = getConfigurationFile();
            if (CommonsPlugin.isJUnitTest()) {
                FilesUtils.copyFile(new FileInputStream(tempFile), configurationFile);
            } else {
                if (!IOUtils.contentEquals(new FileInputStream(configurationFile), new FileInputStream(tempFile))) {
                    if (GlobalServiceRegister.getDefault().isServiceRegistered(ICoreTisService.class)) {
                        ICoreTisService coreTisService = (ICoreTisService) GlobalServiceRegister.getDefault()
                                .getService(ICoreTisService.class);
                        coreTisService.updateConfiguratorBundles(configurationFile, tempFile);
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public static File getConfigurationFile() throws IOException {
        return getConfigurationFile(true);
    }

    public static File getConfigurationFile(boolean create) throws IOException {
        File configFile = getConfigurationFolder().toPath().resolve(UpdatesHelper.FILE_CONFIG_INI).toFile();
        if (CommonsPlugin.isJUnitTest()) {
            File configFile4Test = getProductTempFolder().toPath().resolve("configuration").resolve(UpdatesHelper.FILE_CONFIG_INI) //$NON-NLS-1$
                    .toFile();
            if (create && !configFile4Test.exists()) {
                FilesUtils.copyFile(configFile, configFile4Test);
            }
            return configFile4Test;
        }
        return configFile;
    }

    public static File getTempConfigurationFile() {
        String folderName;
        if (CommonsPlugin.isJUnitTest()) {
            folderName = "Junit"; //$NON-NLS-1$
        } else {
            folderName = "PatchInstaller"; //$NON-NLS-1$
        }
        return getProductTempFolder().toPath().resolve(folderName).resolve(UpdatesHelper.FILE_CONFIG_INI)
                .toFile();
    }

    /**
     * look for all {@link IInstallableUnit} and check that they are of type {@link InstallableUnit}. If that is so,
     * then their singleton state is set to false. WARNING : internal APIs of p2 are used because I could not find any
     * way around the limitation of P2 that does not allow 2 singletons to be deployed at the same time
     *
     * @param toInstall a set of {@link IInstallableUnit} to be set as not a singleton
     */
    public static void setIuSingletonToFalse(Set<IInstallableUnit> toInstall) {
        for (IInstallableUnit iu : toInstall) {
            if (iu instanceof InstallableUnit) {
                ((InstallableUnit) iu).setSingleton(false);
            } // else not a IU supporting singleton so ignore.
        }
    }

    /**
     * look for all {@link IInstallableUnit} in the current installed p2 profile that have the same id as the toInstall
     * IUs. then their state is forced to be singleton=false so that multiple singleton may be installed.
     *
     * @param toInstall a set of {@link IInstallableUnit} to get the Id from
     * @param agent to get the current installed IUs
     */
    public static Set<IInstallableUnit> makeInstalledIuSingletonFrom(Set<IInstallableUnit> toInstall, IProvisioningAgent agent) {
        IProfileRegistry profRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
        IProfile profile = profRegistry.getProfile("_SELF_"); //$NON-NLS-1$
        HashSet<IQuery<IInstallableUnit>> queryCollection = new HashSet<IQuery<IInstallableUnit>>();
        for (IInstallableUnit toBeInstalled : toInstall) {
            IQuery<IInstallableUnit> iuQuery = QueryUtil.createIUQuery(toBeInstalled.getId());
            queryCollection.add(iuQuery);
        }
        IQueryResult<IInstallableUnit> profileIUToBeUpdated = profile.query(QueryUtil.createCompoundQuery(queryCollection, false),
                new NullProgressMonitor());
        final Set<IInstallableUnit> unmodifiableSet = profileIUToBeUpdated.toUnmodifiableSet();
        setIuSingletonToFalse(unmodifiableSet);
        return unmodifiableSet;
    }

    public static String readProductVersionFromPatch(File installingFile) throws IOException {
        try (InputStream in = new FileInputStream(new File(installingFile, FILE_PATCH_PROPERTIES))) {
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty(PATCH_PROPUCT_VERSION, StringUtils.EMPTY);
        }
    }

    public static void syncExtraFeatureIndex(final File installingPatchFolder) throws IOException {
        File sourceFile = new File(installingPatchFolder, FILE_EXTRA_FEATURE_INDEX);
        File targetFile = new File(
                Platform.getConfigurationLocation().getURL().getPath() + File.separator + FILE_EXTRA_FEATURE_INDEX);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        FilesUtils.copyFile(sourceFile, targetFile);
    }

    public static File getProductRootFolder() {
        return new File(Platform.getInstallLocation().getURL().getPath());
    }

    public static File getConfigurationFolder() {
        return new File(Platform.getConfigurationLocation().getURL().getPath());
    }

    public static File getProductTempFolder() {
        return getProductRootFolder().toPath().resolve("temp").toFile(); //$NON-NLS-1$
    }

    public static boolean installCars(IProgressMonitor monitor, File installingPatchesFolder, boolean cancellable)
            throws Exception {
        if (installingPatchesFolder != null && installingPatchesFolder.exists()) {
            File carFolder = new File(installingPatchesFolder, ITaCoKitUpdateService.FOLDER_CAR);
            TaCoKitCarUtils.installCars(carFolder, monitor, cancellable);
        }
        return true;
    }

}
