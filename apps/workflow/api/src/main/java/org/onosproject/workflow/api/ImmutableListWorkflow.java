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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.onosproject.workflow.api.CheckCondition.check;

/**
 * Class for immutable list workflow.
 */
public final class ImmutableListWorkflow extends AbstractWorkflow {

    /**
     * Init worklet type(class name of init worklet type).
     */
    private String initWorkletType;

    /**
     * Sequential program(List of worklet desc) to be executed.
     */
    private List<WorkletDescription> program;

    /**
     * Set of workflow attributes.
     */
    private Set<WorkflowAttribute> attributes;

    private static JsonDataModelInjector dataModelInjector = new JsonDataModelInjector();
    private static StaticDataModelInjector staticDataModelInjector = new StaticDataModelInjector();

    /**
     * Workflow Logger injector.
     */
    private WorkflowLoggerInjector workflowLoggerInjector = new WorkflowLoggerInjector();

    /**
     * Constructor of ImmutableListWorkflow.
     *
     * @param builder builder of ImmutableListWorkflow
     */
    private ImmutableListWorkflow(Builder builder) {
        super(builder.id);
        this.initWorkletType = builder.initWorkletType;
        program = ImmutableList.copyOf(builder.workletDescList);
        attributes = ImmutableSet.copyOf(builder.attributes);
    }

    @Override
    public Worklet init(WorkflowContext context) throws WorkflowException {
        if (Objects.isNull(initWorkletType)) {
            return null;
        }

        return getWorkletInstance(initWorkletType);
    }

    @Override
    public ProgramCounter next(WorkflowContext context) throws WorkflowException {

        int cnt = 0;

        ProgramCounter current = context.current();
        check(current != null, "Invalid program counter");

        ProgramCounter pc = current.clone();

        for (int i = current.workletIndex(); i < program.size(); pc = increased(pc), i++) {

            if (cnt++ > Worklet.MAX_WORKS) {
                throw new WorkflowException("Maximum worklet execution exceeded");
            }
            if (pc.isCompleted()) {
                return pc;
            }

            if (pc.isInit()) {
                continue;
            }

            Worklet worklet = getWorkletInstance(pc);
            Class workClass = worklet.getClass();

            if (BranchWorklet.class.isAssignableFrom(workClass)) {
                Class nextClass = ((BranchWorklet) worklet).next(context);
                if (nextClass == null) {
                    throw new WorkflowException("Invalid next Worklet for " + workClass);
                }

                // TODO : it does not support duplicated use of WorkType. It needs to consider label concept
                int nextIdx = getClassIndex(nextClass);
                if (nextIdx == -1) {
                    throw new WorkflowException("Failed to find next " + nextClass + " for " + workClass);
                }

                i = nextIdx;
                continue;

            } else {
                workflowLoggerInjector.inject(worklet, context);
                // isNext is read only. It does not perform 'inhale'.
                dataModelInjector.inject(worklet, context);
                WorkletDescription workletDesc = getWorkletDesc(pc);
                if (Objects.nonNull(workletDesc)) {
                    if (!(workletDesc.tag().equals("INIT") || workletDesc.tag().equals("COMPLETED"))) {
                        staticDataModelInjector.inject(worklet, workletDesc);
                    }
                }
                if (worklet.isNext(context)) {
                    return pc;
                }
            }
        }
        throw new WorkflowException("workflow reached to end but not COMPLETED (pc:"
                + current + ", workflow:" + this.toString());
    }

    @Override
    public ProgramCounter increased(ProgramCounter pc) throws WorkflowException {

        int increaedIndex = pc.workletIndex() + 1;
        if (increaedIndex >= program.size()) {
            throw new WorkflowException("Out of bound in program counter(" + pc + ")");
        }

        WorkletDescription workletDesc = program.get(increaedIndex);
        return ProgramCounter.valueOf(workletDesc.tag(), increaedIndex);
    }

    @Override
    public Worklet getWorkletInstance(ProgramCounter pc) throws WorkflowException {

        return getWorkletInstance(program.get(pc.workletIndex()).tag());
    }

    private Worklet getWorkletInstance(String workletType) throws WorkflowException {

        if (Worklet.Common.INIT.tag().equals(workletType)) {
            return Worklet.Common.INIT;
        }

        if (Worklet.Common.COMPLETED.tag().equals(workletType)) {
            return Worklet.Common.COMPLETED;
        }

        WorkflowStore store;
        try {
            store = DefaultServiceDirectory.getService(WorkflowStore.class);
        } catch (ServiceNotFoundException e) {
            throw new WorkflowException(e);
        }

        Class workClass;
        try {
            workClass = store.getClass(workletType);
        } catch (ClassNotFoundException e) {
            throw new WorkflowException(e);
        }

        if (!isAllowed(workClass)) {
            throw new WorkflowException("Not allowed class(" + workClass.getSimpleName() + ")");
        }

        Worklet worklet;
        try {
            worklet = (Worklet) workClass.newInstance();
        } catch (Exception e) {
            throw new WorkflowException(e);
        }

        return worklet;
    }

    @Override
    public Set<WorkflowAttribute> attributes() {
        return ImmutableSet.copyOf(attributes);
    }

    @Override
    public List<ProgramCounter> getProgram() {

        int size = program.size();
        List pcList = new ArrayList();
        for (int i = 0; i < size; i++) {
            pcList.add(ProgramCounter.valueOf(program.get(i).tag(), i));
        }

        return pcList;
    }

    @Override
    public WorkletDescription getWorkletDesc(ProgramCounter pc) {

        WorkletDescription workletDescription = program.get(pc.workletIndex());
        return workletDescription;
    }


    /**
     * Gets index of class in worklet type list.
     *
     * @param aClass class to get index
     * @return index of class in worklet type list
     */
    private int getClassIndex(Class aClass) {
        for (int i = 0; i < program.size(); i++) {
            if (Objects.equals(aClass.getName(), program.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks whether class is allowed class or not.
     *
     * @param clazz class to check
     * @return Check result
     */
    private boolean isAllowed(Class clazz) {
        // non static inner class is not allowed
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            return false;
        }
        // enum is not allowed
        if (clazz.isEnum()) {
            return false;
        }
        // class should be subclass of Work
        if (!Worklet.class.isAssignableFrom(clazz)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EventTask)) {
            return false;
        }
        return Objects.equals(this.id(), ((ImmutableListWorkflow) obj).id())
                && Objects.equals(this.initWorkletType, ((ImmutableListWorkflow) obj).initWorkletType)
                && Objects.equals(this.program, ((ImmutableListWorkflow) obj).program)
                && Objects.equals(this.attributes, ((ImmutableListWorkflow) obj).attributes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("initWorklet", initWorkletType)
                .add("workList", program)
                .add("attributes", attributes)
                .toString();
    }

    /**
     * Gets a instance of builder.
     *
     * @return instance of builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of ImmutableListWorkflow.
     */
    public static class Builder {

        private URI id;
        private String initWorkletType;
        private final List<WorkletDescription> workletDescList = Lists.newArrayList();
        private final Set<WorkflowAttribute> attributes = Sets.newHashSet();

        /**
         * Sets id of immutable list workflow.
         *
         * @param uri id of immutable list workflow
         * @return builder
         */
        public Builder id(URI uri) {
            this.id = uri;
            workletDescList.add(new DefaultWorkletDescription(Worklet.Common.INIT.tag()));
            return this;
        }

        /**
         * Sets init worklet class name of immutable list workflow.
         *
         * @param workletClassName class name of worklet
         * @return builder
         */
        public Builder init(String workletClassName) {
            this.initWorkletType = workletClassName;
            return this;
        }

        /**
         * Chains worklet class name of immutable list workflow.
         *
         * @param workletClassName class name of worklet
         * @return builder
         */
        public Builder chain(String workletClassName) {
            workletDescList.add(new DefaultWorkletDescription(workletClassName));
            return this;
        }

        /**
         * Adds workflow attribute.
         *
         * @param attribute workflow attribute to be added
         * @return builder
         */
        public Builder attribute(WorkflowAttribute attribute) {
            attributes.add(attribute);
            return this;
        }

        /**
         * Builds ImmutableListWorkflow.
         *
         * @return instance of ImmutableListWorkflow
         */
        public ImmutableListWorkflow build() {
            workletDescList.add(new DefaultWorkletDescription(Worklet.Common.COMPLETED.tag()));
            return new ImmutableListWorkflow(this);
        }

        public Builder chain(DefaultWorkletDescription workletDesc) {
            workletDescList.add(workletDesc);
            return this;
        }
    }
}
