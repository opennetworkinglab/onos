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

import com.google.common.collect.Maps;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.onlab.stc.Coordinator.Status;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.stc.Coordinator.Status.FAILED;
import static org.onlab.stc.Coordinator.Status.WAITING;
import static org.onlab.stc.Coordinator.print;

/**
 * Maintains state of scenario execution.
 */
class ScenarioStore {

    private final ProcessFlow processFlow;
    private final File storeFile;

    private final Map<Step, Status> stepStatus = Maps.newConcurrentMap();

    /**
     * Creates a new scenario store for the specified process flow.
     *
     * @param processFlow scenario process flow
     * @param logDir      scenario log directory
     * @param name        scenario name
     */
    ScenarioStore(ProcessFlow processFlow, File logDir, String name) {
        this.processFlow = processFlow;
        this.storeFile = new File(logDir, name + ".stc");
        processFlow.getVertexes().forEach(step -> stepStatus.put(step, WAITING));
    }


    /**
     * Returns set of all test steps.
     *
     * @return set of steps
     */
    Set<Step> getSteps() {
        return processFlow.getVertexes();
    }

    /**
     * Returns the status of the specified test step.
     *
     * @param step test step or group
     * @return step status
     */
    Status getStatus(Step step) {
        return checkNotNull(stepStatus.get(step), "Step %s not found", step.name());
    }

    /**
     * Updates the status of the specified test step.
     *
     * @param step   test step or group
     * @param status new step status
     */
    void updateStatus(Step step, Status status) {
        stepStatus.put(step, status);
        save();
    }

    /**
     * Indicates whether there are any failures.
     *
     * @return true if there are failed steps
     */
    boolean hasFailures() {
        for (Status status : stepStatus.values()) {
            if (status == FAILED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads the states from disk.
     */
    private void load() {
        // FIXME: implement this
    }

    /**
     * Saves the states to disk.
     */
    private void save() {
        try {
            PropertiesConfiguration cfg = new PropertiesConfiguration(storeFile);
            stepStatus.forEach((step, status) -> cfg.setProperty(step.name(), status));
            cfg.save();
        } catch (ConfigurationException e) {
            print("Unable to store file %s", storeFile);
        }
    }

}
