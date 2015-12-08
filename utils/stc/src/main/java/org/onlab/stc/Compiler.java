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
package org.onlab.stc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.graph.DepthFirstSearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Integer.parseInt;
import static org.onlab.graph.DepthFirstSearch.EdgeType.BACK_EDGE;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;
import static org.onlab.stc.Scenario.loadScenario;

/**
 * Entity responsible for loading a scenario and producing a redy-to-execute
 * process flow graph.
 */
public class Compiler {

    private static final String DEFAULT_LOG_DIR = "${WORKSPACE}/tmp/stc/";

    private static final String IMPORT = "import";
    private static final String GROUP = "group";
    private static final String STEP = "step";
    private static final String PARALLEL = "parallel";
    private static final String SEQUENTIAL = "sequential";
    private static final String DEPENDENCY = "dependency";

    private static final String LOG_DIR = "[@logDir]";
    private static final String NAME = "[@name]";
    private static final String COMMAND = "[@exec]";
    private static final String ENV = "[@env]";
    private static final String CWD = "[@cwd]";
    private static final String DELAY = "[@delay]";
    private static final String REQUIRES = "[@requires]";
    private static final String IF = "[@if]";
    private static final String UNLESS = "[@unless]";
    private static final String VAR = "[@var]";
    private static final String STARTS = "[@starts]";
    private static final String ENDS = "[@ends]";
    private static final String FILE = "[@file]";
    private static final String NAMESPACE = "[@namespace]";

    static final String PROP_START = "${";
    static final String PROP_END = "}";

    private static final String HASH = "#";
    private static final String HASH_PREV = "#-1";

    private final Scenario scenario;

    private final Map<String, Step> steps = Maps.newHashMap();
    private final Map<String, Step> inactiveSteps = Maps.newHashMap();
    private final Map<String, String> requirements = Maps.newHashMap();
    private final Set<Dependency> dependencies = Sets.newHashSet();
    private final List<Integer> clonables = Lists.newArrayList();

    private ProcessFlow processFlow;
    private File logDir;

    private String previous = null;
    private String pfx = "";
    private boolean debugOn = System.getenv("debug") != null;

    /**
     * Creates a new compiler for the specified scenario.
     *
     * @param scenario scenario to be compiled
     */
    public Compiler(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * Returns the scenario being compiled.
     *
     * @return test scenario
     */
    public Scenario scenario() {
        return scenario;
    }

    /**
     * Compiles the specified scenario to produce a final process flow graph.
     */
    public void compile() {
        compile(scenario.definition(), null, null);
        compileRequirements();

        // Produce the process flow
        processFlow = new ProcessFlow(ImmutableSet.copyOf(steps.values()),
                                      ImmutableSet.copyOf(dependencies));

        scanForCycles();

        // Extract the log directory if there was one specified
        String defaultPath = DEFAULT_LOG_DIR + scenario.name();
        String path = scenario.definition().getString(LOG_DIR, defaultPath);
        logDir = new File(expand(path));
    }

    /**
     * Returns the step with the specified name.
     *
     * @param name step or group name
     * @return test step or group
     */
    public Step getStep(String name) {
        return steps.get(name);
    }

    /**
     * Returns the process flow generated from this scenario definition.
     *
     * @return process flow as a graph
     */
    public ProcessFlow processFlow() {
        return processFlow;
    }

    /**
     * Returns the log directory where scenario logs should be kept.
     *
     * @return scenario logs directory
     */
    public File logDir() {
        return logDir;
    }

    /**
     * Recursively elaborates this definition to produce a final process flow graph.
     *
     * @param cfg         hierarchical definition
     * @param namespace   optional namespace
     * @param parentGroup optional parent group
     */
    private void compile(HierarchicalConfiguration cfg,
                         String namespace, Group parentGroup) {
        String opfx = pfx;
        pfx = pfx + ">";
        print("pfx=%s namespace=%s", pfx, namespace);

        // Scan all imports
        cfg.configurationsAt(IMPORT)
                .forEach(c -> processImport(c, namespace, parentGroup));

        // Scan all steps
        cfg.configurationsAt(STEP)
                .forEach(c -> processStep(c, namespace, parentGroup));

        // Scan all groups
        cfg.configurationsAt(GROUP)
                .forEach(c -> processGroup(c, namespace, parentGroup));

        // Scan all parallel groups
        cfg.configurationsAt(PARALLEL)
                .forEach(c -> processParallelGroup(c, namespace, parentGroup));

        // Scan all sequential groups
        cfg.configurationsAt(SEQUENTIAL)
                .forEach(c -> processSequentialGroup(c, namespace, parentGroup));

        // Scan all dependencies
        cfg.configurationsAt(DEPENDENCY)
                .forEach(c -> processDependency(c, namespace));

        pfx = opfx;
    }

    /**
     * Compiles requirements for all steps and groups accrued during the
     * overall compilation process.
     */
    private void compileRequirements() {
        requirements.forEach((name, requires) ->
                                     compileRequirements(getStep(name), requires));
    }

    private void compileRequirements(Step src, String requires) {
        split(requires).forEach(n -> {
            boolean isSoft = n.startsWith("~");
            String name = n.replaceFirst("^~", "");
            Step dst = getStep(name);
            if (dst != null) {
                dependencies.add(new Dependency(src, dst, isSoft));
            }
        });
    }

    /**
     * Processes an import directive.
     *
     * @param cfg         hierarchical definition
     * @param namespace   optional namespace
     * @param parentGroup optional parent group
     */
    private void processImport(HierarchicalConfiguration cfg,
                               String namespace, Group parentGroup) {
        String file = checkNotNull(expand(cfg.getString(FILE)),
                                   "Import directive must specify 'file'");
        String newNamespace = expand(prefix(cfg.getString(NAMESPACE), namespace));
        print("import file=%s namespace=%s", file, newNamespace);
        try {
            Scenario importScenario = loadScenario(new FileInputStream(file));
            compile(importScenario.definition(), newNamespace, parentGroup);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to import scenario", e);
        }
    }

    /**
     * Processes a step directive.
     *
     * @param cfg         hierarchical definition
     * @param namespace   optional namespace
     * @param parentGroup optional parent group
     */
    private void processStep(HierarchicalConfiguration cfg,
                             String namespace, Group parentGroup) {
        String name = expand(prefix(cfg.getString(NAME), namespace));
        String command = expand(cfg.getString(COMMAND, parentGroup != null ? parentGroup.command() : null), true);
        String env = expand(cfg.getString(ENV, parentGroup != null ? parentGroup.env() : null));
        String cwd = expand(cfg.getString(CWD, parentGroup != null ? parentGroup.cwd() : null));
        int delay = parseInt(expand(cfg.getString(DELAY, parentGroup != null ? "" + parentGroup.delay() : "0")));

        print("step name=%s command=%s env=%s cwd=%s delay=%d", name, command, env, cwd, delay);
        Step step = new Step(name, command, env, cwd, parentGroup, delay);
        registerStep(step, cfg, namespace, parentGroup);
    }

    /**
     * Processes a group directive.
     *
     * @param cfg         hierarchical definition
     * @param namespace   optional namespace
     * @param parentGroup optional parent group
     */
    private void processGroup(HierarchicalConfiguration cfg,
                              String namespace, Group parentGroup) {
        String name = expand(prefix(cfg.getString(NAME), namespace));
        String command = expand(cfg.getString(COMMAND, parentGroup != null ? parentGroup.command() : null), true);
        String env = expand(cfg.getString(ENV, parentGroup != null ? parentGroup.env() : null));
        String cwd = expand(cfg.getString(CWD, parentGroup != null ? parentGroup.cwd() : null));
        int delay = parseInt(expand(cfg.getString(DELAY, parentGroup != null ? "" + parentGroup.delay() : "0")));

        print("group name=%s command=%s env=%s cwd=%s delay=%d", name, command, env, cwd, delay);
        Group group = new Group(name, command, env, cwd, parentGroup, delay);
        if (registerStep(group, cfg, namespace, parentGroup)) {
            compile(cfg, namespace, group);
        }
    }

    /**
     * Registers the specified step or group.
     *
     * @param step        step or group
     * @param cfg         hierarchical definition
     * @param namespace   optional namespace
     * @param parentGroup optional parent group
     * @return true of the step or group was registered as active
     */
    private boolean registerStep(Step step, HierarchicalConfiguration cfg,
                                 String namespace, Group parentGroup) {
        checkState(!steps.containsKey(step.name()), "Step %s already exists", step.name());
        String ifClause = expand(cfg.getString(IF));
        String unlessClause = expand(cfg.getString(UNLESS));

        if ((ifClause != null && ifClause.length() == 0) ||
                (unlessClause != null && unlessClause.length() > 0) ||
                (parentGroup != null && inactiveSteps.containsValue(parentGroup))) {
            inactiveSteps.put(step.name(), step);
            return false;
        }

        if (parentGroup != null) {
            parentGroup.addChild(step);
        }

        steps.put(step.name(), step);
        processRequirements(step, expand(cfg.getString(REQUIRES)), namespace);
        previous = step.name();
        return true;
    }

    /**
     * Processes a parallel clone group directive.
     *
     * @param cfg         hierarchical definition
     * @param namespace   optional namespace
     * @param parentGroup optional parent group
     */
    private void processParallelGroup(HierarchicalConfiguration cfg,
                                      String namespace, Group parentGroup) {
        String var = cfg.getString(VAR);
        print("parallel var=%s", var);

        int i = 1;
        while (condition(var, i).length() > 0) {
            clonables.add(0, i);
            compile(cfg, namespace, parentGroup);
            clonables.remove(0);
            i++;
        }
    }

    /**
     * Processes a sequential clone group directive.
     *
     * @param cfg         hierarchical definition
     * @param namespace   optional namespace
     * @param parentGroup optional parent group
     */
    private void processSequentialGroup(HierarchicalConfiguration cfg,
                                        String namespace, Group parentGroup) {
        String var = cfg.getString(VAR);
        String starts = cfg.getString(STARTS);
        String ends = cfg.getString(ENDS);
        print("sequential var=%s", var);

        int i = 1;
        while (condition(var, i).length() > 0) {
            clonables.add(0, i);
            compile(cfg, namespace, parentGroup);
            if (i > 1) {
                processSequentialRequirements(starts, ends, namespace);
            }
            clonables.remove(0);
            i++;
        }
    }

    /**
     * Hooks starts of this sequence tier to the previous tier.
     *
     * @param starts    comma-separated list of start steps
     * @param ends      comma-separated list of end steps
     * @param namespace optional namespace
     */
    private void processSequentialRequirements(String starts, String ends,
                                               String namespace) {
        for (String s : split(starts)) {
            String start = expand(prefix(s, namespace));
            String reqs = requirements.get(s);
            for (String n : split(ends)) {
                boolean isSoft = n.startsWith("~");
                String name = n.replaceFirst("^~", "");
                name = (isSoft ? "~" : "") + expand(prefix(name, namespace));
                reqs = reqs == null ? name : (reqs + "," + name);
            }
            requirements.put(start, reqs);
        }
    }

    /**
     * Returns the elaborated repetition construct conditional.
     *
     * @param var repetition var property
     * @param i   index to elaborate
     * @return elaborated string
     */
    private String condition(String var, Integer i) {
        return expand(var.replaceFirst("#", i.toString())).trim();
    }

    /**
     * Processes a dependency directive.
     *
     * @param cfg       hierarchical definition
     * @param namespace optional namespace
     */
    private void processDependency(HierarchicalConfiguration cfg, String namespace) {
        String name = expand(prefix(cfg.getString(NAME), namespace));
        String requires = expand(cfg.getString(REQUIRES));

        print("dependency name=%s requires=%s", name, requires);
        Step step = getStep(name, namespace);
        if (!inactiveSteps.containsValue(step)) {
            processRequirements(step, requires, namespace);
        }
    }

    /**
     * Processes the specified requiremenst string and adds dependency for
     * each requirement of the given step.
     *
     * @param src       source step
     * @param requires  comma-separated list of required steps
     * @param namespace optional namespace
     */
    private void processRequirements(Step src, String requires, String namespace) {
        String reqs = requirements.get(src.name());
        for (String n : split(requires)) {
            boolean isSoft = n.startsWith("~");
            String name = n.replaceFirst("^~", "");
            name = previous != null && name.equals("^") ? previous : name;
            name = (isSoft ? "~" : "") + expand(prefix(name, namespace));
            reqs = reqs == null ? name : (reqs + "," + name);
        }
        requirements.put(src.name(), reqs);
    }

    /**
     * Retrieves the step or group with the specified name.
     *
     * @param name      step or group name
     * @param namespace optional namespace
     * @return step or group; null if none found in active or inactive steps
     */
    private Step getStep(String name, String namespace) {
        String dName = prefix(name, namespace);
        Step step = steps.get(dName);
        step = step != null ? step : inactiveSteps.get(dName);
        checkArgument(step != null, "Unknown step %s", dName);
        return step;
    }

    /**
     * Prefixes the specified name with the given namespace.
     *
     * @param name      name of a step or a group
     * @param namespace optional namespace
     * @return composite name
     */
    private String prefix(String name, String namespace) {
        return isNullOrEmpty(namespace) ? name : namespace + "." + name;
    }

    /**
     * Expands any environment variables in the specified string. These are
     * specified as ${property} tokens.
     *
     * @param string     string to be processed
     * @param keepTokens true if the original unresolved tokens should be kept
     * @return original string with expanded substitutions
     */
    private String expand(String string, boolean... keepTokens) {
        if (string == null) {
            return null;
        }

        String pString = string;
        StringBuilder sb = new StringBuilder();
        int start, end, last = 0;
        while ((start = pString.indexOf(PROP_START, last)) >= 0) {
            end = pString.indexOf(PROP_END, start + PROP_START.length());
            checkArgument(end > start, "Malformed property in %s", pString);
            sb.append(pString.substring(last, start));
            String prop = pString.substring(start + PROP_START.length(), end);
            String value;
            if (prop.equals(HASH)) {
                value = Integer.toString(clonables.get(0));
            } else if (prop.equals(HASH_PREV)) {
                value = Integer.toString(clonables.get(0) - 1);
            } else if (prop.endsWith(HASH)) {
                pString = pString.replaceFirst("#}", clonables.get(0) + "}");
                last = start;
                continue;
            } else {
                // Try system property first, then fall back to env. variable.
                value = System.getProperty(prop);
                if (value == null) {
                    value = System.getenv(prop);
                }
            }
            if (value == null && keepTokens.length == 1 && keepTokens[0]) {
                sb.append("${").append(prop).append("}");
            } else {
                sb.append(value != null ? value : "");
            }
            last = end + 1;
        }
        sb.append(pString.substring(last));
        return sb.toString().replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * Splits the comma-separated string into a list of strings.
     *
     * @param string string to split
     * @return list of strings
     */
    private List<String> split(String string) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        String[] fields = string != null ? string.split(",") : new String[0];
        for (String field : fields) {
            builder.add(field.trim());
        }
        return builder.build();
    }

    /**
     * Scans the process flow graph for cyclic dependencies.
     */
    private void scanForCycles() {
        DepthFirstSearch<Step, Dependency> dfs = new DepthFirstSearch<>();
        // Use a brute-force method of searching paths from all vertices.
        processFlow().getVertexes().forEach(s -> {
            DepthFirstSearch<Step, Dependency>.SpanningTreeResult r =
                    dfs.search(processFlow, s, null, null, ALL_PATHS);
            r.edges().forEach((e, et) -> checkArgument(et != BACK_EDGE,
                                                       "Process flow has a cycle involving dependency from %s to %s",
                                                       e.src().name, e.dst().name));
        });
    }


    /**
     * Prints formatted output.
     *
     * @param format printf format string
     * @param args   arguments to be printed
     */
    private void print(String format, Object... args) {
        if (debugOn) {
            System.err.println(pfx + String.format(format, args));
        }
    }

}
