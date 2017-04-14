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

package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.resource.impl.LabelAllocator;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Auxiliary utility to register either flow-rule compilers or flow-objective
 * compilers.
 */
@Component
@Service(value = IntentConfigurableRegistrator.class)
public class IntentConfigurableRegistrator {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService extensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private static final boolean DEFAULT_FLOW_OBJECTIVES = false;
    @Property(name = "useFlowObjectives",
            boolValue = DEFAULT_FLOW_OBJECTIVES,
            label = "Indicates whether or not to use flow objective-based compilers")
    private boolean useFlowObjectives = DEFAULT_FLOW_OBJECTIVES;

    private static final String DEFAULT_LABEL_SELECTION = "RANDOM";
    @Property(name = "labelSelection",
            value = DEFAULT_LABEL_SELECTION,
            label = "Defines the label selection algorithm - RANDOM or FIRST_FIT")
    private String labelSelection = DEFAULT_LABEL_SELECTION;

    private static final boolean DEFAULT_FLOW_OPTIMIZATION = false;
    @Property(name = "optimizeInstructions",
            boolValue = DEFAULT_FLOW_OPTIMIZATION,
            label = "Indicates whether or not to optimize the flows in the link collection compiler")
    private boolean optimizeInstructions = DEFAULT_FLOW_OPTIMIZATION;

    private static final boolean DEFAULT_COPY_TTL = false;
    @Property(name = "useCopyTtl",
            boolValue = DEFAULT_COPY_TTL,
            label = "Indicates whether or not to use copy ttl in the link collection compiler")
    private boolean useCopyTtl = DEFAULT_COPY_TTL;

    /**
     * Temporary for switching old compiler and new compiler.
     * @deprecated 1.10 Kingfisher
     */
    private static final String DEFAULT_FLOW_OBJECTIVE_COMPILER =
            "org.onosproject.net.intent.impl.compiler.LinkCollectionIntentFlowObjectiveCompiler";
    @Deprecated
    @Property(name = "defaultFlowObjectiveCompiler",
            value = DEFAULT_FLOW_OBJECTIVE_COMPILER,
            label = "Default compiler to generate flow objective")
    private String defaultFlowObjectiveCompiler = DEFAULT_FLOW_OBJECTIVE_COMPILER;

    private final Map<Class<Intent>, IntentCompiler<Intent>> flowRuleBased = Maps.newConcurrentMap();

    // FIXME: temporary code for switching old compiler to new compiler
    private final Map<Class<Intent>, Set<IntentCompiler<Intent>>> flowObjectiveBased = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            log.info("Settings: useFlowObjectives={}", useFlowObjectives);
            log.info("Settings: labelSelection={}", labelSelection);
            log.info("Settings: useFlowOptimization={}", optimizeInstructions);
            log.info("Settings: useCopyTtl={}", useCopyTtl);

            // FIXME: temporary code for switching old compiler to new compiler
            log.info("Settings: defaultFlowObjectiveCompiler={}", defaultFlowObjectiveCompiler);
            return;
        }

        boolean newFlowObjectives;
        try {
            String s = Tools.get(context.getProperties(), "useFlowObjectives");
            newFlowObjectives = isNullOrEmpty(s) ? useFlowObjectives : Boolean.parseBoolean(s.trim());
        } catch (ClassCastException e) {
            newFlowObjectives = useFlowObjectives;
        }

        if (useFlowObjectives != newFlowObjectives) {
            useFlowObjectives = newFlowObjectives;
            changeCompilers();
            log.info("Settings: useFlowObjectives={}", useFlowObjectives);
        }

        // FIXME: temporary code for switching old compiler to new compiler
        String newDefaultFlowObjectiveCompiler;
        try {
            String s = Tools.get(context.getProperties(), "defaultFlowObjectiveCompiler");
            newDefaultFlowObjectiveCompiler = isNullOrEmpty(s) ? defaultFlowObjectiveCompiler : s.trim();
        } catch (ClassCastException e) {
            newDefaultFlowObjectiveCompiler = defaultFlowObjectiveCompiler;
        }

        if (!defaultFlowObjectiveCompiler.equals(newDefaultFlowObjectiveCompiler)) {
            defaultFlowObjectiveCompiler = newDefaultFlowObjectiveCompiler;
            changeCompilers();
            log.info("Settings: defaultFlowObjectiveCompiler={}", defaultFlowObjectiveCompiler);
        }

        String newLabelSelection;
        try {
            String s = Tools.get(context.getProperties(), "labelSelection");
            newLabelSelection = isNullOrEmpty(s) ? labelSelection : s.trim();
        } catch (ClassCastException e) {
            newLabelSelection = labelSelection;
        }

        if (!labelSelection.equals(newLabelSelection) && LabelAllocator.isInEnum(newLabelSelection)) {
            labelSelection = newLabelSelection;
            changeLabelSelections();
            log.info("Settings: labelSelection={}", labelSelection);
        }

        boolean newFlowOptimization;
        try {
            String s = Tools.get(context.getProperties(), "useFlowOptimization");
            newFlowOptimization = isNullOrEmpty(s) ? optimizeInstructions : Boolean.parseBoolean(s.trim());
        } catch (ClassCastException e) {
            newFlowOptimization = optimizeInstructions;
        }

        if (optimizeInstructions != newFlowOptimization) {
            optimizeInstructions = newFlowOptimization;
            changeFlowOptimization();
            log.info("Settings: useFlowOptimization={}", optimizeInstructions);
        }

        boolean newCopyTtl;
        try {
            String s = Tools.get(context.getProperties(), "useCopyTtl");
            newCopyTtl = isNullOrEmpty(s) ? useCopyTtl : Boolean.parseBoolean(s.trim());
        } catch (ClassCastException e) {
            newCopyTtl = useCopyTtl;
        }

        if (useCopyTtl != newCopyTtl) {
            useCopyTtl = newCopyTtl;
            changeCopyTtl();
            log.info("Settings: useCopyTtl={}", useCopyTtl);
        }
    }

    /**
     * Registers the specified compiler for the given intent class.
     *
     * @param cls       the intent class
     * @param compiler  the intent compiler
     * @param flowBased true if the compiler is flow based
     * @param <T>       the type of intent
     */
    @SuppressWarnings("unchecked")
    <T extends Intent> void registerCompiler(Class<T> cls, IntentCompiler<T> compiler,
                                             boolean flowBased) {
        if (flowBased) {
            // FIXME: temporary code for switching old compiler to new compiler
            flowObjectiveBased.compute((Class<Intent>) cls, (clz, compilers) -> {
                if (compilers == null) {
                    compilers = Sets.newHashSet();
                }

                compilers.add((IntentCompiler<Intent>) compiler);
                return compilers;
            });
        } else {
            flowRuleBased.put((Class<Intent>) cls, (IntentCompiler<Intent>) compiler);
        }
        if (flowBased == useFlowObjectives) {
            extensionService.registerCompiler(cls, compiler);
        }
    }

    /**
     * Unregisters the compiler for the specified intent class.
     *
     * @param cls       the intent class
     * @param flowBased true if the compiler is flow based
     * @param <T>       the type of intent
     */
    @SuppressWarnings("unchecked")
    <T extends Intent> void unregisterCompiler(Class<T> cls, boolean flowBased) {
        if (flowBased) {
            flowObjectiveBased.remove(cls);
        } else {
            flowRuleBased.remove(cls);
        }
        if (flowBased == useFlowObjectives) {
            extensionService.unregisterCompiler(cls);
        }
    }

    private void changeCompilers() {
        if (useFlowObjectives) {
            flowRuleBased.forEach((cls, compiler) -> extensionService.unregisterCompiler(cls));
            // FIXME: temporary code for switching old compiler to new compiler
            flowObjectiveBased.forEach((cls, compilers) -> {
                compilers.forEach(compiler -> {
                    // filter out flow objective compiler which doesn't match
                    if (compiler.getClass().getName().equals(defaultFlowObjectiveCompiler)) {
                        extensionService.registerCompiler(cls, compiler);
                    }
                });
            });
        } else {
            flowObjectiveBased.forEach((cls, compiler) -> extensionService.unregisterCompiler(cls));
            flowRuleBased.forEach((cls, compiler) -> extensionService.registerCompiler(cls, compiler));
        }
    }

    private void changeLabelSelections() {
        LinkCollectionCompiler.labelAllocator.setLabelSelection(labelSelection);
    }

    private void changeFlowOptimization() {
        LinkCollectionCompiler.optimizeInstructions = optimizeInstructions;
    }

    private void changeCopyTtl() {
        LinkCollectionCompiler.copyTtl = useCopyTtl;
    }

}
