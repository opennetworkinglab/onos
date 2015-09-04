/*
 * Copyright 2015 Open Networking Laboratory
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
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.onlab.util.Tools;
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
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
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

    // Magic strings to search for and how deep to search it into the archive stream
    private static final String APP_MAGIC = "<app ";
    private static final int APP_MAGIC_DEPTH = 1024;

    private static final String NAME = "[@name]";
    private static final String ORIGIN = "[@origin]";
    private static final String VERSION = "[@version]";
    private static final String FEATURES_REPO = "[@featuresRepo]";
    private static final String FEATURES = "[@features]";
    private static final String DESCRIPTION = "description";

    private static final String ROLE = "security.role";
    private static final String APP_PERMISSIONS = "security.permissions.app-perm";
    private static final String NET_PERMISSIONS = "security.permissions.net-perm";
    private static final String JAVA_PERMISSIONS = "security.permissions.java-perm";

    private static final String OAR = ".oar";
    private static final String APP_XML = "app.xml";
    private static final String M2_PREFIX = "m2";

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
                expandZippedApplication(bis, desc);

                bis.reset();
                saveApplication(bis, desc);
            }

            installArtifacts(desc);
            return desc;
        } catch (IOException e) {
            throw new ApplicationException("Unable to save application", e);
        }
    }

    // Indicates whether the stream encoded in the given bytes is plain XML.
    private boolean isPlainXml(byte[] bytes) {
        return substring(bytes, XML_MAGIC.length()).equals(XML_MAGIC) ||
                substring(bytes, APP_MAGIC_DEPTH).contains(APP_MAGIC);
    }

    // Returns the substring of maximum possible length from the specified bytes.
    private String substring(byte[] bytes, int length) {
        return new String(bytes, 0, Math.min(bytes.length, length), Charset.forName("UTF-8"));
    }

    /**
     * Purges the application archive directory.
     *
     * @param appName application name
     */
    public synchronized void purgeApplication(String appName) {
        File appDir = new File(appsDir, appName);
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
     * will be either the application ZIP file or the application XML file.
     *
     * @param appName application name
     * @return application archive stream
     */
    public synchronized InputStream getApplicationInputStream(String appName) {
        try {
            File appFile = appFile(appName, appName + OAR);
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
        String desc = cfg.getString(DESCRIPTION);
        String origin = cfg.getString(ORIGIN);
        ApplicationRole role = getRole(cfg.getString(ROLE));
        Set<Permission> perms = getPermissions(cfg);
        String featRepo = cfg.getString(FEATURES_REPO);
        URI featuresRepo = featRepo != null ? URI.create(featRepo) : null;
        List<String> features = ImmutableList.copyOf(cfg.getString(FEATURES).split(","));

        return new DefaultApplicationDescription(name, version, desc, origin, role,
                                                 perms, featuresRepo, features);
    }

    // Expands the specified ZIP stream into app-specific directory.
    private void expandZippedApplication(InputStream stream, ApplicationDescription desc)
            throws IOException {
        ZipInputStream zis = new ZipInputStream(stream);
        ZipEntry entry;
        File appDir = new File(appsDir, desc.name());
        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                byte[] data = ByteStreams.toByteArray(zis);
                zis.closeEntry();
                File file = new File(appDir, entry.getName());
                createParentDirs(file);
                write(data, file);
            }
        }
        zis.close();
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
    private void saveApplication(InputStream stream, ApplicationDescription desc)
            throws IOException {
        Files.write(toByteArray(stream), appFile(desc.name(), desc.name() + OAR));
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
            return appFile(appName, "active").createNewFile() && updateTime(appName);
        } catch (IOException e) {
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
        return new File(new File(appsDir, appName), fileName);
    }

    // Returns the set of Permissions specified in the app.xml file
    private ImmutableSet<Permission> getPermissions(XMLConfiguration cfg) {
        List<Permission> permissionList = new ArrayList();

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

    //
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
}
