/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.cfgdef;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.expression.Add;
import com.thoughtworks.qdox.model.expression.AnnotationValue;
import com.thoughtworks.qdox.model.expression.AnnotationValueList;
import com.thoughtworks.qdox.model.expression.FieldRef;
import com.thoughtworks.qdox.parser.ParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Produces ONOS component configuration catalogue resources.
 */
public class CfgDefGenerator {

    private static final String COMPONENT = "org.osgi.service.component.annotations.Component";
    private static final String PROPERTY = "property";
    private static final String SEP = "|";
    private static final String UTF_8 = "UTF-8";
    private static final String JAVA = ".java";
    private static final String EXT = ".cfgdef";
    private static final String STRING = "STRING";
    private static final String NO_DESCRIPTION = "no description provided";

    private final File resourceJar;
    private final JavaProjectBuilder builder;

    private final Map<String, String> constants = Maps.newHashMap();
    private final Map<JavaClass, List<String>> pendingProperties = Maps.newHashMap();

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: cfgdef outputJar javaSource javaSource ...");
            System.exit(1);
        }
        CfgDefGenerator gen = new CfgDefGenerator(args[0], Arrays.copyOfRange(args, 1, args.length));
        gen.analyze();
        gen.generate();
    }

    private CfgDefGenerator(String resourceJarPath, String[] sourceFilePaths) {
        this.resourceJar = new File(resourceJarPath);
        this.builder = new JavaProjectBuilder();
        Arrays.stream(sourceFilePaths).forEach(filename -> {
            try {
                if (filename.endsWith(JAVA))
                builder.addSource(new File(filename));
            } catch (ParseException e) {
                // When unable to parse, skip the source; leave it to javac to fail.
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to open file", e);
            }
        });
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
        JarOutputStream jar = new JarOutputStream(new FileOutputStream(resourceJar));
        for (JavaClass javaClass : builder.getClasses()) {
            processClass(jar, javaClass);
        }
        jar.close();
    }

    private void processClass(JarOutputStream jar, JavaClass javaClass) throws IOException {
        Optional<JavaAnnotation> annotation = javaClass.getAnnotations().stream()
                .filter(ja -> ja.getType().getName().equals(COMPONENT))
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
                // FIXME this might not work if we have multiple inheritance stages
                for (JavaClass derivedClass : javaClass.getDerivedClasses()) {
                    // Temporary appends the properties - jar stream cannot be opened multiple times
                    pendingProperties.compute(derivedClass, (k, v) -> {
                        if (v == null) {
                            v = Lists.newArrayList();
                        }
                        v.addAll(lines);
                        return v;
                    });
                }
                // Get the attributes stored by the super class
                List<String> superProperties = pendingProperties.remove(javaClass);
                if (superProperties != null && !superProperties.isEmpty()) {
                    lines.addAll(superProperties);
                }
                writeCatalog(jar, javaClass, lines);
            } else {
                // Get the attributes stored by the super class
                List<String> superProperties = pendingProperties.remove(javaClass);
                if (superProperties != null && !superProperties.isEmpty()) {
                    writeCatalog(jar, javaClass, superProperties);
                }
            }
        }
    }

    private void processProperty(List<String> lines, JavaClass javaClass,
                                 AnnotationValue value) {
        String s = elaborate(value);
        String pex[] = s.split("=", 2);

        if (pex.length == 2) {
            String rex[] = pex[0].split(":", 2);
            String name = rex[0];
            String type = rex.length == 2 ? rex[1].toUpperCase() : STRING;
            String def = pex[1];
            String desc = description(javaClass, name);

            if (desc != null) {
                String line = name + SEP + type + SEP + def + SEP + desc + "\n";
                lines.add(line);
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
            String comment = field.getComment();
            return comment != null ? comment : NO_DESCRIPTION;
        }
        throw new IllegalStateException("cfgdef could not find a variable named " + name + " in "
                + javaClass.getName());
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

    private void writeCatalog(JarOutputStream jar, JavaClass javaClass, List<String> lines)
            throws IOException {
        String name = javaClass.getPackageName().replace('.', '/') + "/" + javaClass.getName() + EXT;
        jar.putNextEntry(new JarEntry(name));
        jar.write("# This file is auto-generated\n".getBytes(UTF_8));
        for (String line : lines) {
            jar.write(line.getBytes(UTF_8));
        }
        jar.closeEntry();
    }

}
