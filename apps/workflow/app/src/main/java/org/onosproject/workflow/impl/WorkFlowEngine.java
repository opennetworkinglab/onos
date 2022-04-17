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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.service.StorageException;
import org.onosproject.workflow.api.DefaultWorkplace;
import org.onosproject.workflow.api.EventHintSupplier;
import org.onosproject.workflow.api.EventTask;
import org.onosproject.workflow.api.JsonDataModelInjector;
import org.onosproject.workflow.api.JsonDataModelTree;
import org.onosproject.workflow.api.ProgramCounter;
import org.onosproject.workflow.api.SystemWorkflowContext;
import org.onosproject.workflow.api.EventTimeoutTask;
import org.onosproject.workflow.api.TimeoutTask;
import org.onosproject.workflow.api.TimerChain;
import org.onosproject.workflow.api.Worklet;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.WorkflowData;
import org.onosproject.workflow.api.ContextEventMapStore;
import org.onosproject.workflow.api.WorkflowState;
import org.onosproject.workflow.api.WorkflowStore;
import org.onosproject.workflow.api.WorkflowBatchDelegate;
import org.onosproject.workflow.api.WorkflowDataEvent;
import org.onosproject.workflow.api.WorkflowDataListener;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.HandlerTask;
import org.onosproject.workflow.api.HandlerTaskBatchDelegate;
import org.onosproject.workflow.api.Workplace;
import org.onosproject.workflow.api.WorkplaceStore;
import org.onosproject.workflow.api.WorkplaceStoreDelegate;
import org.onosproject.workflow.api.WorkflowExecutionService;
import org.onosproject.workflow.api.WorkletDescription;
import org.onosproject.workflow.api.StaticDataModelInjector;
import org.onosproject.workflow.api.WorkflowLoggerInjector;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.Event;
import org.onosproject.net.intent.WorkPartitionService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.workflow.api.CheckCondition.check;
import static org.onosproject.workflow.api.WorkflowAttribute.REMOVE_AFTER_COMPLETE;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = WorkflowExecutionService.class)
public class WorkFlowEngine extends AbstractListenerManager<WorkflowDataEvent, WorkflowDataListener>
        implements WorkflowExecutionService {

    protected static final Logger log = getLogger(WorkFlowEngine.class);
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected WorkPartitionService partitionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected WorkplaceStore workplaceStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected WorkflowStore workflowStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ContextEventMapStore eventMapStore;

    private final WorkplaceStoreDelegate workplaceStoreDelegate = this::post;

    private final WorkflowBatchDelegate workflowBatchDelegate = new InternalWorkflowBatchDelegate();
    private final WorkflowAccumulator workflowAccumulator = new WorkflowAccumulator(workflowBatchDelegate);

    private final HandlerTaskBatchDelegate eventtaskBatchDelegate = new InternalHandlerTaskBatchDelegate();
    private final HandlerTaskAccumulator eventtaskAccumulator = new HandlerTaskAccumulator(eventtaskBatchDelegate);

    private ExecutorService workflowBatchExecutor;
    private ExecutorService workflowExecutor;

    private ExecutorService handlerTaskBatchExecutor;
    private ExecutorService handlerTaskExecutor;

    private static final int DEFAULT_WORKFLOW_THREADS = 12;
    private static final int DEFAULT_EVENTTASK_THREADS = 12;
    private static final int MAX_REGISTER_EVENTMAP_WAITS = 10;
    private static final String ERROR = "ERROR";

    private ScheduledExecutorService eventMapTriggerExecutor;

    private TimerChain timerChain = new TimerChain();

    private JsonDataModelInjector dataModelInjector = new JsonDataModelInjector();
    private StaticDataModelInjector staticDataModelInjector = new StaticDataModelInjector();
    private WorkflowLoggerInjector workflowLoggerInjector = new WorkflowLoggerInjector();

    public static final String APPID = "org.onosproject.workflow";
    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APPID);
        workplaceStore.setDelegate(workplaceStoreDelegate);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        workflowBatchExecutor = newSingleThreadExecutor(
                groupedThreads("onos/workflow", "workflow-batch", log));
        workflowExecutor = newFixedThreadPool(DEFAULT_WORKFLOW_THREADS,
                groupedThreads("onos/workflow-exec", "worker-%d", log));
        handlerTaskBatchExecutor = newSingleThreadExecutor(
                groupedThreads("onos/workflow", "handlertask-batch", log));
        handlerTaskExecutor = newFixedThreadPool(DEFAULT_EVENTTASK_THREADS,
                groupedThreads("onos/handlertask-exec", "worker-%d", log));
        eventMapTriggerExecutor = newSingleThreadScheduledExecutor(
                groupedThreads("onos/workflow-engine", "eventmap-trigger-executor"));

        (new WorkplaceWorkflow(this, workflowStore)).registerWorkflows();
        JsonDataModelTree data = new JsonDataModelTree(JsonNodeFactory.instance.objectNode());
        workplaceStore.registerWorkplace(Workplace.SYSTEM_WORKPLACE,
                new DefaultWorkplace(Workplace.SYSTEM_WORKPLACE, data));

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        leadershipService.withdraw(appId.name());
        workplaceStore.unsetDelegate(workplaceStoreDelegate);
        workflowBatchExecutor.shutdown();
        workflowExecutor.shutdown();
        handlerTaskBatchExecutor.shutdown();
        handlerTaskExecutor.shutdown();
        eventMapTriggerExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void execInitWorklet(WorkflowContext context) {

        Workflow workflow = workflowStore.get(context.workflowId());
        if (workflow == null) {
            log.error("Invalid workflow id:{}", context.workflowId());
            return;
        }

        initWorkletExecution(context);
        try {
            Worklet initWorklet = workflow.init(context);
            if (initWorklet != null) {

                log.info("{} worklet.process:{}", context.name(), initWorklet.tag());
                log.trace("{} context: {}", context.name(), context);

                workflowLoggerInjector.inject(initWorklet, context);
                dataModelInjector.inject(initWorklet, context);
                initWorklet.process(context);
                dataModelInjector.inhale(initWorklet, context);

                log.info("{} worklet.process(done): {}", context.name(), initWorklet.tag());
                log.trace("{} context: {}", context.name(), context);
            }

            check(workplaceStore.getContext(context.name()) == null,
                    "Duplicated workflow context(" + context.name() + ") assignment.");

        } catch (WorkflowException e) {
            log.error("Exception: ", e);
            context.setCause(e.getMessage());
            context.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(context.name(), context, false);
            return;
        }
        // trigger the execution of next worklet.
        workplaceStore.registerContext(context.name(), context);
    }

    @Override
    public void eval(String contextName) {

        final WorkflowContext latestContext = workplaceStore.getContext(contextName);
        if (latestContext == null) {
            log.error("Invalid workflow context {}", contextName);
            return;
        }

        initWorkletExecution(latestContext);

        workplaceStore.commitContext(latestContext.name(), latestContext, true);

    }

    @Override
    public void eventMapTrigger(Event event, EventHintSupplier supplier) {

        if (event.subject() instanceof SystemWorkflowContext) {
            return;
        }

        Map<String, String> eventMap;

        String eventHint;
        try {
            eventHint = supplier.apply(event);
        } catch (Throwable e) {
            log.error("Exception: ", e);
            return;
        }
        if (eventHint == null) {
            // do nothing
            log.error("Invalid eventHint, event: {}", event);
            return;
        }

        try {
            eventMap = eventMapStore.getEventMapByHint(event.getClass().getName(), eventHint);
        } catch (WorkflowException e) {
            log.error("Exception: ", e);
            return;
        }

        if (Objects.isNull(eventMap) || eventMap.isEmpty()) {
            // do nothing;
            log.debug("Invalid eventMap, event: {}", event);
            return;
        }

        for (Map.Entry<String, String> entry : eventMap.entrySet()) {
            String contextName = entry.getKey();
            String strProgramCounter = entry.getValue();
            ProgramCounter pc;
            try {
                pc = ProgramCounter.valueOf(strProgramCounter);
            } catch (IllegalArgumentException e) {
                log.error("Exception: ", e);
                return;
            }

            WorkflowContext context = workplaceStore.getContext(contextName);
            if (Objects.isNull(context)) {
                log.info("Invalid context: {}, event: {}", contextName, event);
                continue;
            }
            EventTask eventtask = null;
            try {
                eventtask = EventTask.builder()
                        .event(event)
                        .eventHint(eventHint)
                        .context(context)
                        .programCounter(pc)
                        .build();
            } catch (WorkflowException e) {
                log.error("Exception: ", e);
            }

            log.debug("eventtaskAccumulator.add: task: {}", eventtask);
            if (!Objects.isNull(eventtask)) {
                eventtaskAccumulator.add(eventtask);
            }
        }
    }

    @Override
    public void registerEventMap(Class<? extends Event> eventType, Set<String> eventHintSet,
                                 String contextName, String programCounterString) throws WorkflowException {
        eventMapStore.registerEventMap(eventType.getName(), eventHintSet, contextName, programCounterString);
        for (String eventHint : eventHintSet) {
            for (int i = 0; i < MAX_REGISTER_EVENTMAP_WAITS; i++) {
                Map<String, String> eventMap = eventMapStore.getEventMapByHint(eventType.getName(), eventHint);
                if (eventMap != null && eventMap.containsKey(contextName)) {
                    break;
                }
                try {
                    log.info("sleep {}", i);
                    Thread.sleep(10L * (i + 1));
                } catch (InterruptedException e) {
                    log.error("Exception: ", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    @Override
    protected void post(WorkflowDataEvent event) {

        if (event.subject() == null || !isRelevant(event.subject())) {
            log.debug("ignore event {}", event);
            return;
        }

        // trigger next worklet selection
        WorkflowData dataModelContainer = event.subject();
        switch (event.type()) {
            case INSERT:
            case UPDATE:
                if (dataModelContainer.triggerNext()) {
                    log.debug("workflowAccumulator.add: {}", dataModelContainer);
                    workflowAccumulator.add(dataModelContainer);
                } else {
                    log.debug("pass-workflowAccumulator.add: {}", dataModelContainer);
                }
                break;
            case REMOVE:
                break;
            default:
        }

        // trigger EventTask for WorkflowDataEvent
        eventMapTriggerExecutor.submit(
                () -> eventMapTrigger(
                        event,
                        // event hint supplier
                        (ev) -> {
                            if (ev == null || ev.subject() == null) {
                                return null;
                            }

                            if (ev.subject() instanceof WorkflowData) {
                                return ((WorkflowData) ev.subject()).name();
                            } else {
                                return null;
                            }
                        }
                )
        );
    }

    /**
     * Checks whether this workflow data job is relevant to this ONOS node.
     *
     * @param job workflow data
     * @return checking result
     */
    private boolean isRelevant(WorkflowData job) {
        // distributes event processing with work-partition
        return partitionService.isMine(job.distributor(), this::stringHash);
    }

    /**
     * Gets hash of the string.
     *
     * @param str string to get a hash
     * @return hash value
     */
    public Long stringHash(String str) {
        return UUID.nameUUIDFromBytes(str.getBytes()).getMostSignificantBits();
    }

    /**
     * Class for handler task batch delegation.
     */
    private class InternalHandlerTaskBatchDelegate implements HandlerTaskBatchDelegate {
        @Override
        public void execute(Collection<Collection<HandlerTask>> operations) {
            log.debug("Execute {} operation(s).", operations.size());

            CompletableFuture.runAsync(() -> {
                List<CompletableFuture<Collection<HandlerTask>>> futures = operations.stream()
                        .map(
                                x -> CompletableFuture.completedFuture(x)
                                        .thenApplyAsync(WorkFlowEngine.this::processHandlerTask, handlerTaskExecutor)
                                        .exceptionally(e -> null)
                        )
                        .collect(Collectors.toList());

                // waiting the completion of futures
                futures.parallelStream().forEach(x -> x.join());

            }, handlerTaskBatchExecutor).exceptionally(e -> {
                log.error("Error submitting batches:", e);
                return null;
            }).thenRun(eventtaskAccumulator::ready);
        }
    }

    /**
     * Initializes worklet execution.
     *
     * @param context workflow context
     */
    private void initWorkletExecution(WorkflowContext context) {
        context.setState(WorkflowState.RUNNING);
        context.setCause("");
        context.setWorkflowExecutionService(this);
        context.setWorkflowStore(workflowStore);
        context.setWorkplaceStore(workplaceStore);
        context.waitCompletion(null, null, null, 0L);
        context.setTriggerNext(false);
    }

    /**
     * Processes handler tasks.
     *
     * @param tasks handler tasks
     * @return handler tasks processed
     */
    private Collection<HandlerTask> processHandlerTask(Collection<HandlerTask> tasks) {

        for (HandlerTask task : tasks) {
            if (task instanceof EventTimeoutTask) {
                execEventTimeoutTask((EventTimeoutTask) task);
            } else if (task instanceof TimeoutTask) {
                execTimeoutTask((TimeoutTask) task);
            } else if (task instanceof EventTask) {
                execEventTask((EventTask) task);
            } else {
                log.error("Unsupported handler task {}", task);
            }
        }

        return null;
    }

    /**
     * Executes event task.
     *
     * @param task event task
     * @return event task
     */
    private EventTask execEventTask(EventTask task) {

        if (!eventMapStore.isEventMapPresent(task.context().name())) {
            log.trace("EventMap doesnt exist for taskcontext:{}", task.context().name());
            return task;
        }

        log.debug("execEventTask- task: {}, hash: {}", task, stringHash(task.context().distributor()));

        WorkflowContext context = (WorkflowContext) (task.context());
        Workflow workflow = workflowStore.get(context.workflowId());
        if (workflow == null) {
            log.error("Invalid workflow {}", context.workflowId());
            return task;
        }

        WorkflowContext latestContext = workplaceStore.getContext(context.name());
        if (latestContext == null) {
            log.error("Invalid workflow context {}", context.name());
            return task;
        }

        try {
            if (!Objects.equals(latestContext.current(), task.programCounter())) {
                log.error("Current worklet({}) is not mismatched with task work({}). Ignored.",
                        latestContext.current(), task.programCounter());
                return task;
            }

            Worklet worklet = workflow.getWorkletInstance(task.programCounter());
            if (Worklet.Common.COMPLETED.equals(worklet) || Worklet.Common.INIT.equals(worklet)) {
                log.error("Current worklet is {}, Ignored", worklet);
                return task;
            }

            initWorkletExecution(latestContext);

            log.info("{} worklet.isCompleted:{}", latestContext.name(), worklet.tag());
            log.trace("{} task:{}, context: {}", latestContext.name(), task, latestContext);

            workflowLoggerInjector.inject(worklet, latestContext);
            dataModelInjector.inject(worklet, latestContext);
            boolean completed = worklet.isCompleted(latestContext, task.event());
            dataModelInjector.inhale(worklet, latestContext);

            if (completed) {

                log.info("{} worklet.isCompleted(true):{}", latestContext.name(), worklet.tag());
                log.trace("{} task:{}, context: {}", latestContext.name(), task, latestContext);

                eventMapStore.unregisterEventMap(
                        task.eventType(), latestContext.name());

                //completed case
                //increase program counter
                ProgramCounter pc = latestContext.current();
                latestContext.setCurrent(workflow.increased(pc));

                workplaceStore.commitContext(latestContext.name(), latestContext, true);
                return null;
            } else {

                log.info("{} worklet.isCompleted(false):{}", latestContext.name(), worklet.tag());
                log.trace("{} task:{}, context: {}", latestContext.name(), task, latestContext);

                workplaceStore.commitContext(latestContext.name(), latestContext, false);
            }

        } catch (WorkflowException e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        } catch (StorageException e) {
            log.error("Exception: ", e);
            // StorageException does not commit context.
        } catch (Exception e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        }

        return task;
    }

    /**
     * Executes event timeout task.
     *
     * @param task event timeout task
     * @return handler task
     */
    private HandlerTask execEventTimeoutTask(EventTimeoutTask task) {

        if (!eventMapStore.isEventMapPresent(task.context().name())) {
            log.trace("EventMap doesnt exist for taskcontext:{}", task.context().name());
            return task;
        }

        log.debug("execEventTimeoutTask- task: {}, hash: {}", task, stringHash(task.context().distributor()));

        WorkflowContext context = (WorkflowContext) (task.context());
        Workflow workflow = workflowStore.get(context.workflowId());
        if (workflow == null) {
            log.error("execEventTimeoutTask: Invalid workflow {}", context.workflowId());
            return task;
        }

        WorkflowContext latestContext = workplaceStore.getContext(context.name());
        if (latestContext == null) {
            log.error("execEventTimeoutTask: Invalid workflow context {}", context.name());
            return task;
        }

        try {
            if (!Objects.equals(latestContext.current(), task.programCounter())) {
                log.error("execEventTimeoutTask: Current worklet({}) is not mismatched with task work({}). Ignored.",
                        latestContext.current(), task.programCounter());
                return task;
            }

            Worklet worklet = workflow.getWorkletInstance(task.programCounter());
            if (worklet == Worklet.Common.COMPLETED || worklet == Worklet.Common.INIT) {
                log.error("execEventTimeoutTask: Current worklet is {}, Ignored", worklet);
                return task;
            }

            initWorkletExecution(latestContext);

            eventMapStore.unregisterEventMap(task.eventType(), latestContext.name());

            log.info("{} worklet.timeout(for event):{}", latestContext.name(), worklet.tag());
            log.trace("{} task:{}, context: {}", latestContext.name(), task, latestContext);

            workflowLoggerInjector.inject(worklet, latestContext);
            dataModelInjector.inject(worklet, latestContext);

            WorkletDescription workletDesc = workflow.getWorkletDesc(task.programCounter());
            if (Objects.nonNull(workletDesc)) {
                if (!(workletDesc.tag().equals("INIT") || workletDesc.tag().equals("COMPLETED"))) {
                    staticDataModelInjector.inject(worklet, workletDesc);
                }
            }

            worklet.timeout(latestContext);
            dataModelInjector.inhale(worklet, latestContext);

            log.info("{} worklet.timeout(for event)(done):{}", latestContext.name(), worklet.tag());
            log.trace("{} task:{}, context: {}", latestContext.name(), task, latestContext);


            workplaceStore.commitContext(latestContext.name(), latestContext, latestContext.triggerNext());

        } catch (WorkflowException e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        } catch (StorageException e) {
            log.error("Exception: ", e);
            // StorageException does not commit context.
        } catch (Exception e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        }

        return task;
    }

    /**
     * Executes timeout task.
     *
     * @param task time out task
     * @return handler task
     */
    private HandlerTask execTimeoutTask(TimeoutTask task) {

        log.debug("execTimeoutTask- task: {}, hash: {}", task, stringHash(task.context().distributor()));

        WorkflowContext context = (WorkflowContext) (task.context());
        Workflow workflow = workflowStore.get(context.workflowId());
        if (workflow == null) {
            log.error("execTimeoutTask: Invalid workflow {}", context.workflowId());
            return task;
        }

        WorkflowContext latestContext = workplaceStore.getContext(context.name());
        if (latestContext == null) {
            log.error("execTimeoutTask: Invalid workflow context {}", context.name());
            return task;
        }

        try {
            if (!Objects.equals(latestContext.current(), task.programCounter())) {
                log.error("execTimeoutTask: Current worklet({}) is not mismatched with task work({}). Ignored.",
                        latestContext.current(), task.programCounter());
                return task;
            }

            Worklet worklet = workflow.getWorkletInstance(task.programCounter());
            if (worklet == Worklet.Common.COMPLETED || worklet == Worklet.Common.INIT) {
                log.error("execTimeoutTask: Current worklet is {}, Ignored", worklet);
                return task;
            }

            initWorkletExecution(latestContext);

            log.info("{} worklet.timeout:{}", latestContext.name(), worklet.tag());
            log.trace("{} context: {}", latestContext.name(), latestContext);

            workflowLoggerInjector.inject(worklet, latestContext);
            dataModelInjector.inject(worklet, latestContext);

            WorkletDescription workletDesc = workflow.getWorkletDesc(task.programCounter());
            if (Objects.nonNull(workletDesc)) {
                if (!(workletDesc.tag().equals("INIT") || workletDesc.tag().equals("COMPLETED"))) {
                    staticDataModelInjector.inject(worklet, workletDesc);
                }
            }

            worklet.timeout(latestContext);
            dataModelInjector.inhale(worklet, latestContext);

            log.info("{} worklet.timeout(done):{}", latestContext.name(), worklet.tag());
            log.trace("{} context: {}", latestContext.name(), latestContext);

            workplaceStore.commitContext(latestContext.name(), latestContext, latestContext.triggerNext());

        } catch (WorkflowException e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        } catch (StorageException e) {
            log.error("Exception: ", e);
            // StorageException does not commit context.
        } catch (Exception e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        }

        return task;
    }

    /**
     * Class for delegation of workflow batch execution.
     */
    private class InternalWorkflowBatchDelegate implements WorkflowBatchDelegate {
        @Override
        public void execute(Collection<WorkflowData> operations) {
            log.debug("Execute {} operation(s).", operations.size());

            CompletableFuture.runAsync(() -> {
                List<CompletableFuture<WorkflowData>> futures = operations.stream()
                        .map(
                                x -> CompletableFuture.completedFuture(x)
                                        .thenApplyAsync(WorkFlowEngine.this::execWorkflow, workflowExecutor)
                                        .exceptionally(e -> null)
                        )
                        .collect(Collectors.toList());

                // waiting the completion of futures
                futures.parallelStream().forEach(x -> x.join());

            }, workflowBatchExecutor).exceptionally(e -> {
                log.error("Error submitting batches:", e);
                return null;
            }).thenRun(workflowAccumulator::ready);
        }
    }

    /**
     * Executes workflow.
     *
     * @param dataModelContainer workflow data model container(workflow or workplace)
     * @return
     */
    private WorkflowData execWorkflow(WorkflowData dataModelContainer) {
        if (dataModelContainer instanceof WorkflowContext) {
            return execWorkflowContext((WorkflowContext) dataModelContainer);
        } else if (dataModelContainer instanceof Workplace) {
            return execWorkplace((Workplace) dataModelContainer);
        } else {
            log.error("Invalid context {}", dataModelContainer);
            return null;
        }
    }

    /**
     * Executes workflow context.
     *
     * @param context workflow context
     * @return workflow context
     */
    private WorkflowContext execWorkflowContext(WorkflowContext context) {

        Workflow workflow = workflowStore.get(context.workflowId());
        if (workflow == null) {
            log.error("Invalid workflow {}", context.workflowId());
            return null;
        }

        final WorkflowContext latestContext = workplaceStore.getContext(context.name());
        if (latestContext == null) {
            log.error("Invalid workflow context {}", context.name());
            return null;
        }

        initWorkletExecution(latestContext);

        try {
            final ProgramCounter pc = workflow.next(latestContext);
            final Worklet worklet = workflow.getWorkletInstance(pc);

            if (worklet == Worklet.Common.INIT) {
                log.error("workflow.next gave INIT. It cannot be executed (context: {})", context.name());
                return latestContext;
            }

            latestContext.setCurrent(pc);
            if (worklet == Worklet.Common.COMPLETED) {

                if (workflow.attributes().contains(REMOVE_AFTER_COMPLETE)) {
                    workplaceStore.removeContext(latestContext.name());
                    return null;
                } else {
                    latestContext.setState(WorkflowState.IDLE);
                    workplaceStore.commitContext(latestContext.name(), latestContext, false);
                    return latestContext;
                }
            }

            log.info("{} worklet.process:{}", latestContext.name(), worklet.tag());
            log.trace("{} context: {}", latestContext.name(), latestContext);


            workflowLoggerInjector.inject(worklet, latestContext);
            dataModelInjector.inject(worklet, latestContext);

            WorkletDescription workletDesc = workflow.getWorkletDesc(pc);
            if (Objects.nonNull(workletDesc)) {
                if (!(workletDesc.tag().equals("INIT") || workletDesc.tag().equals("COMPLETED"))) {
                    staticDataModelInjector.inject(worklet, workletDesc);
                }
            }

            worklet.process(latestContext);
            dataModelInjector.inhale(worklet, latestContext);

            log.info("{} worklet.process(done): {}", latestContext.name(), worklet.tag());
            log.trace("{} context: {}", latestContext.name(), latestContext);

            if (latestContext.completionEventType() != null) {
                if (latestContext.completionEventGenerator() == null) {
                    String msg = String.format("Invalid exepecting event(%s), generator(%s)",
                            latestContext.completionEventType(),
                            latestContext.completionEventGenerator());
                    throw new WorkflowException(msg);
                }

                registerEventMap(latestContext.completionEventType(), latestContext.completionEventHints(),
                        latestContext.name(), pc.toString());

                latestContext.completionEventGenerator().apply();

                if (latestContext.completionEventTimeout() != 0L) {
                    final EventTimeoutTask eventTimeoutTask = EventTimeoutTask.builder()
                            .context(latestContext)
                            .programCounter(pc)
                            .eventType(latestContext.completionEventType().getName())
                            .eventHintSet(latestContext.completionEventHints())
                            .build();
                    timerChain.schedule(latestContext.completionEventTimeout(),
                            () -> {
                                eventtaskAccumulator.add(eventTimeoutTask);
                            });
                }
            } else {
                if (latestContext.completionEventTimeout() != 0L) {
                    final TimeoutTask timeoutTask = TimeoutTask.builder()
                            .context(latestContext)
                            .programCounter(pc)
                            .build();

                    timerChain.schedule(latestContext.completionEventTimeout(),
                            () -> {
                                eventtaskAccumulator.add(timeoutTask);
                            });
                } else {
                    //completed case
                    // increase program counter
                    latestContext.setCurrent(workflow.increased(pc));
                }
            }
            workplaceStore.commitContext(latestContext.name(), latestContext, latestContext.triggerNext());

        } catch (WorkflowException e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        } catch (StorageException e) {
            log.error("Exception: ", e);
            // StorageException does not commit context.
        } catch (Exception e) {
            log.error("Exception: ", e);
            latestContext.setCause(e.getMessage());
            latestContext.setState(WorkflowState.EXCEPTION);
            workplaceStore.commitContext(latestContext.name(), latestContext, false);
        }

        return latestContext;
    }

    /**
     * Execute workplace.
     *
     * @param workplace workplace
     * @return workplace
     */
    private Workplace execWorkplace(Workplace workplace) {

        return null;
    }

}
