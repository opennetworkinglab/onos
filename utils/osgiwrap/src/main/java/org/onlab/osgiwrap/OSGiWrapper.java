/*
 * Copyright 2016 Open Networking Laboratory
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

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scrplugin.bnd.SCRDescriptorBndPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * BND-based wrapper to convert Buck JARs to OSGi-compatible JARs.
 */
public class OSGiWrapper {

    private String inputJar;
    private String outputJar;
    private List<String> classpath;

    private String bundleName;
    private String groupId;
    private String bundleSymbolicName;
    private String bundleVersion;

    private String bundleDescription;
    private String bundleLicense;

    private String webContext;

    public static void main(String[] args) {

        if (args.length < 7) {
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
        String webContext = args[7];
        String desc = Joiner.on(' ').join(Arrays.copyOfRange(args, 8, args.length));

        OSGiWrapper wrapper = new OSGiWrapper(jar, output, cp,
                                              name, group,
                                              version, license,
                                              webContext, desc);
        wrapper.log(wrapper + "\n");
        if (!wrapper.execute()) {
            System.err.println("ERROR");
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
                       String webContext,
                       String bundleDescription) {
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

        this.webContext = webContext;
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

        // There are no good defaults so make sure you set the Import-Package
        analyzer.setProperty(Analyzer.IMPORT_PACKAGE, "*,org.glassfish.jersey.servlet");

        // TODO include version in export, but not in import
        analyzer.setProperty(Analyzer.EXPORT_PACKAGE, "*");

        // TODO we may need INCLUDE_RESOURCE, or that might be done by Buck
        //analyzer.setProperty(analyzer.INCLUDE_RESOURCE, ...)
        if (isWab()) {
            analyzer.setProperty(Analyzer.WAB, "src/main/webapp/");
            analyzer.setProperty("Web-ContextPath", webContext);
        }
    }

    public boolean execute() {
        Analyzer analyzer = new Analyzer();
        try {

            Jar jar = new Jar(new File(inputJar));  // where our data is
            analyzer.setJar(jar);                   // give bnd the contents

            // You can provide additional class path entries to allow
            // bnd to pickup export version from the packageinfo file,
            // Version annotation, or their manifests.
            analyzer.addClasspath(classpath);

            setProperties(analyzer);

//            analyzer.setProperty("DESTDIR");
//            analyzer.setBase();

            // ------------- let's begin... -------------------------

            // Analyze the target JAR first
            analyzer.analyze();

            // Scan the JAR for Felix SCR annotations and generate XML files
            Map<String, String> properties = Maps.newHashMap();
            SCRDescriptorBndPlugin scrDescriptorBndPlugin = new SCRDescriptorBndPlugin();
            scrDescriptorBndPlugin.setProperties(properties);
            scrDescriptorBndPlugin.setReporter(analyzer);
            scrDescriptorBndPlugin.analyzeJar(analyzer);

            // Repack the JAR as a WAR
            doWabStaging(analyzer);

            // Calculate the manifest
            Manifest manifest = analyzer.calcManifest();
            //OutputStream s = new FileOutputStream("/tmp/foo2.txt");
            //manifest.write(s);
            //s.close();

            if (analyzer.isOk()) {
                analyzer.getJar().setManifest(manifest);
                analyzer.save(new File(outputJar), true);
                log("Saved!\n");
            } else {
                warn("%s\n", analyzer.getErrors());
                return false;
            }

            analyzer.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isWab() {
        return !Objects.equals(webContext, "NONE");
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

        Set<String> paths = new HashSet<String>(dot.getResources().keySet());

        for (String path : paths) {
            if (path.indexOf('/') > 0 && !Character.isUpperCase(path.charAt(0))) {
                log("wab: moving: %s", path);
                dot.rename(path, "WEB-INF/classes/" + path);
            }
        }
    }

    private void log(String format, Object... objects) {
        //System.err.printf(format, objects);
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
                .toString();

    }
}
