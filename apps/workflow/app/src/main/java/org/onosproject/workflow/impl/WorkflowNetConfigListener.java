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
package org.onosproject.workflow.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.workflow.api.DefaultWorkflowDescription;
import org.onosproject.workflow.api.RpcDescription;
import org.onosproject.workflow.api.WorkflowService;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.DefaultWorkplaceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

public class WorkflowNetConfigListener implements NetworkConfigListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNetConfigListener.class);

    public static final String CONFIG_KEY = "workflow";
    public static final String EXECUTOR_GROUPNAME = "onos/workflow-netcfg";
    public static final String EXECUTOR_PATTERN = "netcfg-event-handler";

    public static final String WORKPLACE_CREATE = "workplace.create";
    public static final String WORKPLACE_REMOVE = "workplace.remove";
    public static final String WORKFLOW_INVOKE = "workflow.invoke";
    public static final String WORKFLOW_TERMINATE = " workflow.terminate";

    private final ConfigFactory<ApplicationId, WorkflowNetConfig> configFactory =
            new ConfigFactory<ApplicationId, WorkflowNetConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, WorkflowNetConfig.class, CONFIG_KEY) {
                @Override
                public WorkflowNetConfig createConfig() {
                    return new WorkflowNetConfig();
                }
            };

    private final WorkflowService workflowService;

    private final ScheduledExecutorService executor =
            newSingleThreadScheduledExecutor(groupedThreads(EXECUTOR_GROUPNAME, EXECUTOR_PATTERN));

    public WorkflowNetConfigListener(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public ConfigFactory<ApplicationId, WorkflowNetConfig> getConfigFactory() {
        return configFactory;
    }

    @Override
    public boolean isRelevant(NetworkConfigEvent event) {
        return event.config().isPresent() && event.config().get() instanceof WorkflowNetConfig;
    }

    @Override
    public void event(NetworkConfigEvent event) {
        log.info("Configuration event: {}", event);
        switch (event.type()) {
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
                if (!event.config().isPresent()) {
                    log.error("No configuration found");
                    return;
                }
                WorkflowNetConfig config = (WorkflowNetConfig) event.config().get();

                //Single thread executor(locking is not required)
                executor.execute(new Handler(workflowService, config));
                break;
            default:
                break;
        }
    }

    public static class Handler implements Runnable {

        private WorkflowService workflowService;
        private WorkflowNetConfig config;

        public Handler(WorkflowService workflowService, WorkflowNetConfig config) {
            this.workflowService = workflowService;
            this.config = config;
        }

        @Override
        public void run() {

            try {
                Collection<RpcDescription> rpcs = config.getRpcDescriptions();
                log.info("" + rpcs);
                for (RpcDescription rpc : rpcs) {
                    if (!rpcMap.containsKey(rpc.op())) {
                        log.error("Invalid RPC: {}", rpc);
                        continue;
                    }

                    rpcMap.get(rpc.op()).apply(this.workflowService, rpc);
                }
            } catch (WorkflowException e) {
                log.error("Exception: ", e);
            }
        }
    }

    @FunctionalInterface
    public interface RpcCall {
        void apply(WorkflowService workflowService, RpcDescription rpcDesc) throws WorkflowException;
    }

    private static Map<String, RpcCall> rpcMap = new HashMap<>();
    static {
        rpcMap.put(WORKPLACE_CREATE,
                (service, desc) -> service.createWorkplace(DefaultWorkplaceDescription.valueOf(desc.params())));
        rpcMap.put(WORKPLACE_REMOVE,
                (service, desc) -> service.removeWorkplace(DefaultWorkplaceDescription.valueOf(desc.params())));
        rpcMap.put(WORKFLOW_INVOKE,
                (service, desc) -> service.invokeWorkflow(desc.params()));
        rpcMap.put(WORKFLOW_TERMINATE,
                (service, desc) -> service.terminateWorkflow(DefaultWorkflowDescription.valueOf(desc.params())));
    }
}
