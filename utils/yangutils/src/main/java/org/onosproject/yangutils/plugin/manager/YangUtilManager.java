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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.YangUtilsParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;
import org.onosproject.yangutils.translator.tojava.utils.YangToJavaNamingConflictUtil;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;
import static org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorUtil.generateJavaCode;
import static org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorUtil.translatorErrorHandler;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT_BASE_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.addToSource;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.copyYangFilesToTarget;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getDirectory;
import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.findBelongsToModuleNode;

/**
 * Represents ONOS YANG utility maven plugin.
 * Goal of plugin is yang2java.
 * Execution phase is generate-sources.
 * requiresDependencyResolution at compile time.
 */
@Mojo(name = "yang2java", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = COMPILE,
        requiresProject = true)
public class YangUtilManager extends AbstractMojo {

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
     * Build context.
     */
    @Component
    private BuildContext context;

    private static final String DEFAULT_PKG = SLASH
            + getPackageDirPathFromJavaJPackage(DEFAULT_BASE_PKG);

    private YangUtilsParser yangUtilsParser = new YangUtilsParserManager();
    private YangNode rootNode;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {

            /*
             * For deleting the generated code in previous build.
             */
            deleteDirectory(getDirectory(baseDir, genFilesDir) + DEFAULT_PKG);
            deleteDirectory(getDirectory(baseDir, outputDirectory));

            String searchDir = getDirectory(baseDir, yangFilesDir);
            String codeGenDir = getDirectory(baseDir, genFilesDir) + SLASH;
            YangToJavaNamingConflictUtil conflictResolver = new YangToJavaNamingConflictUtil();
            conflictResolver.setReplacementForPeriod(replacementForPeriod);
            conflictResolver.setReplacementForHyphen(replacementForHyphen);
            conflictResolver.setReplacementForUnderscore(replacementForUnderscore);
            List<YangFileInfo> yangFileInfo = YangFileScanner.getYangFiles(searchDir);
            if (yangFileInfo == null || yangFileInfo.isEmpty()) {
                // no files to translate
                return;
            }
            YangPluginConfig yangPlugin = new YangPluginConfig();
            yangPlugin.setCodeGenDir(codeGenDir);
            yangPlugin.setConflictResolver(conflictResolver);

            Iterator<YangFileInfo> yangFileIterator = yangFileInfo.iterator();
            while (yangFileIterator.hasNext()) {
                YangFileInfo yangFile = yangFileIterator.next();
                try {
                    YangNode yangNode = yangUtilsParser.getDataModel(yangFile.getYangFileName());
                    yangFile.setRootNode(yangNode);
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

            resolveLinkingForSubModule(yangFileInfo);

            translateToJava(yangFileInfo, yangPlugin);

            addToSource(getDirectory(baseDir, genFilesDir) + DEFAULT_PKG, project, context);
            copyYangFilesToTarget(yangFileInfo, getDirectory(baseDir, outputDirectory), project);
        } catch (Exception e) {
            String fileName = "";
            if (e instanceof TranslatorException) {
                fileName = ((TranslatorException) e).getFileName();
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
     * Set current project.
     *
     * @param curProject maven project
     */
    public void setCurrentProject(MavenProject curProject) {
        project = curProject;

    }

    /**
     * Returns current project.
     *
     * @return current project
     */
    public MavenProject getCurrentProject() {
        return project;
    }

    /**
     * Returns current root YANG node of data-model tree.
     *
     * @return current root YANG node of data-model tree
     */
    public YangNode getRootNode() {
        return rootNode;
    }

    /**
     * Sets current root YANG node of data-model tree.
     *
     * @param rootNode current root YANG node of data-model tree
     */
    public void setRootNode(YangNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Translates to java code corresponding to the YANG schema.
     *
     * @param yangFileInfo YANG file information
     * @param yangPlugin YANG plugin config
     * @throws IOException when fails to generate java code file the current
     *             node
     */
    public static void translateToJava(List<YangFileInfo> yangFileInfo, YangPluginConfig yangPlugin)
            throws IOException {
        Iterator<YangFileInfo> yangFileIterator = yangFileInfo.iterator();
        while (yangFileIterator.hasNext()) {
            YangFileInfo yangFile = yangFileIterator.next();
            generateJavaCode(yangFile.getRootNode(), yangPlugin, yangFile.getYangFileName());
        }
    }

    /**
     * Resolves sub-module linking.
     *
     * @param yangFileInfo YANG file information
     * @throws DataModelException when belongs-to module node is not found
     */
    public static void resolveLinkingForSubModule(List<YangFileInfo> yangFileInfo) throws DataModelException {
        Iterator<YangFileInfo> yangFileIterator = yangFileInfo.iterator();
        while (yangFileIterator.hasNext()) {
            YangFileInfo yangFile = yangFileIterator.next();
            YangNode yangNode = yangFile.getRootNode();
            if (yangNode instanceof YangSubModule) {
                String belongsToModuleName = ((YangSubModule) yangNode).getBelongsTo()
                        .getBelongsToModuleName();
                YangNode moduleNode = findBelongsToModuleNode(yangFileInfo, belongsToModuleName);
                ((YangSubModule) yangNode).getBelongsTo().setModuleNode(moduleNode);
            }
        }
    }
}
