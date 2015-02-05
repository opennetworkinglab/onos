/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;

import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleOperations;

import java.util.List;

/**
 * Abstraction of entity capable of installing intents to the environment.
 */
public interface IntentInstaller<T extends Intent> {
    /**
     * Installs the specified intent to the environment.
     *
     * @param intent    intent to be installed
     * @return flow rule operations to complete install
     * @throws IntentException if issues are encountered while installing the intent
     */
    @Deprecated
    List<FlowRuleBatchOperation> install(T intent);
    // FIXME
    default FlowRuleOperations.Builder install2(T intent) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (FlowRuleBatchOperation batch : install(intent)) {
            for (FlowRuleBatchEntry entry : batch.getOperations()) {
                FlowRule rule = entry.target();
                switch (entry.operator()) {
                    case ADD:
                        builder.add(rule);
                        break;
                    case REMOVE:
                        builder.remove(rule);
                        break;
                    case MODIFY:
                        builder.modify(rule);
                        break;
                    default:
                        break;
                }
            }
            builder.newStage();
        }
        return builder;
    }

    /**
     * Uninstalls the specified intent from the environment.
     *
     * @param intent    intent to be uninstalled
     * @return flow rule operations to complete uninstall
     * @throws IntentException if issues are encountered while uninstalling the intent
     */
    @Deprecated
    List<FlowRuleBatchOperation> uninstall(T intent);
    // FIXME
    default FlowRuleOperations.Builder uninstall2(T intent) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (FlowRuleBatchOperation batch : uninstall(intent)) {
            for (FlowRuleBatchEntry entry : batch.getOperations()) {
                FlowRule rule = entry.target();
                switch (entry.operator()) {
                    case ADD:
                        builder.add(rule);
                        break;
                    case REMOVE:
                        builder.remove(rule);
                        break;
                    case MODIFY:
                        builder.modify(rule);
                        break;
                    default:
                        break;
                }
            }
            builder.newStage();
        }
        return builder;
    }

    /**
     * Replaces the specified intent with a new one in the environment.
     *
     * @param oldIntent intent to be removed
     * @param newIntent intent to be installed
     * @return flow rule operations to complete the replace
     * @throws IntentException if issues are encountered while uninstalling the intent
     */
    @Deprecated
    List<FlowRuleBatchOperation> replace(T oldIntent, T newIntent);
    // FIXME
    default FlowRuleOperations.Builder replace2(T oldIntent, T newIntent) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (FlowRuleBatchOperation batch : replace(oldIntent, newIntent)) {
            for (FlowRuleBatchEntry entry : batch.getOperations()) {
                FlowRule rule = entry.target();
                switch (entry.operator()) {
                    case ADD:
                        builder.add(rule);
                        break;
                    case REMOVE:
                        builder.remove(rule);
                        break;
                    case MODIFY:
                        builder.modify(rule);
                        break;
                    default:
                        break;
                }
            }
            builder.newStage();
        }
        return builder;
    }

}
