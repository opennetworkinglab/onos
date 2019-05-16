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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An interface representing workflow program counter.
 */
public final class ProgramCounter {

    public static final ProgramCounter INIT_PC = ProgramCounter.valueOf(Worklet.Common.INIT.name(), 0);

    /**
     * index of the worklet.
     */
    private final int workletIndex;

    /**
     * Type of worklet.
     */
    private final String workletType;

    /**
     * Index of worklet.
     * @return index of worklet
     */
    public int workletIndex() {
        return this.workletIndex;
    }

    /**
     * Type of worklet.
     * @return type of worklet
     */
    public String workletType() {
        return this.workletType;
    }

    /**
     * Constructor of workflow Program Counter.
     * @param workletType type of worklet
     * @param workletIndex index of worklet
     */
    private ProgramCounter(String workletType, int workletIndex) {
        this.workletType = workletType;
        this.workletIndex = workletIndex;
    }

    /**
     * Clones this workflow Program Counter.
     * @return clone of this workflow Program Counter
     */
    public ProgramCounter clone() {
        return ProgramCounter.valueOf(this.workletType(), this.workletIndex());
    }

    /**
     * Returns whether this program counter is INIT worklet program counter.
     * @return whether this program counter is INIT worklet program counter
     */
    public boolean isInit() {
        return Worklet.Common.INIT.tag().equals(this.workletType);
    }

    /**
     * Returns whether this program counter is COMPLETED worklet program counter.
     * @return whether this program counter is COMPLETED worklet program counter
     */
    public boolean isCompleted() {
        return Worklet.Common.COMPLETED.tag().equals(this.workletType);
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
        if (!(obj instanceof ProgramCounter)) {
            return false;
        }
        return Objects.equals(this.workletType(), ((ProgramCounter) obj).workletType())
                && Objects.equals(this.workletIndex(), ((ProgramCounter) obj).workletIndex());
    }

    @Override
    public String toString() {
        return String.format("(%d)%s", workletIndex, workletType);
    }

    /**
     * Builder of workflow Program Counter.
     * @param workletType type of worklet
     * @param workletIndex index of worklet
     * @return program counter
     */
    public static ProgramCounter valueOf(String workletType, int workletIndex) {
        return new ProgramCounter(workletType, workletIndex);
    }

    /**
     * Builder of workflow Program Counter.
     * @param strProgramCounter string format for program counter
     * @return program counter
     */
    public static ProgramCounter valueOf(String strProgramCounter) {

        Matcher m = Pattern.compile("\\((\\d+)\\)(.+)").matcher(strProgramCounter);

        if (!m.matches()) {
            throw new IllegalArgumentException("Malformed program counter string");
        }

        return new ProgramCounter(m.group(2), Integer.parseInt(m.group(1)));
    }

}

