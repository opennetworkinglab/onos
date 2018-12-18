/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.cli;

import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.Action;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Annotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.security.AuditService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;
import java.util.TreeSet;

/**
 * Base abstraction of Karaf shell commands.
 */
public abstract class AbstractShellCommand implements Action, CodecContext {

    protected static final Logger log = getLogger(AbstractShellCommand.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Option(name = "-j", aliases = "--json", description = "Output JSON",
            required = false, multiValued = false)
    private boolean json = false;

    /**
     * Returns the reference to the implementation of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     * @throws org.onlab.osgi.ServiceNotFoundException if service is unavailable
     */
    public static <T> T get(Class<T> serviceClass) {
        return DefaultServiceDirectory.getService(serviceClass);
    }

    /**
     * Returns application ID for the CLI.
     *
     * @return command-line application identifier
     */
    protected ApplicationId appId() {
        return get(CoreService.class)
                .registerApplication("org.onosproject.cli");
    }

    /**
     * Prints the arguments using the specified format.
     *
     * @param format format string; see {@link String#format}
     * @param args   arguments
     */
    public void print(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

    /**
     * Prints the arguments using the specified format to error stream.
     *
     * @param format format string; see {@link String#format}
     * @param args   arguments
     */
    public void error(String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    /**
     * Produces a string image of the specified key/value annotations.
     *
     * @param annotations key/value annotations
     * @return string image with ", k1=v1, k2=v2, ..." pairs
     */
    public static String annotations(Annotations annotations) {
        if (annotations == null) {
            annotations = DefaultAnnotations.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        Set<String> keys = new TreeSet<>(annotations.keys());
        for (String key : keys) {
            sb.append(", ").append(key).append('=').append(annotations.value(key));
        }
        return sb.toString();
    }

    /**
     * Produces a string image of the specified key/value annotations.
     * Excludes the keys in the given Set.
     *
     * @param annotations  key/value annotations
     * @param excludedKeys keys not to add in the resulting string
     * @return string image with ", k1=v1, k2=v2, ..." pairs
     */
    public static String annotations(Annotations annotations, Set<String> excludedKeys) {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = new TreeSet<>(annotations.keys());
        keys.removeAll(excludedKeys);
        for (String key : keys) {
            sb.append(", ").append(key).append('=').append(annotations.value(key));
        }
        return sb.toString();
    }

    /**
     * Produces a JSON object from the specified key/value annotations.
     *
     * @param mapper      ObjectMapper to use while converting to JSON
     * @param annotations key/value annotations
     * @return JSON object
     */
    public static ObjectNode annotations(ObjectMapper mapper, Annotations annotations) {
        ObjectNode result = mapper.createObjectNode();
        for (String key : annotations.keys()) {
            result.put(key, annotations.value(key));
        }
        return result;
    }

    /**
     * Indicates whether JSON format should be output.
     *
     * @return true if JSON is requested
     */
    protected boolean outputJson() {
        return json;
    }

    @Override
    public final Object execute() throws Exception {
        try {
            auditCommand();
            doExecute();
        } catch (ServiceNotFoundException e) {
            error(e.getMessage());
        }
        return null;
    }

    // Handles auditing
    private void auditCommand() {
        AuditService auditService = get(AuditService.class);
        if (auditService != null && auditService.isAuditing()) {
            // FIXME: Compose and log audit message here; this is a hack
            String user = "foo"; // FIXME
            String action = "{\"command\" : \"" + Thread.currentThread().getName().substring(5) + "\"}";
            auditService.logUserAction(user, action);
        }
    }

    /**
     * Body of the shell command.
     *
     * @throws Exception thrown when problem is encountered
     */
    protected abstract void doExecute() throws Exception;

    @Override
    public ObjectMapper mapper() {
        return mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonCodec<T> codec(Class<T> entityClass) {
        return get(CodecService.class).getCodec(entityClass);
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return get(serviceClass);
    }

    /**
     * Generates a Json representation of an object.
     *
     * @param entity      object to generate JSON for
     * @param entityClass class to format with - this chooses which codec to use
     * @param <T>         Type of the object being formatted
     * @return JSON object representation
     */
    public <T> ObjectNode jsonForEntity(T entity, Class<T> entityClass) {
        return codec(entityClass).encode(entity, this);
    }
}
