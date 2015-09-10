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
package org.onlab.jdvue;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Produces a package &amp; source catalogue.
 *
 * @author Thomas Vachuska
 */
public class Catalog {

    private static final String PACKAGE = "package";
    private static final String IMPORT = "import";
    private static final String STATIC = "static";
    private static final String SRC_ROOT = "src/main/java/";
    private static final String WILDCARD = "\\.*$";

    private final Map<String, JavaSource> sources = new HashMap<>();
    private final Map<String, JavaPackage> packages = new HashMap<>();
    private final Set<DependencyCycle> cycles = new HashSet<>();
    private final Set<Dependency> cycleSegments = new HashSet<>();
    private final Map<JavaPackage, Set<DependencyCycle>> packageCycles = new HashMap<>();
    private final Map<JavaPackage, Set<Dependency>> packageCycleSegments = new HashMap<>();

    /**
     * Loads the catalog from the specified catalog file.
     *
     * @param catalogPath catalog file path
     * @throws IOException if unable to read the catalog file
     */
    public void load(String catalogPath) throws IOException {
        InputStream is = new FileInputStream(catalogPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            // Split the line into the two fields: path and pragmas
            String fields[] = line.trim().split(":");
            if (fields.length <= 1) {
                continue;
            }
            String path = fields[0];

            // Now split the pragmas on whitespace and trim punctuation
            String pragma[] = fields[1].trim().replaceAll("[;\n\r]", "").split("[\t ]");

            // Locate (or create) Java source entity based on the path
            JavaSource source = getOrCreateSource(path);

            // Now process the package or import statements
            if (pragma[0].equals(PACKAGE)) {
                processPackageDeclaration(source, pragma[1]);

            } else if (pragma[0].equals(IMPORT)) {
                if (pragma[1].equals(STATIC)) {
                    processImportStatement(source, pragma[2]);
                } else {
                    processImportStatement(source, pragma[1]);
                }
            }
        }
    }

    /**
     * Analyzes the catalog by resolving imports and identifying circular
     * package dependencies.
     */
    public void analyze() {
        resolveImports();
        findCircularDependencies();
    }

    /**
     * Identifies circular package dependencies through what amounts to be a
     * depth-first search rooted with each package.
     */
    private void findCircularDependencies() {
        cycles.clear();
        for (JavaPackage javaPackage : getPackages()) {
            findCircularDependencies(javaPackage);
        }

        cycleSegments.clear();
        packageCycles.clear();
        packageCycleSegments.clear();

        for (DependencyCycle cycle : getCycles()) {
            recordCycleForPackages(cycle);
            cycleSegments.addAll(cycle.getCycleSegments());
        }
    }

    /**
     * Records the specified cycle into a set for each involved package.
     *
     * @param cycle cycle to record for involved packages
     */
    private void recordCycleForPackages(DependencyCycle cycle) {
        for (JavaPackage javaPackage : cycle.getCycle()) {
            Set<DependencyCycle> cset = packageCycles.get(javaPackage);
            if (cset == null) {
                cset = new HashSet<>();
                packageCycles.put(javaPackage, cset);
            }
            cset.add(cycle);

            Set<Dependency> sset = packageCycleSegments.get(javaPackage);
            if (sset == null) {
                sset = new HashSet<>();
                packageCycleSegments.put(javaPackage, sset);
            }
            sset.addAll(cycle.getCycleSegments());
        }
    }

    /**
     * Identifies circular dependencies in which this package participates
     * using depth-first search.
     *
     * @param javaPackage Java package to inspect for dependency cycles
     */
    private void findCircularDependencies(JavaPackage javaPackage) {
        // Setup a depth trace anchored at the given java package.
        List<JavaPackage> trace = newTrace(new ArrayList<JavaPackage>(), javaPackage);

        Set<JavaPackage> searched = new HashSet<>();
        searchDependencies(javaPackage, trace, searched);
    }

    /**
     * Generates a new trace using the previous one and a new element
     *
     * @param trace       old search trace
     * @param javaPackage package to add to the trace
     * @return new search trace
     */
    private List<JavaPackage> newTrace(List<JavaPackage> trace,
                                       JavaPackage javaPackage) {
        List<JavaPackage> newTrace = new ArrayList<>(trace);
        newTrace.add(javaPackage);
        return newTrace;
    }


    /**
     * Recursive depth-first search through dependency tree
     *
     * @param javaPackage java package being searched currently
     * @param trace       search trace
     * @param searched    set of java packages already searched
     */
    private void searchDependencies(JavaPackage javaPackage,
                                    List<JavaPackage> trace,
                                    Set<JavaPackage> searched) {
        if (!searched.contains(javaPackage)) {
            searched.add(javaPackage);
            for (JavaPackage dependency : javaPackage.getDependencies()) {
                if (trace.contains(dependency)) {
                    cycles.add(new DependencyCycle(trace, dependency));
                } else {
                    searchDependencies(dependency, newTrace(trace, dependency), searched);
                }
            }
        }
    }

    /**
     * Resolves import names of Java sources into imports of entities known
     * to this catalog. All other import names will be ignored.
     */
    private void resolveImports() {
        for (JavaPackage javaPackage : getPackages()) {
            Set<JavaPackage> dependencies = new HashSet<>();
            for (JavaSource source : javaPackage.getSources()) {
                Set<JavaEntity> imports = resolveImports(source);
                source.setImports(imports);
                dependencies.addAll(importedPackages(imports));
            }
            javaPackage.setDependencies(dependencies);
        }
    }

    /**
     * Produces a set of imported Java packages from the specified set of
     * Java source entities.
     *
     * @param imports list of imported Java source entities
     * @return list of imported Java packages
     */
    private Set<JavaPackage> importedPackages(Set<JavaEntity> imports) {
        Set<JavaPackage> packages = new HashSet<>();
        for (JavaEntity entity : imports) {
            packages.add(entity instanceof JavaPackage ? (JavaPackage) entity :
                                 ((JavaSource) entity).getPackage());
        }
        return packages;
    }

    /**
     * Resolves import names of the specified Java source into imports of
     * entities known to this catalog. All other import names will be ignored.
     *
     * @param source Java source
     * @return list of resolved imports
     */
    private Set<JavaEntity> resolveImports(JavaSource source) {
        Set<JavaEntity> imports = new HashSet<>();
        for (String importName : source.getImportNames()) {
            JavaEntity entity = importName.matches(WILDCARD) ?
                    getPackage(importName.replaceAll(WILDCARD, "")) :
                    getSource(importName);
            if (entity != null) {
                imports.add(entity);
            }
        }
        return imports;
    }

    /**
     * Returns either an existing or a newly created Java package.
     *
     * @param packageName Java package name
     * @return Java package
     */
    private JavaPackage getOrCreatePackage(String packageName) {
        JavaPackage javaPackage = packages.get(packageName);
        if (javaPackage == null) {
            javaPackage = new JavaPackage(packageName);
            packages.put(packageName, javaPackage);
        }
        return javaPackage;
    }

    /**
     * Returns either an existing or a newly created Java source.
     *
     * @param path Java source path
     * @return Java source
     */
    private JavaSource getOrCreateSource(String path) {
        String name = nameFromPath(path);
        JavaSource source = sources.get(name);
        if (source == null) {
            source = new JavaSource(name, path);
            sources.put(name, source);
        }
        return source;
    }

    /**
     * Extracts a fully qualified source class name from the given path.
     * <p/>
     * For now, this implementation assumes standard Maven source structure
     * and thus will look for start of package name under 'src/main/java/'.
     * If it will not find such a prefix, it will simply return the path as
     * the name.
     *
     * @param path source path
     * @return source name
     */
    private String nameFromPath(String path) {
        int i = path.indexOf(SRC_ROOT);
        String name = i < 0 ? path : path.substring(i + SRC_ROOT.length());
        return name.replaceAll("\\.java$", "").replace("/", ".");
    }

    /**
     * Processes the package declaration pragma for the given source.
     *
     * @param source      Java source
     * @param packageName Java package name
     */
    private void processPackageDeclaration(JavaSource source, String packageName) {
        JavaPackage javaPackage = getOrCreatePackage(packageName);
        source.setPackage(javaPackage);
        javaPackage.addSource(source);
    }

    /**
     * Processes the import pragma for the given source.
     *
     * @param source Java source
     * @param name   name of the Java entity being imported (class or package)
     */
    private void processImportStatement(JavaSource source, String name) {
        source.addImportName(name);
    }

    /**
     * Returns the collection of java sources.
     *
     * @return collection of java sources
     */
    public Collection<JavaSource> getSources() {
        return Collections.unmodifiableCollection(sources.values());
    }

    /**
     * Returns the Java source with the specified name.
     *
     * @param name Java source name
     * @return Java source
     */
    public JavaSource getSource(String name) {
        return sources.get(name);
    }

    /**
     * Returns the collection of all Java packages.
     *
     * @return collection of java packages
     */
    public Collection<JavaPackage> getPackages() {
        return Collections.unmodifiableCollection(packages.values());
    }

    /**
     * Returns the set of all Java package dependency cycles.
     *
     * @return set of dependency cycles
     */
    public Set<DependencyCycle> getCycles() {
        return Collections.unmodifiableSet(cycles);
    }

    /**
     * Returns the set of all Java package dependency cycle segments.
     *
     * @return set of dependency cycle segments
     */
    public Set<Dependency> getCycleSegments() {
        return Collections.unmodifiableSet(cycleSegments);
    }

    /**
     * Returns the set of dependency cycles which involve the specified package.
     *
     * @param javaPackage java package
     * @return set of dependency cycles
     */
    public Set<DependencyCycle> getPackageCycles(JavaPackage javaPackage) {
        Set<DependencyCycle> set = packageCycles.get(javaPackage);
        return Collections.unmodifiableSet(set == null ? new HashSet<DependencyCycle>() : set);
    }

    /**
     * Returns the set of dependency cycle segments which involve the specified package.
     *
     * @param javaPackage java package
     * @return set of dependency cycle segments
     */
    public Set<Dependency> getPackageCycleSegments(JavaPackage javaPackage) {
        Set<Dependency> set = packageCycleSegments.get(javaPackage);
        return Collections.unmodifiableSet(set == null ? new HashSet<Dependency>() : set);
    }

    /**
     * Returns the Java package with the specified name.
     *
     * @param name Java package name
     * @return Java package
     */
    public JavaPackage getPackage(String name) {
        return packages.get(name);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("packages", packages.size())
                .add("sources", sources.size())
                .add("cycles", cycles.size())
                .add("cycleSegments", cycleSegments.size()).toString();
    }

}
