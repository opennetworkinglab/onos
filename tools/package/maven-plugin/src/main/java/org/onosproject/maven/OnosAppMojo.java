/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.maven;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.ByteStreams.toByteArray;
import static org.codehaus.plexus.util.FileUtils.copyFile;
import static org.codehaus.plexus.util.FileUtils.fileRead;
import static org.codehaus.plexus.util.FileUtils.fileWrite;
import static org.codehaus.plexus.util.FileUtils.forceMkdir;

/**
 * Produces ONOS application archive using the app.xml file information.
 */
@Mojo(name = "app", defaultPhase = LifecyclePhase.PACKAGE)
public class OnosAppMojo extends AbstractMojo {

    private static final String APP = "app";
    private static final String NAME = "[@name]";
    private static final String VERSION = "[@version]";
    private static final String FEATURES_REPO = "[@featuresRepo]";
    private static final String ARTIFACT = "artifact";

    private static final String APP_XML = "app.xml";
    private static final String APP_PNG = "app.png";
    private static final String FEATURES_XML = "features.xml";

    private static final String MVN_URL = "mvn:";
    private static final String M2_PREFIX = "m2";

    private static final String ONOS_APP_NAME = "onos.app.name";
    private static final String ONOS_APP_ORIGIN = "onos.app.origin";
    private static final String ONOS_APP_REQUIRES = "onos.app.requires";

    private static final String ONOS_APP_CATEGORY = "onos.app.category";
    private static final String ONOS_APP_URL = "onos.app.url";
    private static final String ONOS_APP_TITLE = "onos.app.title";
    private static final String ONOS_APP_README = "onos.app.readme";

    private static final String PROJECT_GROUP_ID = "project.groupId";
    private static final String PROJECT_ARTIFACT_ID = "project.artifactId";
    private static final String PROJECT_VERSION = "project.version";
    private static final String PROJECT_DESCRIPTION = "project.description";

    private static final String JAR = "jar";
    private static final String XML = "xml";
    private static final String APP_ZIP = "oar";
    private static final String PACKAGE_DIR = "oar";

    private static final String DEFAULT_ORIGIN = "ON.Lab";
    private static final String DEFAULT_VERSION = "${project.version}";

    private static final String DEFAULT_CATEGORY = "default";
    private static final String DEFAULT_URL = "http://onosproject.org";

    private static final String DEFAULT_FEATURES_REPO =
            "mvn:${project.groupId}/${project.artifactId}/${project.version}/xml/features";
    private static final String DEFAULT_ARTIFACT =
            "mvn:${project.groupId}/${project.artifactId}/${project.version}";

    private static final String PROP_START = "${";
    private static final String PROP_END = "}";

    private static final int BUFFER_SIZE = 8192;

    private String name;
    private String origin;
    private String requiredApps;
    private String category;
    private String url;
    private String title;
    private String readme;
    private String version = DEFAULT_VERSION;
    private String featuresRepo = DEFAULT_FEATURES_REPO;
    private List<String> artifacts;

    /**
     * The project base directory.
     */
    @Parameter(defaultValue = "${basedir}")
    protected File baseDir;

    /**
     * The directory where the generated catalogue file will be put.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected File dstDirectory;

    /**
     * The project group ID.
     */
    @Parameter(defaultValue = "${project.groupId}")
    protected String projectGroupId;

    /**
     * The project artifact ID.
     */
    @Parameter(defaultValue = "${project.artifactId}")
    protected String projectArtifactId;

    /**
     * The project version.
     */
    @Parameter(defaultValue = "${project.version}")
    protected String projectVersion;

    /**
     * The project version.
     */
    @Parameter(defaultValue = "${project.description}")
    protected String projectDescription;

    @Parameter(defaultValue = "${localRepository}")
    protected ArtifactRepository localRepository;

    /**
     * Maven project
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    /**
     * Maven project helper.
     */
    @Component
    protected MavenProjectHelper projectHelper;

    private File m2Directory;
    protected File stageDirectory;
    protected String projectPath;
    private Map<String, String> properties;

    @Override
    public void execute() throws MojoExecutionException {
        File appFile = new File(baseDir, APP_XML);
        File iconFile = new File(baseDir, APP_PNG);
        File featuresFile = new File(baseDir, FEATURES_XML);

        name = (String) project.getProperties().get(ONOS_APP_NAME);

        // If neither the app.xml file exists, nor the onos.app.name property
        // is defined, there is nothing for this Mojo to do, so bail.
        if (!appFile.exists() && name == null) {
            return;
        }

        m2Directory = new File(localRepository.getBasedir());
        stageDirectory = new File(dstDirectory, PACKAGE_DIR);
        projectPath = M2_PREFIX + "/" + artifactDir(projectGroupId, projectArtifactId, projectVersion);

        origin = (String) project.getProperties().get(ONOS_APP_ORIGIN);
        origin = origin != null ? origin : DEFAULT_ORIGIN;

        requiredApps = (String) project.getProperties().get(ONOS_APP_REQUIRES);
        requiredApps = requiredApps == null ? "" : requiredApps.replaceAll("[\\s]", "");

        category = (String) project.getProperties().get(ONOS_APP_CATEGORY);
        category = category != null ? category : DEFAULT_CATEGORY;

        url = (String) project.getProperties().get(ONOS_APP_URL);
        url = url != null ? url : DEFAULT_URL;

        // if title does not exist, fall back to the name
        title = (String) project.getProperties().get(ONOS_APP_TITLE);
        title = title != null ? title : name;

        // if readme does not exist, we simply fallback to use description
        readme = (String) project.getProperties().get(ONOS_APP_README);
        readme = readme != null ? readme : projectDescription;

        properties = buildProperties();

        if (appFile.exists()) {
            loadAppFile(appFile);
        } else {
            artifacts = ImmutableList.of(expand(DEFAULT_ARTIFACT));
        }

        // If there are any artifacts, stage the
        if (!artifacts.isEmpty()) {
            getLog().info("Building ONOS application package for " + name + " (v" + expand(version) + ")");
            artifacts.forEach(a -> getLog().debug("Including artifact: " + a));

            if (stageDirectory.exists() || stageDirectory.mkdirs()) {
                processAppXml(appFile);
                processAppPng(iconFile);
                processFeaturesXml(featuresFile);
                processArtifacts();
                generateAppPackage();
            } else {
                throw new MojoExecutionException("Unable to create directory: " + stageDirectory);
            }
        }
    }

    // Sets up a properties dictionary with the properties from the POM file,
    // some of which have been sanitized with nice defaults
    private Map<String, String> buildProperties() {
        Map<String, String> properties = new HashMap();
        project.getProperties().forEach((k, v) -> properties.put((String) k, (String) v));
        properties.put(PROJECT_GROUP_ID, projectGroupId);
        properties.put(PROJECT_ARTIFACT_ID, projectArtifactId);
        properties.put(PROJECT_VERSION, projectVersion);
        properties.put(PROJECT_DESCRIPTION, readme);
        properties.put(ONOS_APP_ORIGIN, origin);
        properties.put(ONOS_APP_REQUIRES, requiredApps);
        properties.put(ONOS_APP_CATEGORY, category);
        properties.put(ONOS_APP_URL, url);
        properties.put(ONOS_APP_TITLE, title);
        properties.put(ONOS_APP_README, readme);
        return properties;
    }

    // Loads the app.xml file.
    private void loadAppFile(File appFile) throws MojoExecutionException {
        XMLConfiguration xml = new XMLConfiguration();
        xml.setRootElementName(APP);

        try (FileInputStream stream = new FileInputStream(appFile)) {
            xml.load(stream);
            xml.setAttributeSplittingDisabled(true);
            xml.setDelimiterParsingDisabled(true);

            name = xml.getString(NAME);
            version = expand(xml.getString(VERSION));
            featuresRepo = expand(xml.getString(FEATURES_REPO));

            artifacts = xml.configurationsAt(ARTIFACT).stream()
                    .map(cfg -> expand(cfg.getRootNode().getValue().toString()))
                    .collect(Collectors.toList());

            stream.close();
        } catch (ConfigurationException e) {
            throw new MojoExecutionException("Unable to parse app.xml file", e);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Unable to find app.xml file", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read app.xml file", e);
        }
    }

    // Processes and stages the app.xml file.
    private void processAppXml(File appFile) throws MojoExecutionException {
        try {
            File file = new File(stageDirectory, APP_XML);
            forceMkdir(stageDirectory);
            String contents;

            if (appFile.exists()) {
                contents = fileRead(appFile);
            } else {
                byte[] bytes = toByteArray(getClass().getResourceAsStream(APP_XML));
                contents = new String(bytes);
            }
            fileWrite(file.getAbsolutePath(), expand(contents));
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to process app.xml", e);
        }
    }

    // Stages the app.png file of a specific application.
    private void processAppPng(File iconFile) throws MojoExecutionException {
        try {
            File stagedIconFile = new File(stageDirectory, APP_PNG);

            if (iconFile.exists()) {
                FileUtils.copyFile(iconFile, stagedIconFile);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy app.png", e);
        }
    }

    private void processFeaturesXml(File featuresFile) throws MojoExecutionException {
        boolean specified = featuresRepo != null && featuresRepo.length() > 0;

        // If featuresRepo attribute is specified and there is a features.xml
        // file present, add the features repo as an artifact
        try {
            if (specified && featuresFile.exists()) {
                processFeaturesXml(new FileInputStream(featuresFile));
            } else if (specified) {
                processFeaturesXml(getClass().getResourceAsStream(FEATURES_XML));
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Unable to find features.xml file", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to process features.xml file", e);
        }
    }

    // Processes and stages the features.xml file.
    private void processFeaturesXml(InputStream stream) throws IOException {
        String featuresArtifact =
                artifactFile(projectArtifactId, projectVersion, XML, "features");
        File dstDir = new File(stageDirectory, projectPath);
        forceMkdir(dstDir);
        String s = expand(new String(toByteArray(stream)));
        fileWrite(new File(dstDir, featuresArtifact).getAbsolutePath(), s);
    }

    // Stages all artifacts.
    private void processArtifacts() throws MojoExecutionException {
        for (String artifact : artifacts) {
            processArtifact(artifact);
        }
    }

    // Stages the specified artifact.
    private void processArtifact(String artifact) throws MojoExecutionException {
        if (!artifact.startsWith(MVN_URL)) {
            throw new MojoExecutionException("Unsupported artifact URL:" + artifact);
        }

        String[] fields = artifact.substring(4).split("/");
        if (fields.length < 3) {
            throw new MojoExecutionException("Illegal artifact URL:" + artifact);
        }

        try {
            String file = artifactFile(fields);

            if (projectGroupId.equals(fields[0]) && projectArtifactId.equals(fields[1])) {
                // Local artifact is not installed yet, package it from target directory.
                File dstDir = new File(stageDirectory, projectPath);
                forceMkdir(dstDir);
                copyFile(new File(dstDirectory, file), new File(dstDir, file));
            } else {
                // Other artifacts are packaged from ~/.m2/repository directory.
                String m2Path = artifactDir(fields);
                File srcDir = new File(m2Directory, m2Path);
                File dstDir = new File(stageDirectory, M2_PREFIX + "/" + m2Path);
                forceMkdir(dstDir);
                copyFile(new File(srcDir, file), new File(dstDir, file));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to stage artifact " + artifact, e);
        }
    }

    // Generates the ONOS package ZIP file.
    private void generateAppPackage() throws MojoExecutionException {
        File appZip = new File(dstDirectory, artifactFile(projectArtifactId, projectVersion,
                APP_ZIP, null));
        try (FileOutputStream fos = new FileOutputStream(appZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory("", stageDirectory, zos);
            projectHelper.attachArtifact(this.project, APP_ZIP, null, appZip);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to compress application package", e);
        }
    }

    // Generates artifact directory name from the specified fields.
    private String artifactDir(String[] fields) {
        return artifactDir(fields[0], fields[1], fields[2]);
    }

    // Generates artifact directory name from the specified elements.
    private String artifactDir(String gid, String aid, String version) {
        return gid.replace('.', '/') + "/" + aid + "/" + version;
    }

    // Generates artifact file name from the specified fields.
    private String artifactFile(String[] fields) {
        return fields.length < 5 ?
                artifactFile(fields[1], fields[2],
                        (fields.length < 4 ? JAR : fields[3]), null) :
                artifactFile(fields[1], fields[2], fields[3], fields[4]);
    }

    // Generates artifact file name from the specified elements.
    private String artifactFile(String aid, String version, String type,
                                String classifier) {
        return classifier == null ? aid + "-" + version + "." + type :
                aid + "-" + version + "-" + classifier + "." + type;
    }

    /**
     * Expands any environment variables in the specified string. These are
     * specified as ${property} tokens.
     *
     * @param string     string to be processed
     * @return original string with expanded substitutions
     */
    private String expand(String string) {
        return expand(string, properties);
    }

    /**
     * Expands any environment variables in the specified string. These are
     * specified as ${property} tokens.
     *
     * @param string     string to be processed
     * @param properties dictionary of property values to substitute
     * @return original string with expanded substitutions
     */
    private String expand(String string, Map<String, String> properties) {
        if (string == null) {
            return null;
        }

        String pString = string;
        StringBuilder sb = new StringBuilder();
        int start, end, last = 0;
        while ((start = pString.indexOf(PROP_START, last)) >= 0) {
            end = pString.indexOf(PROP_END, start + PROP_START.length());
            checkArgument(end > start, "Malformed property in %s", pString);
            sb.append(pString.substring(last, start));
            String prop = pString.substring(start + PROP_START.length(), end);
            String value;

            value = properties.get(prop);

            if (value == null) {
                sb.append(PROP_START).append(prop).append(PROP_END);
            } else {
                sb.append(value != null ? value : "");
            }
            last = end + 1;
        }
        sb.append(pString.substring(last));
        return sb.toString();
    }

    // Recursively archives the specified directory into a given ZIP stream.
    private void zipDirectory(String root, File dir, ZipOutputStream zos)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String path = root + file.getName() + "/";
                    zos.putNextEntry(new ZipEntry(path));
                    zipDirectory(path, file, zos);
                    zos.closeEntry();
                } else {
                    FileInputStream fin = new FileInputStream(file);
                    zos.putNextEntry(new ZipEntry(root + file.getName()));
                    int length;
                    while ((length = fin.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    fin.close();
                }
            }
        }
    }
}
