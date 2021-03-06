// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.items.importexport.handlers.exports;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.talend.commons.runtime.utils.io.FileCopyUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.process.JobInfo;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.runtime.process.ITalendProcessJavaProject;
import org.talend.core.runtime.repository.build.IBuildResourcesProvider;
import org.talend.core.runtime.util.ParametersUtil;
import org.talend.designer.runprocess.IRunProcessService;

public class SyncChildrenTestReportsProvider implements IBuildResourcesProvider {

    private static final String TEST_REPORTS_FOLDER = "surefire-reports";

    @SuppressWarnings("unchecked")
    @Override
    public void prepare(IProgressMonitor monitor, Map<String, Object> parameters) throws Exception {
        if (parameters == null) {
            return;
        }
        final ITalendProcessJavaProject processJavaProject = (ITalendProcessJavaProject) ParametersUtil.getObject(parameters,
                OBJ_PROCESS_JAVA_PROJECT, ITalendProcessJavaProject.class);
        if (processJavaProject == null) {
            return;
        }
        final ProcessItem processItem = (ProcessItem) ParametersUtil.getObject(parameters, OBJ_PROCESS_ITEM, ProcessItem.class);
        if (processItem == null) {
            return;
        }
        final Set<JobInfo> dependenciesItems = (Set<JobInfo>) ParametersUtil.getObject(parameters, OBJ_ITEM_DEPENDENCIES,
                Set.class);
        if (dependenciesItems == null || dependenciesItems.isEmpty()) {
            return;
        }
        if (!GlobalServiceRegister.getDefault().isServiceRegistered(IRunProcessService.class)) {
            return;
        }
        final IRunProcessService runProcessService = (IRunProcessService) GlobalServiceRegister.getDefault()
                .getService(IRunProcessService.class);

        final File reportTargetFolder = processJavaProject.getTargetFolder().getFolder(TEST_REPORTS_FOLDER).getLocation()
                .toFile();

        for (JobInfo jobInfo : dependenciesItems) {
            if (jobInfo.isJoblet()) {
                continue;
            }
            ITalendProcessJavaProject childJavaProject = runProcessService
                    .getTalendJobJavaProject(jobInfo.getProcessItem().getProperty());
            if (childJavaProject != null) {
                childJavaProject.getTargetFolder().refreshLocal(IResource.DEPTH_INFINITE, monitor);
                final IFolder childReportFolder = childJavaProject.getTargetFolder().getFolder(TEST_REPORTS_FOLDER);
                if (childReportFolder.exists()) {
                    FileCopyUtils.syncFolder(childReportFolder.getLocation().toFile(), reportTargetFolder, false);
                }
            }
        }

        processJavaProject.getTargetFolder().refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }

}
