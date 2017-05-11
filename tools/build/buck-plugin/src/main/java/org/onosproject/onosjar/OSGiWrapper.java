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

package org.onosproject.onosjar;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.StepExecutionResult;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scrplugin.bnd.SCRDescriptorBndPlugin;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static java.nio.file.Files.walkFileTree;

/**
 * BND-based wrapper to convert Buck JARs to OSGi-compatible JARs.
 */
public class OSGiWrapper implements Step {

    private Path inputJar;
    private Path outputJar;
    private Path sourcesDir;
    private Path classesDir;
    private List<String> classpath;

    private String bundleName;
    private String groupId;
    private String bundleSymbolicName;
    private String bundleVersion;

    private String importPackages;
    private String privatePackages;
    private String dynamicimportPackages;
    private String embeddedDependencies;

    private String exportPackages;
    private String includeResources;
    private Set<String> includedResources = Sets.newHashSet();

    private String bundleDescription;
    private String bundleLicense;

    private String webContext;

    private PrintStream stderr = System.err;

    public OSGiWrapper(Path inputJar,
                       Path outputJar,
                       Path sourcesDir,
                       Path classesDir,
                       ImmutableSortedSet<Path> classpath,
                       String bundleName,
                       String groupId,
                       String bundleVersion,
                       String bundleLicense,
                       String importPackages,
                       String exportPackages,
                       String includeResources,
                       String webContext,
                       String dynamicimportPackages,
                       String embeddedDependencies,
                       String bundleDescription,
                       String privatePackages) {
        this.inputJar = inputJar;
        this.sourcesDir = sourcesDir;
        this.classesDir = classesDir;
        this.classpath = Lists.newArrayList(
                classpath.stream().map(Path::toString).collect(Collectors.toList()));
        if (!this.classpath.contains(inputJar.toString())) {
            this.classpath.add(0, inputJar.toString());
        }
        this.outputJar = outputJar;

        this.bundleName = bundleName;
        this.groupId = groupId;
        this.bundleSymbolicName = String.format("%s.%s", groupId, bundleName);

        this.bundleVersion = bundleVersion;
        this.bundleLicense = bundleLicense;
        this.bundleDescription = bundleDescription;

        this.importPackages = importPackages;
        this.privatePackages = privatePackages;
        this.dynamicimportPackages = dynamicimportPackages;
        this.embeddedDependencies = embeddedDependencies;
        this.exportPackages = exportPackages;
        this.includeResources = includeResources;

        this.webContext = webContext;
    }

    private void setProperties(Analyzer analyzer) {
        analyzer.setProperty(Analyzer.BUNDLE_NAME, bundleName);
        analyzer.setProperty(Analyzer.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
        analyzer.setProperty(Analyzer.BUNDLE_VERSION, bundleVersion.replace('-', '.'));

        if (bundleDescription != null) {
            analyzer.setProperty(Analyzer.BUNDLE_DESCRIPTION, bundleDescription);
        }
        if (bundleLicense != null) {
            analyzer.setProperty(Analyzer.BUNDLE_LICENSE, bundleLicense);
        }

        //TODO consider using stricter version policy
        //analyzer.setProperty("-provider-policy", "${range;[===,==+)}");
        //analyzer.setProperty("-consumer-policy", "${range;[===,==+)}");

        // There are no good defaults so make sure you set the Import-Package
        analyzer.setProperty(Analyzer.IMPORT_PACKAGE, importPackages);
        if (privatePackages != null) {
            analyzer.setProperty(Analyzer.PRIVATE_PACKAGE, privatePackages);
        }
        analyzer.setProperty(Analyzer.REMOVEHEADERS, "Private-Package,Include-Resource");

        analyzer.setProperty(Analyzer.DYNAMICIMPORT_PACKAGE,
                             dynamicimportPackages);

        // TODO include version in export, but not in import
        analyzer.setProperty(Analyzer.EXPORT_PACKAGE, exportPackages);

        // TODO we may need INCLUDE_RESOURCE, or that might be done by Buck
        // FIXME NOTE we handle this manually below
        if (includeResources != null) {
            analyzer.setProperty(Analyzer.INCLUDE_RESOURCE, includeResources);
        }

        if(embeddedDependencies != null) {
            analyzer.setProperty(Analyzer.BUNDLE_CLASSPATH,
                                 embeddedDependencies);
            String finalIncludes = Strings.isNullOrEmpty(includeResources) ?
                    embeddedDependencies : (includeResources+","+embeddedDependencies);
            analyzer.setProperty(Analyzer.INCLUDE_RESOURCE,
                                 finalIncludes);
        }

        if (isWab()) {
            analyzer.setProperty(Analyzer.WAB, "src/main/webapp/");
            analyzer.setProperty("Web-ContextPath", webContext);
            analyzer.setProperty(Analyzer.IMPORT_PACKAGE, "*,org.glassfish.jersey.servlet,org.jvnet.mimepull\n");
        }
    }

    public boolean execute() {
        Builder analyzer = new Builder();
        try {

            Jar jar = new Jar(inputJar.toFile());   // where our data is
            analyzer.setJar(jar);                   // give bnd the contents

            // You can provide additional class path entries to allow
            // bnd to pickup export version from the packageinfo file,
            // Version annotation, or their manifests.
            analyzer.addClasspath(classpath);

            setProperties(analyzer);

            // Analyze the target JAR first
            analyzer.analyze();

            // Scan the JAR for Felix SCR annotations and generate XML files
            Map<String, String> properties = Maps.newHashMap();
            properties.put("destdir", classesDir.toAbsolutePath().toString());
            SCRDescriptorBndPlugin scrDescriptorBndPlugin = new SCRDescriptorBndPlugin();
            scrDescriptorBndPlugin.setProperties(properties);
            scrDescriptorBndPlugin.setReporter(analyzer);
            scrDescriptorBndPlugin.analyzeJar(analyzer);

            //Add local packges to jar file.
            //FIXME removing this call for now; not sure what exactly it's doing
            //addLocalPackages(new File(classesDir.toString()), analyzer);

            //add resources.
            if (includeResources != null || embeddedDependencies != null) {
                doIncludeResources(analyzer);
            }

            // Repack the JAR as a WAR
            doWabStaging(analyzer);

            // Calculate the manifest
            Manifest manifest = analyzer.calcManifest();

            //Build the jar files
            //FIXME this call conflicts with some of the above
//            analyzer.build();

            if (analyzer.isOk()) {
                //add calculated manifest file.
                analyzer.getJar().setManifest(manifest);
                if (analyzer.save(outputJar.toFile(), true)) {
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

    private static void addLocalPackages(File outputDirectory, Analyzer analyzer) throws IOException {
        Packages packages = new Packages();

        if (outputDirectory != null && outputDirectory.isDirectory()) {
            // scan classes directory for potential packages
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(outputDirectory);
            scanner.setIncludes(new String[]
                                        {"**/*.class"});

            scanner.addDefaultExcludes();
            scanner.scan();

            String[] paths = scanner.getIncludedFiles();
            for (int i = 0; i < paths.length; i++) {
                packages.put(analyzer.getPackageRef(getPackageName(paths[i])));
            }
        }

        Packages exportedPkgs = new Packages();
        Packages privatePkgs = new Packages();

        boolean noprivatePackages = "!*".equals(analyzer.getProperty(Analyzer.PRIVATE_PACKAGE));

        for (Descriptors.PackageRef pkg : packages.keySet()) {
            // mark all source packages as private by default (can be overridden by export list)
            privatePkgs.put(pkg);

            // we can't export the default package (".") and we shouldn't export internal packages
            String fqn = pkg.getFQN();
            if (noprivatePackages || !(".".equals(fqn) || fqn.contains(".internal") || fqn.contains(".impl"))) {
                exportedPkgs.put(pkg);
            }
        }

        Properties properties = analyzer.getProperties();
        String exported = properties.getProperty(Analyzer.EXPORT_PACKAGE);
        if (exported == null) {
            if (!properties.containsKey(Analyzer.EXPORT_CONTENTS)) {
                // no -exportcontents overriding the exports, so use our computed list
                for (Attrs attrs : exportedPkgs.values()) {
                    attrs.put(Constants.SPLIT_PACKAGE_DIRECTIVE, "merge-first");
                }
                properties.setProperty(Analyzer.EXPORT_PACKAGE, Processor.printClauses(exportedPkgs));
            } else {
                // leave Export-Package empty (but non-null) as we have -exportcontents
                properties.setProperty(Analyzer.EXPORT_PACKAGE, "");
            }
        }

        String internal = properties.getProperty(Analyzer.PRIVATE_PACKAGE);
        if (internal == null) {
            if (!privatePkgs.isEmpty()) {
                for (Attrs attrs : privatePkgs.values()) {
                    attrs.put(Constants.SPLIT_PACKAGE_DIRECTIVE, "merge-first");
                }
                properties.setProperty(Analyzer.PRIVATE_PACKAGE, Processor.printClauses(privatePkgs));
            } else {
                // if there are really no private packages then use "!*" as this will keep the Bnd Tool happy
                properties.setProperty(Analyzer.PRIVATE_PACKAGE, "!*");
            }
        }
    }

    private static String getPackageName(String filename) {
        int n = filename.lastIndexOf(File.separatorChar);
        return n < 0 ? "." : filename.substring(0, n).replace(File.separatorChar, '.');
    }

    private boolean isWab() {
        return webContext != null;
    }

    private void doWabStaging(Analyzer analyzer) throws Exception {
        if (!isWab()) {
            return;
        }
        String wab = analyzer.getProperty(analyzer.WAB);
        Jar dot = analyzer.getJar();

        log("wab %s", wab);
        analyzer.setBundleClasspath("WEB-INF/classes," +
                                            analyzer.getProperty(analyzer.BUNDLE_CLASSPATH));

        Set<String> paths = new HashSet<>(dot.getResources().keySet());

        for (String path : paths) {
            if (path.indexOf('/') > 0 && !Character.isUpperCase(path.charAt(0))) {
                log("wab: moving: %s", path);
                dot.rename(path, "WEB-INF/classes/" + path);
            }
        }

        Path wabRoot = Paths.get(wab);
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

        Path classesBasedPath = classesDir.resolve(sourceRoot);
        Path sourceBasedPath = sourcesDir.resolve(sourceRoot);

        File classFile = classesBasedPath.toFile();
        File sourceFile = sourceBasedPath.toFile();

        if (classFile.isFile()) {
            addFileToJar(jar, destinationRoot, classesBasedPath.toAbsolutePath().toString());
        } else if (sourceFile.isFile()) {
            addFileToJar(jar, destinationRoot, sourceBasedPath.toAbsolutePath().toString());
        } else if (classFile.isDirectory()) {
            includeDirectory(jar, destinationRoot, classesBasedPath);
        } else if (sourceFile.isDirectory()) {
            includeDirectory(jar, destinationRoot, sourceBasedPath);
        } else {
            warn("Skipping resource in bundle %s: %s (File Not Found)\n",
                 bundleSymbolicName, sourceRoot);
        }
    }

    private void includeDirectory(Jar jar, String destinationRoot, Path sourceRoot)
            throws IOException {
        // iterate through sources
        // put each source on the jar
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceRoot.relativize(file);
                String destination = destinationRoot != null ?
                        destinationRoot + "/" + relativePath.toString() : //TODO
                        relativePath.toString();

                addFileToJar(jar, destination, file.toAbsolutePath().toString());
                return FileVisitResult.CONTINUE;
            }
        };

        walkFileTree(sourceRoot, visitor);
    }

    private boolean addFileToJar(Jar jar, String destination, String sourceAbsPath) {
        if (includedResources.contains(sourceAbsPath)) {
            log("Skipping already included resource: %s\n", sourceAbsPath);
            return false;
        }
        File file = new File(sourceAbsPath);
        if (!file.isFile()) {
            throw new RuntimeException(
                    String.format("Skipping non-existent file: %s\n", sourceAbsPath));
        }
        Resource resource = new FileResource(file);
        if (jar.getResource(destination) != null) {
            warn("Skipping duplicate resource: %s\n", destination);
            return false;
        }
        jar.putResource(destination, resource);
        includedResources.add(sourceAbsPath);
        log("Adding resource: %s\n", destination);
        return true;
    }

    private void log(String format, Object... objects) {
        //System.err.printf(format, objects);
    }

    private void warn(String format, Object... objects) {
        stderr.printf(format, objects);
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
                .toString();
    }

    @Override
    public StepExecutionResult execute(ExecutionContext executionContext)
            throws IOException, InterruptedException {
        stderr = executionContext.getStdErr();
        boolean success = execute();
        stderr = System.err;
        return success ? StepExecutionResult.SUCCESS : StepExecutionResult.ERROR;
    }

    @Override
    public String getShortName() {
        return "osgiwrap";
    }

    @Override
    public String getDescription(ExecutionContext executionContext) {
        return "osgiwrap"; //FIXME
    }
}
