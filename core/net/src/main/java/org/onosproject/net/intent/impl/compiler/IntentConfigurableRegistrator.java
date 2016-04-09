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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Map;

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
            label = "Indicates whether to use flow objective-based compilers")
    private boolean useFlowObjectives = DEFAULT_FLOW_OBJECTIVES;

    private final Map<Class<Intent>, IntentCompiler<Intent>> flowRuleBased = Maps.newConcurrentMap();
    private final Map<Class<Intent>, IntentCompiler<Intent>> flowObjectiveBased = Maps.newConcurrentMap();

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
    }

    /**
     * Registers the specified compiler for the given intent class.
     *
     * @param cls       intent class
     * @param compiler  intent compiler
     * @param flowBased true if the compiler is flow based
     * @param <T>       the type of intent
     */
    @SuppressWarnings("unchecked")
    <T extends Intent> void registerCompiler(Class<T> cls, IntentCompiler<T> compiler,
                                             boolean flowBased) {
        if (flowBased) {
            flowObjectiveBased.put((Class<Intent>) cls, (IntentCompiler<Intent>) compiler);
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
     * @param cls       intent class
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
            flowObjectiveBased.forEach((cls, compiler) -> extensionService.registerCompiler(cls, compiler));
        } else {
            flowObjectiveBased.forEach((cls, compiler) -> extensionService.unregisterCompiler(cls));
            flowRuleBased.forEach((cls, compiler) -> extensionService.registerCompiler(cls, compiler));
        }
    }

}
