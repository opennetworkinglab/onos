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

import com.google.common.base.MoreObjects;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Default implementation of workplace.
 */
public class DefaultWorkplace extends Workplace {

    /**
     * Name of workplace.
     */
    private String name;

    /**
     * Constructor of DefaultWorkplace.
     * @param name name of workplace
     * @param data data model tree
     */
    public DefaultWorkplace(String name, DataModelTree data) {
        super(data);
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String distributor() {
        return name();
    }

    @Override
    public Collection<WorkflowContext> getContexts() throws WorkflowException {
        WorkplaceStore workplaceStore;
        try {
            workplaceStore = DefaultServiceDirectory.getService(WorkplaceStore.class);
        } catch (ServiceNotFoundException e) {
            throw new WorkflowException(e);
        }

        return workplaceStore.getWorkplaceContexts(name());
    }

    /**
     * Returns collection of context names.
     * @return collection of context names
     */
    private Collection<String> getContextNames() {
        Collection<WorkflowContext> ctx;
        try {
            ctx = getContexts();
        } catch (WorkflowException e) {
            ctx = Collections.emptyList();
        }

        return ctx.stream().map(x -> x.name()).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name())
                .add("triggernext", triggerNext())
                .add("context", data())
                .add("contexts", getContextNames())
                .toString();
    }
}
