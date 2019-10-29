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
package org.talend.designer.maven.tools.creator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.JobInfo;
import org.talend.core.model.process.ProcessUtils;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.core.runtime.maven.MavenArtifact;
import org.talend.core.runtime.maven.MavenConstants;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.core.runtime.process.TalendProcessArgumentConstant;
import org.talend.core.runtime.projectsetting.IProjectSettingTemplateConstants;
import org.talend.core.runtime.repository.build.IMavenPomCreator;
import org.talend.core.ui.ITestContainerProviderService;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.template.ETalendMavenVariables;
import org.talend.designer.maven.tools.ProcessorDependenciesManager;
import org.talend.designer.maven.utils.PomIdsHelper;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.designer.runprocess.IBigDataProcessor;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.repository.ProjectManager;

/**
 * DOC ggu class global comment. Detailled comment
 */
public abstract class AbstractMavenProcessorPom extends CreateMavenBundleTemplatePom implements IMavenPomCreator {

    private final IProcessor jobProcessor;

    private final ProcessorDependenciesManager processorDependenciesManager;

    private IFolder objectTypeFolder;

    private IPath itemRelativePath;

    private boolean syncCodesPoms;

    private boolean hasLoopDependency;

    public AbstractMavenProcessorPom(IProcessor jobProcessor, IFile pomFile, String bundleTemplateName) {
        super(pomFile, IProjectSettingTemplateConstants.PATH_STANDALONE + '/' + bundleTemplateName);
        Assert.isNotNull(jobProcessor);
        this.jobProcessor = jobProcessor;
        this.processorDependenciesManager = new ProcessorDependenciesManager(jobProcessor);

        // always ignore case.
        this.setIgnoreFileNameCase(true);
        // should only base on template.
        this.setBaseOnTemplateOnly(true);
    }

    protected IProcessor getJobProcessor() {
        return this.jobProcessor;
    }

    protected ProcessorDependenciesManager getProcessorDependenciesManager() {
        return processorDependenciesManager;
    }

    public IFolder getObjectTypeFolder() {
        return objectTypeFolder;
    }

    public void setObjectTypeFolder(IFolder objectTypeFolder) {
        this.objectTypeFolder = objectTypeFolder;
    }

    public IPath getItemRelativePath() {
        return itemRelativePath;
    }

    public void setItemRelativePath(IPath itemRelativePath) {
        this.itemRelativePath = itemRelativePath;
    }

    @Override
    protected void setAttributes(Model model) {
        //
        final IProcessor jProcessor = getJobProcessor();
        IProcess process = jProcessor.getProcess();
        Property property = jProcessor.getProperty();

        if (ProcessUtils.isTestContainer(process)) {
            if (GlobalServiceRegister.getDefault().isServiceRegistered(ITestContainerProviderService.class)) {
                ITestContainerProviderService testService = (ITestContainerProviderService) GlobalServiceRegister.getDefault()
                        .getService(ITestContainerProviderService.class);
                try {
                    property = testService.getParentJobItem(property.getItem()).getProperty();
                    process = testService.getParentJobProcess(process);
                } catch (PersistenceException e) {
                    ExceptionHandler.process(e);
                }
            }
        }

        Map<ETalendMavenVariables, String> variablesValuesMap = new HashMap<ETalendMavenVariables, String>();
        // no need check property is null or not, because if null, will get default ids.
        variablesValuesMap.put(ETalendMavenVariables.JobGroupId, PomIdsHelper.getJobGroupId(property));
        variablesValuesMap.put(ETalendMavenVariables.JobArtifactId, PomIdsHelper.getJobArtifactId(property));
        variablesValuesMap.put(ETalendMavenVariables.JobVersion, PomIdsHelper.getJobVersion(property));
        variablesValuesMap.put(ETalendMavenVariables.TalendJobVersion, property.getVersion());
        final String jobName = JavaResourcesHelper.escapeFileName(process.getName());
        variablesValuesMap.put(ETalendMavenVariables.JobName, jobName);

        if (property != null) {
            Project currentProject = ProjectManager.getInstance().getProject(property);
            variablesValuesMap.put(ETalendMavenVariables.ProjectName,
                    currentProject != null ? currentProject.getTechnicalLabel() : null);

            Item item = property.getItem();
            if (item != null) {
                ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(item);
                if (itemType != null) {
                    variablesValuesMap.put(ETalendMavenVariables.JobType, itemType.getLabel());
                }
            }
        }

        this.setGroupId(ETalendMavenVariables.replaceVariables(model.getGroupId(), variablesValuesMap));
        this.setArtifactId(ETalendMavenVariables.replaceVariables(model.getArtifactId(), variablesValuesMap));
        this.setVersion(ETalendMavenVariables.replaceVariables(model.getVersion(), variablesValuesMap));
        this.setName(ETalendMavenVariables.replaceVariables(model.getName(), variablesValuesMap));

        super.setAttributes(model);
    }

    @Override
    protected Model createModel() {
        Model model = super.createModel();
        if (model != null) {
            Map<String, Object> templateParameters = PomUtil.getTemplateParameters(jobProcessor.getProperty());
            PomUtil.checkParent(model, this.getPomFile(), templateParameters);
            setupShade(model);
            addDependencies(model);
        }
        return model;
    }

    protected void setupShade(Model model) {
        if (jobProcessor instanceof IBigDataProcessor) {
            IBigDataProcessor bigDataProcessor = (IBigDataProcessor) jobProcessor;
            if (bigDataProcessor.needsShade()) {
                List<Plugin> plugins = model.getBuild().getPlugins();
                Plugin shade = null;
                for (Plugin plugin : plugins) {
                    if (plugin.getArtifactId().equals("maven-shade-plugin")) { //$NON-NLS-1$
                        shade = plugin;
                        break;
                    }
                }
                if (shade != null) {
                    plugins.remove(shade);
                }
                shade = new Plugin();
                shade.setGroupId("org.apache.maven.plugins"); //$NON-NLS-1$
                shade.setArtifactId("maven-shade-plugin"); //$NON-NLS-1$
                shade.setVersion("3.1.0"); //$NON-NLS-1$
                List<PluginExecution> executions = shade.getExecutions();
                PluginExecution execution = new PluginExecution();
                executions.add(execution);
                execution.addGoal("shade"); //$NON-NLS-1$
                Xpp3Dom configuration = new Xpp3Dom("configuration"); //$NON-NLS-1$
                execution.setConfiguration(configuration);
                // disable the setup of minimize jar for now, as it could cause some other issues
                // like for example a Class.forName("oracle...." as there is no direct class dependency

                // Xpp3Dom minimizeJar = new Xpp3Dom("minimizeJar"); //$NON-NLS-1$
                // minimizeJar.setValue("true"); //$NON-NLS-1$
                // configuration.addChild(minimizeJar);
                Xpp3Dom artifactSet = new Xpp3Dom("artifactSet"); //$NON-NLS-1$
                configuration.addChild(artifactSet);
                Xpp3Dom excludes = new Xpp3Dom("excludes"); //$NON-NLS-1$
                Set<ModuleNeeded> modules = bigDataProcessor.getShadedModulesExclude();
                if (!modules.isEmpty()) {
                    artifactSet.addChild(excludes);
                }

                for (ModuleNeeded module : modules) {
                    Xpp3Dom include = new Xpp3Dom("exclude"); //$NON-NLS-1$
                    excludes.addChild(include);
                    MavenArtifact mvnArtifact = MavenUrlHelper.parseMvnUrl(module.getMavenUri());
                    include.setValue(mvnArtifact.getGroupId() + ":" + mvnArtifact.getArtifactId()); //$NON-NLS-1$
                }
                
                //removing digital signatures from uber jar
                Xpp3Dom filters = new Xpp3Dom("filters"); //$NON-NLS-1$
                Xpp3Dom filter = new Xpp3Dom("filter"); //$NON-NLS-1$
                Xpp3Dom artifact = new Xpp3Dom("artifact"); //$NON-NLS-1$
                artifact.setValue("*:*");
                Xpp3Dom filterExcludes = new Xpp3Dom("excludes"); //$NON-NLS-1$
                Xpp3Dom excludeSF = new Xpp3Dom("exclude");
                excludeSF.setValue("META-INF/*.SF");
                Xpp3Dom excludeDSA = new Xpp3Dom("exclude");
                excludeDSA.setValue("META-INF/*.DSA");
                Xpp3Dom excludeRSA = new Xpp3Dom("exclude");
                excludeRSA.setValue("META-INF/*.RSA");

                filterExcludes.addChild(excludeSF);
                filterExcludes.addChild(excludeDSA);
                filterExcludes.addChild(excludeRSA);
                
                filter.addChild(artifact);
                filter.addChild(filterExcludes);
                filters.addChild(filter);
                configuration.addChild(filters);
                
                plugins.add(shade);
            }
        }
    }

    protected void addDependencies(Model model) {
        try {
            getProcessorDependenciesManager().updateDependencies(null, model);

            final List<Dependency> dependencies = model.getDependencies();

            // add codes to dependencies
            addCodesDependencies(dependencies);

            // add children jobs in dependencies
            addChildrenDependencies(dependencies);
        } catch (ProcessorException e) {
            ExceptionHandler.process(e);
        }
    }

    protected void addCodesDependencies(final List<Dependency> dependencies) {
        String projectTechName = ProjectManager.getInstance().getProject(getJobProcessor().getProperty()).getTechnicalLabel();
        String codeVersion = PomIdsHelper.getCodesVersion(projectTechName);

        // routines
        String routinesGroupId = PomIdsHelper.getCodesGroupId(projectTechName, TalendMavenConstants.DEFAULT_CODE);
        String routinesArtifactId = TalendMavenConstants.DEFAULT_ROUTINES_ARTIFACT_ID;
        Dependency routinesDependency = PomUtil.createDependency(routinesGroupId, routinesArtifactId, codeVersion, null);
        dependencies.add(routinesDependency);

        // pigudfs
        if (ProcessUtils.isRequiredPigUDFs(jobProcessor.getProcess())) {
            String pigudfsGroupId = PomIdsHelper.getCodesGroupId(projectTechName, TalendMavenConstants.DEFAULT_PIGUDF);
            String pigudfsArtifactId = TalendMavenConstants.DEFAULT_PIGUDFS_ARTIFACT_ID;
            Dependency pigudfsDependency = PomUtil.createDependency(pigudfsGroupId, pigudfsArtifactId, codeVersion, null);
            dependencies.add(pigudfsDependency);
        }

        // beans
        if (ProcessUtils.isRequiredBeans(jobProcessor.getProcess())) {
            String beansGroupId = PomIdsHelper.getCodesGroupId(projectTechName, TalendMavenConstants.DEFAULT_BEAN);
            String beansArtifactId = TalendMavenConstants.DEFAULT_BEANS_ARTIFACT_ID;
            Dependency beansDependency = PomUtil.createDependency(beansGroupId, beansArtifactId, codeVersion, null);
            dependencies.add(beansDependency);
        }
    }

    protected void addChildrenDependencies(final List<Dependency> dependencies) {
        String parentId = getJobProcessor().getProperty().getId();

        final Set<JobInfo> clonedChildrenJobInfors = getJobProcessor().getBuildFirstChildrenJobs();
        for (JobInfo jobInfo : clonedChildrenJobInfors) {
            if (jobInfo.getFatherJobInfo() != null && jobInfo.getFatherJobInfo().getJobId().equals(parentId)) {
                if (!validChildrenJob(jobInfo)) {
                    continue;
                }
                Property property;
                String groupId;
                String artifactId;
                String version;
                String type = null;
                if (!jobInfo.isJoblet()) {
                    property = jobInfo.getProcessItem().getProperty();
                    groupId = PomIdsHelper.getJobGroupId(property);
                    artifactId = PomIdsHelper.getJobArtifactId(jobInfo);
                    if ("OSGI".equals(property.getAdditionalProperties().get(TalendProcessArgumentConstant.ARG_BUILD_TYPE))) {
                        artifactId = artifactId + "-bundle";
                    }

                    version = PomIdsHelper.getJobVersion(property);
                    // try to get the pom version of children job and load from the pom file.
                    String childPomFileName = PomUtil.getPomFileName(jobInfo.getJobName(), jobInfo.getJobVersion());
                    IProject codeProject = getJobProcessor().getCodeProject();
                    if (codeProject != null) {
                        try {
                            codeProject.refreshLocal(IResource.DEPTH_ONE, null); // is it ok or needed here ???
                        } catch (CoreException e) {
                            ExceptionHandler.process(e);
                        }
                        IFile childPomFile = codeProject.getFile(new Path(childPomFileName));
                        if (childPomFile.exists()) {
                            try {
                                Model childModel = MODEL_MANAGER.readMavenModel(childPomFile);
                                // try to get the real groupId, artifactId, version.
                                groupId = childModel.getGroupId();
                                artifactId = childModel.getArtifactId();
                                version = childModel.getVersion();
                            } catch (CoreException e) {
                                ExceptionHandler.process(e);
                            }
                        }
                    }
                } else {
                    property = jobInfo.getJobletProperty();
                    groupId = PomIdsHelper.getJobletGroupId(property);
                    artifactId = PomIdsHelper.getJobletArtifactId(property);
                    version = PomIdsHelper.getJobletVersion(property);
                    type = MavenConstants.PACKAGING_POM;
                }
                Dependency d = PomUtil.createDependency(groupId, artifactId, version, type);
                dependencies.add(d);
            }
        }
    }

    protected boolean validChildrenJob(JobInfo jobInfo) {
        return true; // default, all are valid
    }

    /*
     * (non-Javadoc)
     *
     * @see org.talend.core.runtime.repository.build.IMavenPomCreator#needsyncCodesPoms(boolean)
     */
    @Override
    public void setSyncCodesPoms(boolean isMainJob) {
        this.syncCodesPoms = isMainJob;
    }

    public boolean needSyncCodesPoms() {
        return this.syncCodesPoms;
    }

    /**
     * Sets the hasLoopDependency.
     *
     * @param hasLoopDependency the hasLoopDependency to set
     */
    public void setHasLoopDependency(boolean hasLoopDependency) {
        this.hasLoopDependency = hasLoopDependency;
    }

    /**
     * Getter for hasLoopDependency.
     *
     * @return the hasLoopDependency
     */
    public boolean hasLoopDependency() {
        return this.hasLoopDependency;
    }
}
