/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.gson.JsonParser;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Produces ONOS Swagger api-doc.
 */
@Mojo(name = "swagger", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class OnosSwaggerMojo extends AbstractMojo {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonParser jsonParser = new JsonParser();

    private static final String JSON_FILE = "swagger.json";
    private static final String GEN_SRC = "generated-sources";
    private static final String REG_SRC = "registrator.javat";

    private static final String PATH = "javax.ws.rs.Path";
    private static final String PATH_PARAM = "javax.ws.rs.PathParam";
    private static final String QUERY_PARAM = "javax.ws.rs.QueryParam";
    private static final String POST = "javax.ws.rs.POST";
    private static final String GET = "javax.ws.rs.GET";
    private static final String PUT = "javax.ws.rs.PUT";
    private static final String DELETE = "javax.ws.rs.DELETE";
    private static final String PRODUCES = "javax.ws.rs.Produces";
    private static final String CONSUMES = "javax.ws.rs.Consumes";
    private static final String JSON = "MediaType.APPLICATION_JSON";
    private static final String OCTET_STREAM = "MediaType.APPLICATION_OCTET_STREAM";

    /**
     * The directory where the generated catalogue file will be put.
     */
    @Parameter(defaultValue = "${basedir}")
    protected File srcDirectory;

    /**
     * The directory where the generated catalogue file will be put.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected File dstDirectory;

    /**
     * REST API web-context
     */
    @Parameter(defaultValue = "${web.context}")
    protected String webContext;

    /**
     * REST API version
     */
    @Parameter(defaultValue = "${api.version}")
    protected String apiVersion;

    /**
     * REST API description
     */
    @Parameter(defaultValue = "${api.description}")
    protected String apiDescription;

    /**
     * REST API title
     */
    @Parameter(defaultValue = "${api.title}")
    protected String apiTitle;

    /**
     * REST API title
     */
    @Parameter(defaultValue = "${api.package}")
    protected String apiPackage;

    /**
     * Maven project
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;


    @Override
    public void execute() throws MojoExecutionException {
        try {
            JavaProjectBuilder builder = new JavaProjectBuilder();
            builder.addSourceTree(new File(srcDirectory, "src/main/java"));

            ObjectNode root = initializeRoot();
            ArrayNode tags = mapper.createArrayNode();
            ObjectNode paths = mapper.createObjectNode();
            ObjectNode definitions = mapper.createObjectNode();

            root.set("tags", tags);
            root.set("paths", paths);
            root.set("definitions", definitions);

            builder.getClasses().forEach(jc -> processClass(jc, paths, tags, definitions));

            if (paths.size() > 0) {
                getLog().info("Generating ONOS REST API documentation...");
                genCatalog(root);

                if (!isNullOrEmpty(apiPackage)) {
                    genRegistrator();
                }
            }

            project.addCompileSourceRoot(new File(dstDirectory, GEN_SRC).getPath());

        } catch (Exception e) {
            getLog().warn("Unable to generate ONOS REST API documentation", e);
            throw e;
        }
    }

    // initializes top level root with Swagger required specifications
    private ObjectNode initializeRoot() {
        ObjectNode root = mapper.createObjectNode();
        root.put("swagger", "2.0");
        ObjectNode info = mapper.createObjectNode();
        root.set("info", info);

        root.put("basePath", webContext);
        info.put("version", apiVersion);
        info.put("title", apiTitle);
        info.put("description", apiDescription);

        ArrayNode produces = mapper.createArrayNode();
        produces.add("application/json");
        root.set("produces", produces);

        ArrayNode consumes = mapper.createArrayNode();
        consumes.add("application/json");
        root.set("consumes", consumes);

        return root;
    }

    // Checks whether javaClass has a path tag associated with it and if it does
    // processes its methods and creates a tag for the class on the root
    void processClass(JavaClass javaClass, ObjectNode paths, ArrayNode tags, ObjectNode definitions) {
        // If the class does not have a Path tag then ignore it
        JavaAnnotation annotation = getPathAnnotation(javaClass);
        if (annotation == null) {
            return;
        }

        String path = getPath(annotation);
        if (path == null) {
            return;
        }

        String resourcePath = "/" + path;
        String tagPath = path.isEmpty() ? "/" : path;

        // Create tag node for this class.
        ObjectNode tagObject = mapper.createObjectNode();
        tagObject.put("name", tagPath);
        if (javaClass.getComment() != null) {
            tagObject.put("description", shortText(javaClass.getComment()));
        }
        tags.add(tagObject);

        // Create an array node add to all methods from this class.
        ArrayNode tagArray = mapper.createArrayNode();
        tagArray.add(tagPath);

        processAllMethods(javaClass, resourcePath, paths, tagArray, definitions);
    }

    private JavaAnnotation getPathAnnotation(JavaClass javaClass) {
        Optional<JavaAnnotation> optional = javaClass.getAnnotations()
                .stream().filter(a -> a.getType().getName().equals(PATH)).findAny();
        return optional.orElse(null);
    }

    // Checks whether a class's methods are REST methods and then places all the
    // methods under a specific path into the paths node
    private void processAllMethods(JavaClass javaClass, String resourcePath,
                                   ObjectNode paths, ArrayNode tagArray, ObjectNode definitions) {
        // map of the path to its methods represented by an ObjectNode
        Map<String, ObjectNode> pathMap = new HashMap<>();

        javaClass.getMethods().forEach(javaMethod -> {
            javaMethod.getAnnotations().forEach(annotation -> {
                String name = annotation.getType().getName();
                if (name.equals(POST) || name.equals(GET) || name.equals(DELETE) || name.equals(PUT)) {
                    // substring(12) removes "javax.ws.rs."
                    String method = annotation.getType().toString().substring(12).toLowerCase();
                    processRestMethod(javaMethod, method, pathMap, resourcePath, tagArray, definitions);
                }
            });
        });

        // for each path add its methods to the path node
        for (Map.Entry<String, ObjectNode> entry : pathMap.entrySet()) {
            paths.set(entry.getKey(), entry.getValue());
        }


    }

    private void processRestMethod(JavaMethod javaMethod, String method,
                                   Map<String, ObjectNode> pathMap,
                                   String resourcePath, ArrayNode tagArray, ObjectNode definitions) {
        String fullPath = resourcePath, consumes = "", produces = "",
                comment = javaMethod.getComment();
        DocletTag tag = javaMethod.getTagByName("onos.rsModel");
        for (JavaAnnotation annotation : javaMethod.getAnnotations()) {
            String name = annotation.getType().getName();
            if (name.equals(PATH)) {
                fullPath = resourcePath + "/" + getPath(annotation);
                fullPath = fullPath.replaceFirst("^//", "/");
            }
            if (name.equals(CONSUMES)) {
                consumes = getIOType(annotation);
            }
            if (name.equals(PRODUCES)) {
                produces = getIOType(annotation);
            }
        }
        ObjectNode methodNode = mapper.createObjectNode();
        methodNode.set("tags", tagArray);

        addSummaryDescriptions(methodNode, comment);
        addJsonSchemaDefinition(definitions, tag);

        processParameters(javaMethod, methodNode, method, tag);

        processConsumesProduces(methodNode, "consumes", consumes);
        processConsumesProduces(methodNode, "produces", produces);
        if (tag == null || ((method.toLowerCase().equals("post") || method.toLowerCase().equals("put"))
                && !(tag.getParameters().size() > 1))) {
            addResponses(methodNode, tag, false);
        } else {
            addResponses(methodNode, tag, true);
        }

        ObjectNode operations = pathMap.get(fullPath);
        if (operations == null) {
            operations = mapper.createObjectNode();
            operations.set(method, methodNode);
            pathMap.put(fullPath, operations);
        } else {
            operations.set(method, methodNode);
        }
    }

    private void addJsonSchemaDefinition(ObjectNode definitions, DocletTag tag) {
        File definitionsDirectory = new File(srcDirectory + "/src/main/resources/definitions");
        if (tag != null) {
            tag.getParameters().forEach(param -> {
                try {
                    File config = new File(definitionsDirectory.getAbsolutePath() + "/"
                                                   + param + ".json");
                    definitions.putPOJO(param, jsonParser.parse(new FileReader(config)));
                } catch (IOException e) {
                    getLog().error(String.format("Could not process %s in %s@%s: %s",
                                  tag.getName(), tag.getContext(), tag.getLineNumber(),
                                  e.getMessage()));
                    throw Throwables.propagate(e);
                }
            });

        }
    }

    private void processConsumesProduces(ObjectNode methodNode, String type, String io) {
        if (!io.equals("")) {
            ArrayNode array = mapper.createArrayNode();
            methodNode.set(type, array);
            array.add(io);
        }
    }

    private void addSummaryDescriptions(ObjectNode methodNode, String comment) {
        String summary = "", description;
        if (comment != null) {
            if (comment.contains(".")) {
                int periodIndex = comment.indexOf(".");
                summary = comment.substring(0, periodIndex);
                description = comment.length() > periodIndex + 1 ?
                        comment.substring(periodIndex + 1).trim() : "";
            } else {
                description = comment;
            }
            methodNode.put("summary", summary);
            methodNode.put("description", description);
        }
    }

    // Temporary solution to add responses to a method
    private void addResponses(ObjectNode methodNode, DocletTag tag, boolean responseJson) {
        ObjectNode responses = mapper.createObjectNode();
        methodNode.set("responses", responses);

        ObjectNode success = mapper.createObjectNode();
        success.put("description", "successful operation");
        responses.set("200", success);
        if (tag != null && responseJson) {
            ObjectNode schema = mapper.createObjectNode();
            tag.getParameters().forEach(
                    param -> schema.put("$ref", "#/definitions/" + param));
            success.set("schema", schema);
        }

        ObjectNode defaultObj = mapper.createObjectNode();
        defaultObj.put("description", "Unexpected error");
        responses.set("default", defaultObj);
    }

    // Checks if the annotations has a value of JSON and returns the string
    // that Swagger requires
    private String getIOType(JavaAnnotation annotation) {
        if (annotation.getNamedParameter("value").toString().equals(JSON)) {
            return "application/json";
        } else if (annotation.getNamedParameter("value").toString().equals(OCTET_STREAM)) {
            return "application/octet_stream";
        }
        return "";
    }

    // If the annotation has a Path tag, returns the value with leading and
    // trailing double quotes and slash removed.
    private String getPath(JavaAnnotation annotation) {
        String path = annotation.getNamedParameter("value").toString();
        return path == null ? null : path.replaceAll("(^[\\\"/]*|[/\\\"]*$)", "");
    }

    // Processes parameters of javaMethod and enters the proper key-values into the methodNode
    private void processParameters(JavaMethod javaMethod, ObjectNode methodNode, String method, DocletTag tag) {
        ArrayNode parameters = mapper.createArrayNode();
        methodNode.set("parameters", parameters);
        boolean required = true;

        for (JavaParameter javaParameter : javaMethod.getParameters()) {
            ObjectNode individualParameterNode = mapper.createObjectNode();
            Optional<JavaAnnotation> optional = javaParameter.getAnnotations().stream().filter(
                    annotation -> annotation.getType().getName().equals(PATH_PARAM) ||
                            annotation.getType().getName().equals(QUERY_PARAM)).findAny();
            JavaAnnotation pathType = optional.orElse(null);

            String annotationName = javaParameter.getName();


            if (pathType != null) { //the parameter is a path or query parameter
                individualParameterNode.put("name",
                                            pathType.getNamedParameter("value")
                                                    .toString().replace("\"", ""));
                if (pathType.getType().getName().equals(PATH_PARAM)) {
                    individualParameterNode.put("in", "path");
                } else if (pathType.getType().getName().equals(QUERY_PARAM)) {
                    individualParameterNode.put("in", "query");
                }
                individualParameterNode.put("type", getType(javaParameter.getType()));
            } else { // the parameter is a body parameter
                individualParameterNode.put("name", annotationName);
                individualParameterNode.put("in", "body");

                // Adds the reference to the Json model for the input
                // that goes in the post or put operation
                if (tag != null && (method.toLowerCase().equals("post") ||
                        method.toLowerCase().equals("put"))) {
                    ObjectNode schema = mapper.createObjectNode();
                    tag.getParameters().forEach(param -> {
                        schema.put("$ref", "#/definitions/" + param);
                    });
                    individualParameterNode.set("schema", schema);
                }
            }
            for (DocletTag p : javaMethod.getTagsByName("param")) {
                if (p.getValue().contains(annotationName)) {
                    String description = "";
                    if (p.getValue().split(" ", 2).length >= 2) {
                        description = p.getValue().split(" ", 2)[1].trim();
                        if (description.contains("optional")) {
                            required = false;
                        }
                    } else {
                        getLog().warn(String.format(
                                    "No description for parameter \"%s\" in " +
                                    "method \"%s\" in %s (line %d)",
                                      p.getValue(), javaMethod.getName(),
                                      javaMethod.getDeclaringClass().getName(),
                                      javaMethod.getLineNumber()));
                    }
                    individualParameterNode.put("description", description);
                }
            }
            individualParameterNode.put("required", required);
            parameters.add(individualParameterNode);
        }
    }

    // Returns the Swagger specified strings for the type of a parameter
    private String getType(JavaType javaType) {
        String type = javaType.getFullyQualifiedName();
        String value;
        if (type.equals(String.class.getName())) {
            value = "string";
        } else if (type.equals("int")) {
            value = "integer";
        } else if (type.equals(boolean.class.getName())) {
            value = "boolean";
        } else if (type.equals(long.class.getName())) {
            value = "number";
        } else {
            value = "";
        }
        return value;
    }

    // Writes the swagger.json file using the supplied JSON root.
    private void genCatalog(ObjectNode root) {
        File swaggerCfg = new File(dstDirectory, JSON_FILE);
        if (dstDirectory.exists() || dstDirectory.mkdirs()) {
            try (FileWriter fw = new FileWriter(swaggerCfg);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(root.toString());
            } catch (IOException e) {
                getLog().warn("Unable to write " + JSON_FILE);
            }
        } else {
            getLog().warn("Unable to create " + dstDirectory);
        }
    }

    // Generates the registrator Java component.
    private void genRegistrator() {
        File dir = new File(dstDirectory, GEN_SRC);
        File reg = new File(dir, apiPackage.replaceAll("\\.", "/") + "/ApiDocRegistrator.java");
        File pkg = reg.getParentFile();
        if (pkg.exists() || pkg.mkdirs()) {
            try {
                String src = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(REG_SRC)));
                src = src.replace("${api.package}", apiPackage)
                        .replace("${web.context}", webContext)
                        .replace("${api.title}", apiTitle)
                        .replace("${api.description}", apiTitle);
                Files.write(src.getBytes(), reg);
            } catch (IOException e) {
                getLog().warn("Unable to write " + reg);
            }
        } else {
            getLog().warn("Unable to create " + reg);
        }
    }

    // Returns "nickname" based on method and path for a REST method
    private String setNickname(String method, String path) {
        if (!path.equals("")) {
            return (method + path.replace('/', '_').replace("{", "").replace("}", "")).toLowerCase();
        } else {
            return method.toLowerCase();
        }
    }

    private String shortText(String comment) {
        int i = comment.indexOf('.');
        return i > 0 ? comment.substring(0, i) : comment;
    }

}
