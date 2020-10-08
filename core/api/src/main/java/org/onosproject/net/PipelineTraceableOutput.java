/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.net;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Represents the output of the pipeline traceable processing.
 */
public final class PipelineTraceableOutput {

    /**
     * Represents the result of the pipeline traceable processing.
     */
    public enum PipelineTraceableResult {
        /**
         * Means packet went through the pipeline.
         */
        SUCCESS,
        /**
         * Means packet stopped due to missing flows.
         */
        NO_FLOWS,
        /**
         * Means packet stopped due to missing groups.
         */
        NO_GROUPS,
        /**
         * Means packet stopped due to an empty group.
         */
        NO_GROUP_MEMBERS,
        /**
         * Means packet is dropped by the pipeline.
         */
        DROPPED
    }

    private String log;
    private List<PipelineTraceableHitChain> hitChains;
    private PipelineTraceableResult result;

    /**
     * Creates a new pipeline traceable output with the specified input.
     *
     * @param log the trace log
     * @param hitChains the hit chains
     * @param result the apply result
     */
    private PipelineTraceableOutput(String log, List<PipelineTraceableHitChain> hitChains,
                                    PipelineTraceableResult result) {
        this.log = log;
        this.hitChains = hitChains;
        this.result = result;
    }

    /**
     * Returns the log message as string.
     *
     * @return the log message
     */
    public String log() {
        return log;
    }

    /**
     * Returns the hit chains.
     *
     * @return the pipeline hit chains
     */
    public List<PipelineTraceableHitChain> hitChains() {
        return hitChains;
    }

    /**
     * Returns the result of the computation.
     *
     * @return the pipeline traceable result
     */
    public PipelineTraceableResult result() {
        return result;
    }

    /**
     * Returns a new builder.
     *
     * @return an empty builder
     */
    public static PipelineTraceableOutput.Builder builder() {
        return new PipelineTraceableOutput.Builder();
    }

    /**
     * Returns a new builder initialized with the traceable output.
     *
     * @param pipelineTraceableOutput the output used for the initialization
     * @return an initialized builder
     */
    public static PipelineTraceableOutput.Builder builder(PipelineTraceableOutput pipelineTraceableOutput) {
        return new PipelineTraceableOutput.Builder(pipelineTraceableOutput);
    }

    /**
     * Builder of pipeline traceable entities.
     */
    public static final class Builder {

        private StringBuilder log = new StringBuilder();
        private List<PipelineTraceableHitChain> hitChains = Lists.newArrayList();
        private PipelineTraceableResult result = PipelineTraceableResult.SUCCESS;

        private Builder() {
        }

        private Builder(PipelineTraceableOutput traceableOutput) {
            appendToLog("\n" + traceableOutput.log());
            setResult(traceableOutput.result());
            traceableOutput.hitChains().forEach(this::addHitChain);
        }

        /**
         * Appends a message to the log.
         *
         * @param message the log message to be appended
         * @return this builder
         */
        public Builder appendToLog(String message) {
            if (log.length() != 0) {
                log.append("\n");
            }
            log.append(message);
            return this;
        }

        public Builder setResult(PipelineTraceableResult result) {
            // Do not override original failure
            if (this.result == PipelineTraceableResult.SUCCESS) {
                this.result = result;
            }
            return this;
        }

        /**
         * Sets no flows in the result.
         *
         * @return this builder
         */
        public Builder noFlows() {
            return setResult(PipelineTraceableResult.NO_FLOWS);
        }

        /**
         * Sets no groups in the result.
         *
         * @return this builder
         */
        public Builder noGroups() {
            return setResult(PipelineTraceableResult.NO_GROUPS);
        }

        /**
         * Sets no flows in the result.
         *
         * @return this builder
         */
        public Builder noMembers() {
            return setResult(PipelineTraceableResult.NO_GROUP_MEMBERS);
        }

        /**
         * Sets dropped in the result.
         *
         * @return this builder
         */
        public Builder dropped() {
            return setResult(PipelineTraceableResult.DROPPED);
        }

        /**
         * Stores the provided hit chain.
         *
         * @param hitChain the provided hit chain
         * @return this builder
         */
        public Builder addHitChain(PipelineTraceableHitChain hitChain) {
            if (!hitChains.contains(hitChain)) {
                hitChains.add(hitChain);
            }
            return this;
        }

        /**
         * Builds a new pipeline traceable output.
         *
         * @return a pipeline traceable object
         */
        public PipelineTraceableOutput build() {
            return new PipelineTraceableOutput(log.toString(), hitChains, result);
        }

    }

}
