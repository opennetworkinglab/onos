/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onlab.osgiwrap;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.write;
import static java.nio.file.Files.walkFileTree;

/**
 * BND-based wrapper to convert Buck/Bazel JARs to OSGi-compatible JARs.
 */
public class OSGiWrapper {
    // Resources that need to be modified in the original jar
    private static final String _CFGDEF_JAR = "_cfgdef.jar";
    private static final String MODEL_SRCJAR = "model.srcjar";
    private static final String SCHEMA_JAR = "schema.jar";

    private static final String NONE = "NONE";

    private String inputJar;
    private String outputJar;
    private List<String> classpath;

    private String bundleName;
    private String groupId;
    private String bundleSymbolicName;
    private String bundleVersion;

    private String importPackages;
    private String dynamicimportPackages;

    private String exportPackages;
    private String includeResources;
    private Set<String> includedResources = Sets.newHashSet();

    private String bundleDescription;
    private String bundleLicense;

    private String webContext;
    private String webXmlRoot;
    private String destdir;

    private String bundleClasspath;
    private String karafCommands;

    private String fragmentHost;
    private boolean debug;

    // FIXME should consider using Commons CLI, etc.
    public static void main(String[] args) {
        if (args.length < 18) {
            System.err.println("Not enough args");
            System.exit(1);
        }

        String jar = args[0];
        String output = args[1];
        String cp = args[2];
        String name = args[3];
        String group = args[4];
        String version = args[5];
        String license = args[6];
        String importPackages = args[7];
        String exportPackages = args[8];
        String includeResources = args[9];
        String webContext = args[10];
        String webXmlRoot = args[11];
        String dynamicimportPackages = args[12];
        String destdir = args[13];
        String bundleClasspath = args[14];
        String karafCommands = args[15];
        String fragmentHost = args[16];
        String debug = args[17];
        String desc = Joiner.on(' ').join(Arrays.copyOfRange(args, 18, args.length));

        OSGiWrapper wrapper = new OSGiWrapper(jar, output, cp,
                name, group,
                version, license,
                importPackages, exportPackages,
                includeResources,
                webContext,
                webXmlRoot,
                dynamicimportPackages,
                desc,
                destdir,
                bundleClasspath,
                karafCommands,
                fragmentHost,
                debug);
        wrapper.log(wrapper + "\n");
        if (!wrapper.execute()) {
            System.err.printf("Error generating %s\n", name);
            System.exit(2);
        }
    }


    public OSGiWrapper(String inputJar,
                       String outputJar,
                       String classpath,
                       String bundleName,
                       String groupId,
                       String bundleVersion,
                       String bundleLicense,
                       String importPackages,
                       String exportPackages,
                       String includeResources,
                       String webContext,
                       String webXmlRoot,
                       String dynamicimportPackages,
                       String bundleDescription,
                       String destdir,
                       String bundleClasspath,
                       String karafCommands,
                       String fragmentHost,
                       String debug) {
        this.inputJar = inputJar;
        this.classpath = Lists.newArrayList(classpath.split(":"));
        if (!this.classpath.contains(inputJar)) {
            this.classpath.add(0, inputJar);
        }
        this.outputJar = outputJar;

        this.bundleName = bundleName;
        this.groupId = groupId;
        this.bundleSymbolicName = String.format("%s.%s", groupId, bundleName);

        this.bundleVersion = bundleVersion;
        this.bundleLicense = bundleLicense;
        this.bundleDescription = bundleDescription;

        this.importPackages = importPackages;
        this.dynamicimportPackages = dynamicimportPackages;
        if (Objects.equals(dynamicimportPackages, "''")) {
            this.dynamicimportPackages = null;
        }
        this.exportPackages = exportPackages;
        if (!Objects.equals(includeResources, NONE)) {
            this.includeResources = includeResources;
        }

        this.webContext = webContext;
        this.webXmlRoot = webXmlRoot;
        this.destdir = destdir;

        this.bundleClasspath = bundleClasspath;
        this.karafCommands = karafCommands;

        this.fragmentHost = fragmentHost;
        this.debug = Boolean.parseBoolean(debug);
    }

    private void setProperties(Analyzer analyzer) {
        analyzer.setProperty(Analyzer.BUNDLE_NAME, bundleName);
        analyzer.setProperty(Analyzer.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
        analyzer.setProperty(Analyzer.BUNDLE_VERSION, bundleVersion.replace('-', '.'));

        analyzer.setProperty(Analyzer.BUNDLE_DESCRIPTION, bundleDescription);
        analyzer.setProperty(Analyzer.BUNDLE_LICENSE, bundleLicense);

        //TODO consider using stricter version policy
        //analyzer.setProperty("-provider-policy", "${range;[===,==+)}");
        //analyzer.setProperty("-consumer-policy", "${range;[===,==+)}");

        analyzer.setProperty(Analyzer.DYNAMICIMPORT_PACKAGE, dynamicimportPackages);
        analyzer.setProperty(Analyzer.DSANNOTATIONS_OPTIONS, "inherit");

        // TODO include version in export, but not in import
        analyzer.setProperty(Analyzer.EXPORT_PACKAGE, exportPackages);

        // TODO we may need INCLUDE_RESOURCE, or that might be done by Buck
        if (includeResources != null) {
            analyzer.setProperty(Analyzer.INCLUDE_RESOURCE, includeResources);
        }

        // There are no good defaults so make sure you set the Import-Package
        analyzer.setProperty(Analyzer.IMPORT_PACKAGE, importPackages);

        if (isWab()) {
            analyzer.setProperty(Analyzer.WAB, webXmlRoot);
            analyzer.setProperty("Web-ContextPath", webContext);
            analyzer.setProperty(Analyzer.IMPORT_PACKAGE, importPackages +
                    ",org.glassfish.jersey.servlet,org.jvnet.mimepull\n");
        }
        analyzer.setProperty("Karaf-Commands", karafCommands);

        analyzer.setProperty(Analyzer.FRAGMENT_HOST, fragmentHost);
    }

    public boolean execute() {
        Analyzer analyzer = new Builder();
        try {
            // Sanitize the input jar content
            inputJar = modifyJar(inputJar);

            Jar jar = new Jar(new File(inputJar));  // where our data is
            analyzer.setJar(jar);                   // give bnd the contents

            // You can provide additional class path entries to allow
            // bnd to pickup export version from the packageinfo file,
            // Version annotation, or their manifests.
            analyzer.addClasspath(classpath);

            setProperties(analyzer);

            // ------------- let's begin... -------------------------

            // Analyze the target JAR first
            analyzer.analyze();

            if (includeResources != null) {
                doIncludeResources(analyzer);
            }

            // Repack the JAR as a WAR
            doWabStaging(analyzer);

            // Calculate the manifest
            Manifest manifest = analyzer.calcManifest();

            if (analyzer.isOk()) {
                analyzer.getJar().setManifest(manifest);
                if (analyzer.save(new File(outputJar), true)) {
                    log("Saved!\n");
                } else {
                    warn("Failed to create jar \n");
                    return false;
                }
            } else {
                warn("Analyzer Errors:\n%s\n", analyzer.getErrors());
                return false;
            }

            analyzer.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Extract the jar and melds its content with the jar produced by Bazel
    private void addJarToJar(JarEntry entryJar, JarInputStream jis, JarOutputStream jos) throws IOException {
        File file = new File(new File(destdir), entryJar.getName());
        byte[] data = ByteStreams.toByteArray(jis);
        createParentDirs(file);
        write(data, file);
        // Entry jar input stream which points to the inner jar resources (cfgdef, model,...)
        try (JarInputStream innerJis = new JarInputStream(new FileInputStream(file))) {
            JarEntry entry;
            byte[] byteBuff = new byte[1024];
            while ((entry = innerJis.getNextJarEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (!entry.getName().contains("..")) {
                        jos.putNextEntry(entry);
                        for (int bytesRead; (bytesRead = innerJis.read(byteBuff)) != -1; ) {
                            jos.write(byteBuff, 0, bytesRead);
                        }
                    } else {
                        throw new IOException("Jar " + entryJar + " is corrupted");
                    }
                }
                innerJis.closeEntry();
            }
        }
    }

    // Modify the specified jar to fix the resource_jars loaded by Bazel.
    // Starting from Bazel 5, resource_jars are no longer supported and
    // we have to load them as resources. This means that we expand them
    // and we set the right path in OSGi-compatible JAR.
    private String modifyJar(String inputJar) throws IOException {
        // libonos-xxx input and libonos-xxx-new which is the sanitized final jar
        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(inputJar + "new"))) {
            JarEntry entry;
            byte[] byteBuff = new byte[1024];
            while ((entry = jis.getNextJarEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (!entry.getName().contains("..")) {
                        // We add the content but we don't write them again in the new jar
                        if (entry.getName().contains(bundleName + OSGiWrapper._CFGDEF_JAR) ||
                                entry.getName().contains(SCHEMA_JAR) ||
                                entry.getName().contains(MODEL_SRCJAR)) {
                            addJarToJar(entry, jis, jos);
                        } else {
                            jos.putNextEntry(entry);
                            for (int bytesRead; (bytesRead = jis.read(byteBuff)) != -1; ) {
                                jos.write(byteBuff, 0, bytesRead);
                            }
                        }
                    } else {
                        throw new IOException("Jar " + inputJar + " is corrupted");
                    }
                }
                jis.closeEntry();
            }
        }
        return inputJar + "new";
    }

    private boolean isWab() {
        return !Objects.equals(webContext, NONE);
    }

    private void doWabStaging(Analyzer analyzer) throws Exception {
        if (!isWab()) {
            return;
        }
        String wab = analyzer.getProperty(analyzer.WAB);
        Jar dot = analyzer.getJar();

        log("wab %s", wab);

        String specifiedClasspath = this.bundleClasspath;
        String bundleClasspath = "WEB-INF/classes";
        if (specifiedClasspath != null) {
            bundleClasspath += "," + specifiedClasspath;
        }
        analyzer.setBundleClasspath(bundleClasspath);

        Set<String> paths = new HashSet<>(dot.getResources().keySet());

        for (String path : paths) {
            if (path.indexOf('/') > 0 && !Character.isUpperCase(path.charAt(0))) {
                log("wab: moving: %s", path);
                dot.rename(path, "WEB-INF/classes/" + path);
            }
        }

        Path wabRoot = Paths.get(wab);
        log("wab root " + wabRoot.toString());
        includeFiles(dot, null, wabRoot.toString());
    }

    /**
     * Parse the Bundle-Includes header. Files in the bundles Include header are
     * included in the jar. The source can be a directory or a file.
     *
     * @throws Exception
     */
    private void doIncludeResources(Analyzer analyzer) throws Exception {
        String includes = analyzer.getProperty(Analyzer.INCLUDE_RESOURCE);
        if (includes == null) {
            return;
        }
        Parameters clauses = analyzer.parseHeader(includes);
        Jar jar = analyzer.getJar();

        for (Map.Entry<String, Attrs> entry : clauses.entrySet()) {
            String name = entry.getKey();
            Map<String, String> extra = entry.getValue();
            // TODO consider doing something with extras

            String[] parts = name.split("\\s*=\\s*");
            String source = parts[0];
            String destination = parts[0];
            if (parts.length == 2) {
                source = parts[1];
            }

            includeFiles(jar, destination, source);
        }
    }

    private void includeFiles(Jar jar, String destinationRoot, String sourceRoot)
            throws IOException {
        Path sourceRootPath = Paths.get(sourceRoot);
        // iterate through sources
        // put each source on the jar
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceRootPath.relativize(file);
                String destination = destinationRoot != null ?
                        destinationRoot + "/" + relativePath.toString() : //TODO
                        relativePath.toString();

                addFileToJar(jar, destination, file.toAbsolutePath().toString());
                return FileVisitResult.CONTINUE;
            }
        };
        File dir = new File(sourceRoot);
        if (dir.isFile()) {
            addFileToJar(jar, destinationRoot, dir.getAbsolutePath());
        } else if (dir.isDirectory()) {
            walkFileTree(sourceRootPath, visitor);
        } else {
            warn("Skipping resource in bundle %s: %s (File Not Found)\n",
                    bundleSymbolicName, sourceRoot);
        }
    }

    private void addFileToJar(Jar jar, String destination, String sourceAbsPath) throws IOException {
        if (includedResources.contains(sourceAbsPath)) {
            log("Skipping already included resource: %s\n", sourceAbsPath);
            return;
        }
        File file = new File(sourceAbsPath);
        if (!file.isFile()) {
            throw new RuntimeException(
                    String.format("Skipping non-existent file: %s\n", sourceAbsPath));
        }
        Resource resource = new FileResource(file);
        if (jar.getResource(destination) != null) {
            warn("Skipping duplicate resource: %s\n", destination);
            return;
        }
        jar.putResource(destination, resource);
        includedResources.add(sourceAbsPath);
        log("Adding resource: %s\n", destination);
    }

    private void log(String format, Object... objects) {
        if (debug) {
            System.out.printf(format, objects);
        }
    }

    private void warn(String format, Object... objects) {
        System.err.printf(format, objects);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("inputJar", inputJar)
                .add("outputJar", outputJar)
                .add("classpath", classpath)
                .add("bundleName", bundleName)
                .add("groupId", groupId)
                .add("bundleSymbolicName", bundleSymbolicName)
                .add("bundleVersion", bundleVersion)
                .add("bundleDescription", bundleDescription)
                .add("bundleLicense", bundleLicense)
                .add("bundleClassPath", bundleClasspath)
                .toString();

    }
}
