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
package org.onosproject.workflow.api;

import org.onosproject.event.Event;
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Interface for workflow execution service.
 */
public interface WorkflowExecutionService extends ListenerService<WorkflowDataEvent, WorkflowDataListener> {

    /**
     * Executes init worklet.
     * @param context workflow context
     */
    void execInitWorklet(WorkflowContext context);

    /**
     * Evals workflow context.
     * @param contextName the name of workflow context
     */
    void eval(String contextName);

    /**
     * Triggers workflow event map.
     * @param event triggering event
     * @param generator event hint generation method reference
     */
    void eventMapTrigger(Event event, EventHintSupplier generator);

    /**
     * Registers workflow event map.
     * @param eventType event type (class name of event)
     * @param eventHintSet Set of event hint value
     * @param contextName workflow context name to be called by this event map
     * @param programCounterString worklet type to be called by this event map
     * @throws WorkflowException workflow exception
     */
    void registerEventMap(Class<? extends Event> eventType, Set<String> eventHintSet,
                          String contextName, String programCounterString) throws WorkflowException;
}
