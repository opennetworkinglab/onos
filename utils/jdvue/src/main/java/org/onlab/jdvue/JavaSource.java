package org.onlab.jdvue;

import java.util.*;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Simple abstraction of a Java source file for the purpose of tracking
 * dependencies and requirements.
 *
 * @author Thomas Vachuska
 */
public class JavaSource extends JavaEntity {

    private String path;
    private JavaPackage javaPackage;

    private final Set<String> importNames = new HashSet<>();
    private Set<JavaEntity> imports;

    /**
     * Creates a new Java source entity.
     *
     * @param name java source file name
     * @param path source file path
     */
    JavaSource(String name, String path) {
        super(name);
        this.path = path;
    }

    /**
     * Returns the Java package for this Java source.
     *
     * @return Java package
     */
    public JavaPackage getPackage() {
        return javaPackage;
    }

    /**
     * Sets the Java package for this Java source.
     *
     * @param javaPackage Java package
     */
    void setPackage(JavaPackage javaPackage) {
        if (this.javaPackage == null) {
            this.javaPackage = javaPackage;
        }
    }

    /**
     * Returns the set of resolved imports for this Java source
     *
     * @return set of imports
     */
    public Set<JavaEntity> getImports() {
        return imports;
    }

    /**
     * Sets the set of resolved imported Java entities for this source.
     *
     * @param imports set of resolved Java entities imported by this source
     */
    void setImports(Set<JavaEntity> imports) {
        if (this.imports == null) {
            this.imports = Collections.unmodifiableSet(new HashSet<>(imports));
        }
    }

    /**
     * Adds a name of an imported, but unresolved, Java entity name.
     *
     * @param name name of an imported Java entity
     */
    void addImportName(String name) {
        importNames.add(name);
    }

    /**
     * Returns the set of imported, but unresolved, Java entity names.
     *
     * @return set of imported Java entity names
     */
    Set<String> getImportNames() {
        return importNames;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name())
                .add("javaPackage", (javaPackage != null ? javaPackage.name() : ""))
                .add("importNames", importNames.size())
                .add("imports", (imports != null ? imports.size() : 0))
                .toString();
    }

}
