/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.yangutils.plugin.manager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.linker.YangLinker;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.linker.impl.YangLinkerManager;
import org.onosproject.yangutils.parser.YangUtilsParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;
import org.onosproject.yangutils.utils.io.impl.YangToJavaNamingConflictUtil;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;
import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.addToCompilationRoot;
import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.copyYangFilesToTarget;
import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.resolveInterJarDependencies;
import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.serializeDataModel;
import static org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorUtil.generateJavaCode;
import static org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorUtil.translatorErrorHandler;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT_BASE_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getDirectory;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;

/**
 * Represents ONOS YANG utility maven plugin.
 * Goal of plugin is yang2java.
 * Execution phase is generate-sources.
 * requiresDependencyResolution at compile time.
 */
@Mojo(name = "yang2java", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = COMPILE,
        requiresProject = true)
public class YangUtilManager
        extends AbstractMojo {

    private YangNode rootNode;
    // YANG file information set.
    private Set<YangFileInfo> yangFileInfoSet = new HashSet<>();
    private YangUtilsParser yangUtilsParser = new YangUtilsParserManager();
    private YangLinker yangLinker = new YangLinkerManager();
    private YangFileInfo curYangFileInfo = new YangFileInfo();

    private Set<YangNode> yangNodeSet = new HashSet<>();

    private static final String DEFAULT_PKG = SLASH + getPackageDirPathFromJavaJPackage(DEFAULT_BASE_PKG);

    /**
     * Source directory for YANG files.
     */
    @Parameter(property = "yangFilesDir", defaultValue = "src/main/yang")
    private String yangFilesDir;

    /**
     * Source directory for generated files.
     */
    @Parameter(property = "genFilesDir", defaultValue = "src/main/java")
    private String genFilesDir;

    /**
     * Base directory for project.
     */
    @Parameter(property = "basedir", defaultValue = "${basedir}")
    private String baseDir;

    /**
     * Output directory.
     */
    @Parameter(property = "project.build.outputDirectory", required = true, defaultValue = "target/classes")
    private String outputDirectory;

    /**
     * Current maven project.
     */
    @Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    /**
     * Replacement required for period special character in the identifier.
     */
    @Parameter(property = "replacementForPeriod")
    private String replacementForPeriod;

    /**
     * Replacement required for underscore special character in the identifier.
     */
    @Parameter(property = "replacementForUnderscore")
    private String replacementForUnderscore;

    /**
     * Replacement required for hyphen special character in the identifier.
     */
    @Parameter(property = "replacementForHyphen")
    private String replacementForHyphen;

    /**
     * Prefix which is required for adding with the identifier.
     */
    @Parameter(property = "prefixForIdentifier")
    private String prefixForIdentifier;

    /**
     * Build context.
     */
    @Component
    private BuildContext context;

    @Parameter(readonly = true, defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    @Parameter(readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    private List<ArtifactRepository> remoteRepository;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException {

        try {

            /*
             * For deleting the generated code in previous build.
             */
            deleteDirectory(getDirectory(baseDir, genFilesDir) + DEFAULT_PKG);
            deleteDirectory(getDirectory(baseDir, outputDirectory));

            String searchDir = getDirectory(baseDir, yangFilesDir);
            String codeGenDir = getDirectory(baseDir, genFilesDir) + SLASH;

            // Creates conflict resolver and set values to it.
            YangToJavaNamingConflictUtil conflictResolver = new YangToJavaNamingConflictUtil();
            conflictResolver.setReplacementForPeriod(replacementForPeriod);
            conflictResolver.setReplacementForHyphen(replacementForHyphen);
            conflictResolver.setReplacementForUnderscore(replacementForUnderscore);
            conflictResolver.setPrefixForIdentifier(prefixForIdentifier);
            YangPluginConfig yangPlugin = new YangPluginConfig();
            yangPlugin.setCodeGenDir(codeGenDir);
            yangPlugin.setConflictResolver(conflictResolver);

            /*
             * Obtain the YANG files at a path mentioned in plugin and creates
             * YANG file information set.
             */
            createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));

            // Check if there are any file to translate, if not return.
            if (getYangFileInfoSet() == null || getYangFileInfoSet().isEmpty()) {
                // No files to translate
                return;
            }
            // Resolve inter jar dependency.
            resolveInterJardependency();

            // Carry out the parsing for all the YANG files.
            parseYangFileInfoSet();

            // Resolve dependencies using linker.
            resolveDependenciesUsingLinker();

            // Perform translation to JAVA.
            translateToJava(getYangFileInfoSet(), yangPlugin);

            // Serialize data model.
            serializeDataModel(getDirectory(baseDir, outputDirectory), getYangFileInfoSet(), project, true);

            addToCompilationRoot(getDirectory(baseDir, genFilesDir), project, context);

            copyYangFilesToTarget(getYangFileInfoSet(), getDirectory(baseDir, outputDirectory), project);
        } catch (IOException | ParserException e) {
            getLog().info(e);
            String fileName = "";
            if (getCurYangFileInfo() != null) {
                fileName = getCurYangFileInfo().getYangFileName();
            }
            try {
                translatorErrorHandler(getRootNode());
                deleteDirectory(getDirectory(baseDir, genFilesDir) + DEFAULT_PKG);
            } catch (IOException ex) {
                throw new MojoExecutionException(
                        "Error handler failed to delete files for data model node.");
            }
            throw new MojoExecutionException(
                    "Exception occured due to " + e.getLocalizedMessage() + " in " + fileName
                            + " YANG file.");
        }
    }

    /**
     * Returns the YANG node set.
     *
     * @return YANG node set
     */
    public Set<YangNode> getYangNodeSet() {
        return yangNodeSet;
    }

    /**
     * Resolved inter-jar dependencies.
     *
     * @throws IOException when fails to do IO operations
     */
    public void resolveInterJardependency() throws IOException {
        try {
            List<YangNode> interJarResolvedNodes = resolveInterJarDependencies(project, localRepository,
                    remoteRepository, getDirectory(baseDir, outputDirectory));
            for (YangNode node : interJarResolvedNodes) {
                YangFileInfo dependentFileInfo = new YangFileInfo();
                dependentFileInfo.setRootNode(node);
                dependentFileInfo.setForTranslator(false);
                dependentFileInfo.setYangFileName(node.getName());
                getYangFileInfoSet().add(dependentFileInfo);
            }
        } catch (IOException e) {
            throw new IOException("failed to resolve in interjar scenario.");
        }
    }

    /**
     * Links all the provided with the YANG file info set.
     *
     * @throws MojoExecutionException a violation in mojo excecution
     */
    public void resolveDependenciesUsingLinker()
            throws MojoExecutionException {
        createYangNodeSet();
        for (YangFileInfo yangFileInfo : getYangFileInfoSet()) {
            setCurYangFileInfo(yangFileInfo);
            try {
                yangLinker.resolveDependencies(getYangNodeSet());
            } catch (LinkerException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
    }

    /**
     * Creates YANG nodes set.
     */
    public void createYangNodeSet() {
        for (YangFileInfo yangFileInfo : getYangFileInfoSet()) {
            getYangNodeSet().add(yangFileInfo.getRootNode());
        }
    }

    /**
     * Parses all the provided YANG files and generates YANG data model tree.
     *
     * @throws IOException a violation in IO
     */
    public void parseYangFileInfoSet()
            throws IOException {
        for (YangFileInfo yangFileInfo : getYangFileInfoSet()) {
            setCurYangFileInfo(yangFileInfo);
            if (yangFileInfo.isForTranslator()) {
                try {
                    YangNode yangNode = yangUtilsParser.getDataModel(yangFileInfo.getYangFileName());
                    yangFileInfo.setRootNode(yangNode);
                    setRootNode(yangNode);
                } catch (ParserException e) {
                    String logInfo = "Error in file: " + e.getFileName();
                    if (e.getLineNumber() != 0) {
                        logInfo = logInfo + " at line: " + e.getLineNumber() + " at position: "
                                + e.getCharPositionInLine();

                    }
                    if (e.getMessage() != null) {
                        logInfo = logInfo + NEW_LINE + e.getMessage();
                    }
                    getLog().info(logInfo);
                    throw e;
                }
            }
        }
    }

    /**
     * Returns current root YANG node of data-model tree.
     *
     * @return current root YANG node of data-model tree
     */
    private YangNode getRootNode() {
        return rootNode;
    }

    /**
     * Sets current root YANG node of data-model tree.
     *
     * @param rootNode current root YANG node of data-model tree
     */
    private void setRootNode(YangNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Translates to java code corresponding to the YANG schema.
     *
     * @param yangFileInfoSet YANG file information
     * @param yangPlugin      YANG plugin config
     * @throws IOException when fails to generate java code file the current
     *                     node
     */
    public void translateToJava(Set<YangFileInfo> yangFileInfoSet, YangPluginConfig yangPlugin)
            throws IOException {
        Iterator<YangFileInfo> yangFileIterator = yangFileInfoSet.iterator();
        while (yangFileIterator.hasNext()) {
            YangFileInfo yangFileInfo = yangFileIterator.next();
            setCurYangFileInfo(yangFileInfo);
            if (yangFileInfo.isForTranslator()) {
                generateJavaCode(yangFileInfo.getRootNode(), yangPlugin);
            }
        }
    }

    /**
     * Creates a YANG file info set.
     *
     * @param yangFileList YANG files list
     */
    public void createYangFileInfoSet(List<String> yangFileList) {
        for (String yangFile : yangFileList) {
            YangFileInfo yangFileInfo = new YangFileInfo();
            yangFileInfo.setYangFileName(yangFile);
            getYangFileInfoSet().add(yangFileInfo);
        }
    }

    /**
     * Returns the YANG file info set.
     *
     * @return the YANG file info set
     */
    public Set<YangFileInfo> getYangFileInfoSet() {
        return yangFileInfoSet;
    }

    /**
     * Sets the YANG file info set.
     *
     * @param yangFileInfoSet the YANG file info set
     */
    public void setYangFileInfoSet(Set<YangFileInfo> yangFileInfoSet) {
        this.yangFileInfoSet = yangFileInfoSet;
    }

    /**
     * Returns current YANG file's info.
     *
     * @return the yangFileInfo
     */
    public YangFileInfo getCurYangFileInfo() {
        return curYangFileInfo;
    }

    /**
     * Sets current YANG file's info.
     *
     * @param yangFileInfo the yangFileInfo to set
     */
    public void setCurYangFileInfo(YangFileInfo yangFileInfo) {
        curYangFileInfo = yangFileInfo;
    }
}
