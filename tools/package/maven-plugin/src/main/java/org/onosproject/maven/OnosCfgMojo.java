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

import com.google.common.collect.Maps;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.expression.Add;
import com.thoughtworks.qdox.model.expression.AnnotationValue;
import com.thoughtworks.qdox.model.expression.AnnotationValueList;
import com.thoughtworks.qdox.model.expression.FieldRef;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;

/**
 * Produces ONOS component configuration catalogue resources.
 */
@Mojo(name = "cfg", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
@java.lang.SuppressWarnings("squid:S1148")
public class OnosCfgMojo extends AbstractMojo {

    /**
     * The directory where the generated catalogue file will be put.
     */
    @Parameter(defaultValue = "${basedir}")
    protected File srcDirectory;

    /**
     * The directory where the generated catalogue file will be put.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File dstDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating ONOS component configuration catalogues...");
        try {
            CfgDefGenerator gen = new CfgDefGenerator(dstDirectory, srcDirectory);
            gen.analyze();
            gen.generate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unable to generate property catalog", e);
        }
    }


    private class CfgDefGenerator {

        private static final String COMPONENT = "Component";
        private static final String PROPERTY = "property";
        private static final String SEP = "|";
        private static final String UTF_8 = "UTF-8";
        private static final String JAVA = ".java";
        private static final String EXT = ".cfgdef";
        private static final String STRING = "STRING";
        private static final String NO_DESCRIPTION = "no description provided";

        private final File dstDir;
        private final JavaProjectBuilder builder;

        private final Map<String, String> constants = Maps.newHashMap();


        private CfgDefGenerator(File dstDir, File srcDir) {
            this.dstDir = dstDir;
            this.builder = new JavaProjectBuilder();
            builder.addSourceTree(new File(srcDir, "src/main/java"));

//            Arrays.stream(sourceFilePaths).forEach(filename -> {
//                try {
//                    if (filename.endsWith(JAVA))
//                        builder.addSource(new File(filename));
//                } catch (ParseException e) {
//                    // When unable to parse, skip the source; leave it to javac to fail.
//                } catch (IOException e) {
//                    throw new IllegalArgumentException("Unable to open file", e);
//                }
//            });
        }

        public void analyze() {
            builder.getClasses().forEach(this::collectConstants);
        }

        private void collectConstants(JavaClass javaClass) {
            javaClass.getFields().stream()
                    .filter(f -> f.isStatic() && f.isFinal() && !f.isPrivate())
                    .forEach(f -> constants.put(f.getName(), f.getInitializationExpression()));
        }

        public void generate() throws IOException {
            for (JavaClass javaClass : builder.getClasses()) {
                processClass(javaClass);
            }
        }

        private void processClass(JavaClass javaClass) throws IOException {
            Optional<JavaAnnotation> annotation = javaClass.getAnnotations().stream()
                    .filter(ja -> ja.getType().getName().endsWith(COMPONENT))
                    .findFirst();
            if (annotation.isPresent()) {
                AnnotationValue property = annotation.get().getProperty(PROPERTY);
                List<String> lines = new ArrayList<>();
                if (property instanceof AnnotationValueList) {
                    AnnotationValueList list = (AnnotationValueList) property;
                    list.getValueList().forEach(v -> processProperty(lines, javaClass, v));
                } else {
                    processProperty(lines, javaClass, property);
                }

                if (!lines.isEmpty()) {
                    writeCatalog(javaClass, lines);
                }
            }
        }

        private void processProperty(List<String> lines, JavaClass javaClass,
                                     AnnotationValue value) {
            String s = elaborate(value);
            String[] pex = s.split("=", 2);

            if (pex.length == 2) {
                String[] rex = pex[0].split(":", 2);
                String name = rex[0];
                String type = rex.length == 2 ? rex[1].toUpperCase() : STRING;
                String def = pex[1];
                String desc = description(javaClass, name);

                if (desc != null) {
                    String cleanedDesc = desc.trim().replace("\n", " ").replace("  ", " ");
                    String line = name + SEP + type + SEP + def + SEP + cleanedDesc;
                    getLog().info("Processing property " + line + " ...");
                    lines.add(line + "\n");
                }
            }
        }

        // Retrieve description from a comment preceding the field named the same
        // as the property or
        // TODO: from an annotated comment.
        private String description(JavaClass javaClass, String name) {
            if (name.startsWith("_")) {
                // Static property - just leave it as is, not for inclusion in the cfg defs
                return null;
            }
            JavaField field = javaClass.getFieldByName(name);
            if (field != null) {
                // make sure that the new lines are removed from the comment, they will break the property loading.
                String comment = field.getComment();
                if (comment != null) {
                    comment = comment.replace("\n", " ").replace("\r", " ");
                }
                return comment != null ? comment : NO_DESCRIPTION;
            }
            throw new IllegalStateException("cfgdef could not find a variable named " + name + " in " + javaClass.getName());
        }

        private String elaborate(AnnotationValue value) {
            if (value instanceof Add) {
                return elaborate(((Add) value).getLeft()) + elaborate(((Add) value).getRight());
            } else if (value instanceof FieldRef) {
                return elaborate((FieldRef) value);
            } else if (value != null) {
                return stripped(value.toString());
            } else {
                return "";
            }
        }

        private String elaborate(FieldRef field) {
            String name = field.getName();
            String value = constants.get(name);
            if (value != null) {
                return stripped(value);
            }
            throw new IllegalStateException("Constant " + name + " cannot be elaborated;" +
                                                    " value not in the same compilation context");
        }

        private String stripped(String s) {
            return s.trim().replaceFirst("^[^\"]*\"", "").replaceFirst("\"$", "");
        }

        private void writeCatalog(JavaClass javaClass, List<String> lines) {
            File dir = new File(dstDir, javaClass.getPackageName().replace('.', '/'));
            dir.mkdirs();

            File cfgDef = new File(dir, javaClass.getName().replace('.', '/') + ".cfgdef");
            try (FileWriter fw = new FileWriter(cfgDef);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println("# This file is auto-generated by onos-maven-plugin");
                lines.forEach(pw::println);
            } catch (IOException e) {
                System.err.println("Unable to write catalog for " + javaClass.getName());
                e.printStackTrace();
            }
        }

    }
}