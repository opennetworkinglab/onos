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

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.onlab.util.Tools;
import org.onosproject.app.ApplicationDescription;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationException;
import org.onosproject.app.ApplicationStoreDelegate;
import org.onosproject.app.DefaultApplicationDescription;
import org.onosproject.core.Permission;
import org.onosproject.core.Version;
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
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.write;

/**
 * Facility for reading application archive stream and managing application
 * directory structure.
 */
public class ApplicationArchive
        extends AbstractStore<ApplicationEvent, ApplicationStoreDelegate> {

    private static final String NAME = "[@name]";
    private static final String ORIGIN = "[@origin]";
    private static final String VERSION = "[@version]";
    private static final String FEATURES_REPO = "[@featuresRepo]";
    private static final String FEATURES = "[@features]";
    private static final String DESCRIPTION = "description";

    private static Logger log = LoggerFactory.getLogger(ApplicationArchive.class);
    private static final String APP_XML = "app.xml";
    private static final String APPS_ROOT = "data/apps/";

    private File appsDir = new File(APPS_ROOT);

    /**
     * Sets the root directory where application artifacts are kept.
     *
     * @param appsRoot top-level applications directory path
     */
    protected void setAppsRoot(String appsRoot) {
        this.appsDir = new File(appsRoot);
    }

    /**
     * Returns the root directory where application artifacts are kept.
     *
     * @return top-level applications directory path
     */
    protected String getAppsRoot() {
        return appsDir.getPath();
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
            return loadAppDescription(new XMLConfiguration(appFile(appName, APP_XML)));
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
    public ApplicationDescription saveApplication(InputStream stream) {
        try (InputStream ais = stream) {
            byte[] cache = toByteArray(ais);
            InputStream bis = new ByteArrayInputStream(cache);
            ApplicationDescription desc = parseAppDescription(bis);
            bis.reset();

            expandApplication(bis, desc);
            bis.reset();

            saveApplication(bis, desc);
            installArtifacts(desc);
            return desc;
        } catch (IOException e) {
            throw new ApplicationException("Unable to save application", e);
        }
    }

    /**
     * Purges the application archive directory.
     *
     * @param appName application name
     */
    public void purgeApplication(String appName) {
        try {
            Tools.removeDirectory(new File(appsDir, appName));
        } catch (IOException e) {
            throw new ApplicationException("Unable to purge application " + appName, e);
        }
    }

    /**
     * Returns application archive stream for the specified application.
     *
     * @param appName application name
     * @return application archive stream
     */
    public InputStream getApplicationInputStream(String appName) {
        try {
            return new FileInputStream(appFile(appName, appName + ".zip"));
        } catch (FileNotFoundException e) {
            throw new ApplicationException("Application " + appName + " not found");
        }
    }

    // Scans the specified ZIP stream for app.xml entry and parses it producing
    // an application descriptor.
    private ApplicationDescription parseAppDescription(InputStream stream)
            throws IOException {
        try (ZipInputStream zis = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(APP_XML)) {
                    byte[] data = new byte[(int) entry.getSize()];
                    ByteStreams.readFully(zis, data);
                    XMLConfiguration cfg = new XMLConfiguration();
                    try {
                        cfg.load(new ByteArrayInputStream(data));
                        return loadAppDescription(cfg);
                    } catch (ConfigurationException e) {
                        throw new IOException("Unable to parse " + APP_XML, e);
                    }
                }
                zis.closeEntry();
            }
        }
        throw new IOException("Unable to locate " + APP_XML);
    }

    private ApplicationDescription loadAppDescription(XMLConfiguration cfg) {
        cfg.setAttributeSplittingDisabled(true);
        String name = cfg.getString(NAME);
        Version version = Version.version(cfg.getString(VERSION));
        String desc = cfg.getString(DESCRIPTION);
        String origin = cfg.getString(ORIGIN);
        Set<Permission> perms = ImmutableSet.of();
        String featRepo = cfg.getString(FEATURES_REPO);
        URI featuresRepo = featRepo != null ? URI.create(featRepo) : null;
        Set<String> features = ImmutableSet.copyOf(cfg.getString(FEATURES).split(","));

        return new DefaultApplicationDescription(name, version, desc, origin,
                                                 perms, featuresRepo, features);
    }

    // Expands the specified ZIP stream into app-specific directory.
    private void expandApplication(InputStream stream, ApplicationDescription desc)
            throws IOException {
        ZipInputStream zis = new ZipInputStream(stream);
        ZipEntry entry;
        File appDir = new File(appsDir, desc.name());
        while ((entry = zis.getNextEntry()) != null) {
            byte[] data = new byte[(int) entry.getSize()];
            ByteStreams.readFully(zis, data);
            zis.closeEntry();

            File file = new File(appDir, entry.getName());
            createParentDirs(file);
            write(data, file);
        }
        zis.close();
    }

    // Saves the specified ZIP stream into a file under app-specific directory.
    private void saveApplication(InputStream stream, ApplicationDescription desc)
            throws IOException {
        Files.write(toByteArray(stream), appFile(desc.name(), desc.name() + ".zip"));
    }

    // Installs application artifacts into M2 repository.
    private void installArtifacts(ApplicationDescription desc) {
        // FIXME: implement M2 repository copy
    }

    protected boolean setActive(String appName) {
        try {
            return appFile(appName, "active").createNewFile();
        } catch (IOException e) {
            throw new ApplicationException("Unable to mark app as active", e);
        }
    }

    protected boolean clearActive(String appName) {
        return appFile(appName, "active").delete();
    }

    protected boolean isActive(String appName) {
        return appFile(appName, "active").exists();
    }


    // Returns the name of the file located under the specified app directory.
    private File appFile(String appName, String fileName) {
        return new File(new File(appsDir, appName), fileName);
    }

}
