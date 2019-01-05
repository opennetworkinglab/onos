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
package org.onosproject.common.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.onlab.util.Tools;
import org.onlab.util.FilePathValidator;
import org.onosproject.app.ApplicationDescription;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationException;
import org.onosproject.app.ApplicationStoreDelegate;
import org.onosproject.app.DefaultApplicationDescription;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.Version;
import org.onosproject.security.AppPermission;
import org.onosproject.security.Permission;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.write;

/**
 * Facility for reading application archive stream and managing application
 * directory structure.
 */
public class ApplicationArchive
        extends AbstractStore<ApplicationEvent, ApplicationStoreDelegate> {

    private static Logger log = LoggerFactory.getLogger(ApplicationArchive.class);

    // Magic strings to search for at the beginning of the archive stream
    private static final String XML_MAGIC = "<?xml ";
    private static final String ZIP_MAGIC = "PK";

    // Magic strings to search for and how deep to search it into the archive stream
    private static final String APP_MAGIC = "<app ";
    private static final int APP_MAGIC_DEPTH = 1024;

    private static final String NAME = "[@name]";
    private static final String ORIGIN = "[@origin]";
    private static final String VERSION = "[@version]";
    private static final String FEATURES_REPO = "[@featuresRepo]";
    private static final String FEATURES = "[@features]";
    private static final String APPS = "[@apps]";
    private static final String DESCRIPTION = "description";

    private static final String UTILITY = "utility";

    private static final String CATEGORY = "[@category]";
    private static final String URL = "[@url]";
    private static final String TITLE = "[@title]";

    private static final String ROLE = "security.role";
    private static final String APP_PERMISSIONS = "security.permissions.app-perm";
    private static final String NET_PERMISSIONS = "security.permissions.net-perm";
    private static final String JAVA_PERMISSIONS = "security.permissions.java-perm";

    private static final String JAR = ".jar";
    private static final String OAR = ".oar";
    private static final String APP_XML = "app.xml";
    private static final String APP_PNG = "app.png";
    private static final String M2_PREFIX = "m2";
    private static final String FEATURES_XML = "features.xml";

    private static final String ROOT = "../";
    private static final String M2_ROOT = "system/";
    private static final String APPS_ROOT = "apps/";

    private File root = new File(ROOT);
    private File appsDir = new File(root, APPS_ROOT);
    private File m2Dir = new File(M2_ROOT);

    /**
     * Sets the root directory where apps directory is contained.
     *
     * @param root top-level directory path
     */
    protected void setRootPath(String root) {
        this.root = new File(root);
        this.appsDir = new File(this.root, APPS_ROOT);
        this.m2Dir = new File(M2_ROOT);
    }

    /**
     * Returns the root directory where apps directory is contained.
     *
     * @return top-level directory path
     */
    public String getRootPath() {
        return root.getPath();
    }

    /**
     * Returns the set of installed application names.
     *
     * @return installed application names
     */
    public Set<String> getApplicationNames() {
        ImmutableSet.Builder<String> names = ImmutableSet.builder();
        File[] files = appsDir.listFiles(File::isDirectory);
        if (files != null) {
            for (File file : files) {
                names.add(file.getName());
            }
        }
        return names.build();
    }

    /**
     * Returns the timestamp in millis since start of epoch, of when the
     * specified application was last modified or changed state.
     *
     * @param appName application name
     * @return number of millis since start of epoch
     */
    public long getUpdateTime(String appName) {
        return appFile(appName, APP_XML).lastModified();
    }

    /**
     * Loads the application descriptor from the specified application archive
     * stream and saves the stream in the appropriate application archive
     * directory.
     *
     * @param appName application name
     * @return application descriptor
     * @throws org.onosproject.app.ApplicationException if unable to read application description
     */
    public ApplicationDescription getApplicationDescription(String appName) {
        try {
            XMLConfiguration cfg = new XMLConfiguration();
            cfg.setAttributeSplittingDisabled(true);
            cfg.setDelimiterParsingDisabled(true);
            cfg.load(appFile(appName, APP_XML));
            return loadAppDescription(cfg);
        } catch (Exception e) {
            throw new ApplicationException("Unable to get app description", e);
        }
    }

    /**
     * Loads the application descriptor from the specified application archive
     * stream and saves the stream in the appropriate application archive
     * directory.
     *
     * @param stream application archive stream
     * @return application descriptor
     * @throws org.onosproject.app.ApplicationException if unable to read the
     *                                                  archive stream or store
     *                                                  the application archive
     */
    public synchronized ApplicationDescription saveApplication(InputStream stream) {
        try (InputStream ais = stream) {
            byte[] cache = toByteArray(ais);
            InputStream bis = new ByteArrayInputStream(cache);

            boolean plainXml = isPlainXml(cache);
            ApplicationDescription desc = plainXml ?
                    parsePlainAppDescription(bis) : parseZippedAppDescription(bis);
            checkState(!appFile(desc.name(), APP_XML).exists(),
                    "Application %s already installed", desc.name());

            if (plainXml) {
                expandPlainApplication(cache, desc);
            } else {
                bis.reset();
                boolean isSelfContainedJar = expandZippedApplication(bis, desc);

                if (isSelfContainedJar) {
                    bis.reset();
                    stageSelfContainedJar(bis, desc);
                }

                /*
                 * Reset the ZIP file and reparse the app description now
                 * that the ZIP is expanded onto the filesystem. This way any
                 * file referenced as part of the description (i.e. app.png)
                 * can be loaded into the app description.
                 */
                bis.reset();
                desc = parseZippedAppDescription(bis);

                bis.reset();
                saveApplication(bis, desc, isSelfContainedJar);
            }

            installArtifacts(desc);
            return desc;
        } catch (IOException e) {
            throw new ApplicationException("Unable to save application", e);
        }
    }

    // Indicates whether the stream encoded in the given bytes is plain XML.
    private boolean isPlainXml(byte[] bytes) {
        return !substring(bytes, ZIP_MAGIC.length()).equals(ZIP_MAGIC) &&
                (substring(bytes, XML_MAGIC.length()).equals(XML_MAGIC) ||
                 substring(bytes, APP_MAGIC_DEPTH).contains(APP_MAGIC));
    }

    // Returns the substring of maximum possible length from the specified bytes.
    private String substring(byte[] bytes, int length) {
        return new String(bytes, 0, Math.min(bytes.length, length), StandardCharsets.UTF_8);
    }

    /**
     * Purges the application archive directory.
     *
     * @param appName application name
     */
    public synchronized void purgeApplication(String appName) {
        File appDir = new File(appsDir, appName);
        if (!FilePathValidator.validateFile(appDir, appsDir)) {
            throw new ApplicationException("Application attempting to create files outside the apps directory");
        }
        try {
            Tools.removeDirectory(appDir);
        } catch (IOException e) {
            throw new ApplicationException("Unable to purge application " + appName, e);
        }
        if (appDir.exists()) {
            throw new ApplicationException("Unable to purge application " + appName);
        }
    }

    /**
     * Returns application archive stream for the specified application. This
     * will be either the application OAR file, JAR file or the plain XML file.
     *
     * @param appName application name
     * @return application archive stream
     */
    public synchronized InputStream getApplicationInputStream(String appName) {
        try {
            File appFile = appFile(appName, appName + OAR);
            if (!appFile.exists()) {
                appFile = appFile(appName, appName + JAR);
            }
            return new FileInputStream(appFile.exists() ? appFile : appFile(appName, APP_XML));
        } catch (FileNotFoundException e) {
            throw new ApplicationException("Application " + appName + " not found");
        }
    }

    // Scans the specified ZIP stream for app.xml entry and parses it producing
    // an application descriptor.
    private ApplicationDescription parseZippedAppDescription(InputStream stream)
            throws IOException {
        try (ZipInputStream zis = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(APP_XML)) {
                    byte[] data = ByteStreams.toByteArray(zis);
                    return parsePlainAppDescription(new ByteArrayInputStream(data));
                }
                zis.closeEntry();
            }
        }
        throw new IOException("Unable to locate " + APP_XML);
    }

    // Scans the specified XML stream and parses it producing an application descriptor.
    private ApplicationDescription parsePlainAppDescription(InputStream stream)
            throws IOException {
        XMLConfiguration cfg = new XMLConfiguration();
        cfg.setAttributeSplittingDisabled(true);
        cfg.setDelimiterParsingDisabled(true);
        try {
            cfg.load(stream);
            return loadAppDescription(cfg);
        } catch (ConfigurationException e) {
            throw new IOException("Unable to parse " + APP_XML, e);
        }
    }

    private ApplicationDescription loadAppDescription(XMLConfiguration cfg) {
        String name = cfg.getString(NAME);
        Version version = Version.version(cfg.getString(VERSION));
        String origin = cfg.getString(ORIGIN);

        String title = cfg.getString(TITLE);
        // FIXME: title should be set as attribute to APP, but fallback for now...
        title = title == null ? name : title;

        String category = cfg.getString(CATEGORY, UTILITY);
        String url = cfg.getString(URL);
        byte[] icon = getApplicationIcon(name);
        ApplicationRole role = getRole(cfg.getString(ROLE));
        Set<Permission> perms = getPermissions(cfg);
        String featRepo = cfg.getString(FEATURES_REPO);
        URI featuresRepo = featRepo != null ? URI.create(featRepo) : null;
        List<String> features = ImmutableList.copyOf(cfg.getString(FEATURES).split(","));

        String apps = cfg.getString(APPS, "");
        List<String> requiredApps = apps.isEmpty() ?
                ImmutableList.of() : ImmutableList.copyOf(apps.split(","));

        // put full description to readme field
        String readme = cfg.getString(DESCRIPTION);

        // put short description to description field
        String desc = compactDescription(readme);

        return DefaultApplicationDescription.builder()
            .withName(name)
            .withVersion(version)
            .withTitle(title)
            .withDescription(desc)
            .withOrigin(origin)
            .withCategory(category)
            .withUrl(url)
            .withReadme(readme)
            .withIcon(icon)
            .withRole(role)
            .withPermissions(perms)
            .withFeaturesRepo(featuresRepo)
            .withFeatures(features)
            .withRequiredApps(requiredApps)
            .build();
    }

    // Expands the specified ZIP stream into app-specific directory.
    // Returns true of the application is a self-contained jar rather than an oar file.
    private boolean expandZippedApplication(InputStream stream, ApplicationDescription desc)
            throws IOException {
        boolean isSelfContained = false;
        ZipInputStream zis = new ZipInputStream(stream);
        ZipEntry entry;
        File appDir = new File(appsDir, desc.name());
        if (!FilePathValidator.validateFile(appDir, appsDir)) {
            throw new ApplicationException("Application attempting to create files outside the apps directory");
        }
        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                byte[] data = ByteStreams.toByteArray(zis);
                zis.closeEntry();
                if (FilePathValidator.validateZipEntry(entry, appDir)) {
                    File file = new File(appDir, entry.getName());
                    if (isTopLevel(file)) {
                        createParentDirs(file);
                        write(data, file);
                    } else {
                        isSelfContained = true;
                    }
                } else {
                    throw new ApplicationException("Application Zip archive is attempting to leave application root");
                }
            }
        }
        zis.close();
        return isSelfContained;
    }

    // Returns true if the specified file is a top-level app file, i.e. app.xml,
    // features.xml, .jar or a directory; false if anything else.
    private boolean isTopLevel(File file) {
        String name = file.getName();
        return name.equals(APP_PNG)
            || name.equals(APP_XML)
            || name.endsWith(FEATURES_XML)
            || name.endsWith(JAR)
            || file.isDirectory();
    }

    // Expands the self-contained JAR stream into the app-specific directory,
    // using the bundle coordinates retrieved from the features.xml file.
    private void stageSelfContainedJar(InputStream stream, ApplicationDescription desc)
            throws IOException {
        // First extract the bundle coordinates
        String coords = getSelfContainedBundleCoordinates(desc);
        if (coords == null) {
            return;
        }

        // Split the coordinates into segments and build the file name.
        String[] f = coords.substring(4).split("/");
        String base = "m2/" + f[0].replace('.', '/') + "/" + f[1] + "/" + f[2] + "/" + f[1] + "-" + f[2];
        String jarName = base + (f.length < 4 ? "" : "-" + f[3]) + ".jar";
        String featuresName =  base + "-features.xml";

        // Create the file directory structure and copy the file there.
        File jar = appFile(desc.name(), jarName);
        boolean ok = jar.getParentFile().exists() || jar.getParentFile().mkdirs();
        if (ok) {
            Files.write(toByteArray(stream), jar);
            Files.copy(appFile(desc.name(), FEATURES_XML), appFile(desc.name(), featuresName));
            if (!appFile(desc.name(), FEATURES_XML).delete()) {
                log.warn("Unable to delete self-contained application {} features.xml", desc.name());
            }
        } else {
            throw new IOException("Unable to save self-contained application " + desc.name());
        }
    }

    // Returns the bundle coordinates from the features.xml file.
    private String getSelfContainedBundleCoordinates(ApplicationDescription desc) {
        try {
            XMLConfiguration cfg = new XMLConfiguration();
            cfg.setAttributeSplittingDisabled(true);
            cfg.setDelimiterParsingDisabled(true);
            cfg.load(appFile(desc.name(), FEATURES_XML));
            return cfg.getString("feature.bundle")
                    .replaceFirst("wrap:", "")
                    .replaceFirst("\\$Bundle-.*$", "");
        } catch (ConfigurationException e) {
            log.warn("Self-contained application {} has no features.xml", desc.name());
            return null;
        }
    }

    // Saves the specified XML stream into app-specific directory.
    private void expandPlainApplication(byte[] stream, ApplicationDescription desc)
            throws IOException {
        File file = appFile(desc.name(), APP_XML);
        checkState(!file.getParentFile().exists(), "Application already installed");
        createParentDirs(file);
        write(stream, file);
    }

    // Saves the specified ZIP stream into a file under app-specific directory.
    private void saveApplication(InputStream stream, ApplicationDescription desc,
                                 boolean isSelfContainedJar)
            throws IOException {
        String name = desc.name() + (isSelfContainedJar ? JAR : OAR);
        Files.write(toByteArray(stream), appFile(desc.name(), name));
    }

    // Installs application artifacts into M2 repository.
    private void installArtifacts(ApplicationDescription desc) throws IOException {
        try {
            Tools.copyDirectory(appFile(desc.name(), M2_PREFIX), m2Dir);
        } catch (NoSuchFileException e) {
            log.debug("Application {} has no M2 artifacts", desc.name());
        }
    }

    /**
     * Marks the app as active by creating token file in the app directory.
     *
     * @param appName application name
     * @return true if file was created
     */
    protected boolean setActive(String appName) {
        try {
            File active = appFile(appName, "active");
            createParentDirs(active);
            return active.createNewFile() && updateTime(appName);
        } catch (IOException e) {
            log.warn("Unable to mark app {} as active", appName, e);
            throw new ApplicationException("Unable to mark app as active", e);
        }
    }

    /**
     * Clears the app as active by deleting token file in the app directory.
     *
     * @param appName application name
     * @return true if file was deleted
     */
    protected boolean clearActive(String appName) {
        return appFile(appName, "active").delete() && updateTime(appName);
    }

    /**
     * Updates the time-stamp of the app descriptor file.
     *
     * @param appName application name
     * @return true if the app descriptor was updated
     */
    protected boolean updateTime(String appName) {
        return appFile(appName, APP_XML).setLastModified(System.currentTimeMillis());
    }

    /**
     * Indicates whether the app was marked as active by checking for token file.
     *
     * @param appName application name
     * @return true if the app is marked as active
     */
    protected boolean isActive(String appName) {
        return appFile(appName, "active").exists();
    }

    // Returns the name of the file located under the specified app directory.
    private File appFile(String appName, String fileName) {
        File file = new File(new File(appsDir, appName), fileName);
        if (!FilePathValidator.validateFile(file, appsDir)) {
            throw new ApplicationException("Application attempting to create files outside the apps directory");
        }
        return file;
    }

    // Returns the icon file located under the specified app directory.
    private File iconFile(String appName, String fileName) {
        return new File(new File(appsDir, appName), fileName);
    }

    // Returns the set of Permissions specified in the app.xml file
    private ImmutableSet<Permission> getPermissions(XMLConfiguration cfg) {
        List<Permission> permissionList = Lists.newArrayList();

        for (Object o : cfg.getList(APP_PERMISSIONS)) {
            String name = (String) o;
            permissionList.add(new Permission(AppPermission.class.getName(), name));
        }
        for (Object o : cfg.getList(NET_PERMISSIONS)) {
            //TODO: TO BE FLESHED OUT WHEN NETWORK PERMISSIONS ARE SUPPORTED
            break;
        }

        List<HierarchicalConfiguration> fields =
                cfg.configurationsAt(JAVA_PERMISSIONS);
        for (HierarchicalConfiguration sub : fields) {
            String classname = sub.getString("classname");
            String name = sub.getString("name");
            String actions = sub.getString("actions");

            if (classname != null && name != null) {
                permissionList.add(new Permission(classname, name, actions));
            }
        }
        return ImmutableSet.copyOf(permissionList);
    }

    // Returns the byte stream from icon.png file in oar application archive.
    private byte[] getApplicationIcon(String appName) {
        File iconFile = iconFile(appName, APP_PNG);
        try {
            final InputStream iconStream;
            if (iconFile.exists()) {
                iconStream = new FileInputStream(iconFile);
            } else {
                // assume that we can always fallback to default icon
                iconStream = ApplicationArchive.class.getResourceAsStream("/" + APP_PNG);
            }
            byte[] icon = ByteStreams.toByteArray(iconStream);
            iconStream.close();
            return icon;
        } catch (IOException e) {
            log.warn("Unable to read app icon for app {}", appName, e);
        }
        return new byte[0];
    }

    // Returns application role type
    public ApplicationRole getRole(String value) {
        if (value == null) {
            return ApplicationRole.UNSPECIFIED;
        } else {
            try {
                return ApplicationRole.valueOf(value.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                log.debug("Unknown role value: %s", value);
                return ApplicationRole.UNSPECIFIED;
            }
        }
    }

    // Returns the first sentence of the given sentence
    private String compactDescription(String sentence) {
        if (StringUtils.isNotEmpty(sentence)) {
            if (StringUtils.contains(sentence, ".")) {
                return StringUtils.substringBefore(sentence, ".") + ".";
            } else {
                return sentence;
            }
        }
        return sentence;
    }
}
