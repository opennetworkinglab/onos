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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class BatchWrite {

    public enum OpType {
        CREATE_INTENT,
        REMOVE_INTENT,
        SET_STATE,
        SET_INSTALLABLE,
        REMOVE_INSTALLED
    }

    List<Operation> operations = new ArrayList<>();

    private BatchWrite() {}

    /**
     * Returns a new empty batch write operation builder.
     *
     * @return BatchWrite
     */
    public static BatchWrite newInstance() {
        return new BatchWrite();
    }

    public List<Operation> operations() {
        return Collections.unmodifiableList(operations);
    }

    public boolean isEmpty() {
        return operations.isEmpty();
    }

    public BatchWrite createIntent(Intent intent) {
        operations.add(Operation.of(OpType.CREATE_INTENT,
                ImmutableList.of(intent)));
        return this;
    }

    public BatchWrite removeIntent(IntentId intentId) {
        operations.add(Operation.of(OpType.REMOVE_INTENT,
                ImmutableList.of(intentId)));
        return this;
    }

    public BatchWrite setState(Intent intent, IntentState newState) {
        operations.add(Operation.of(OpType.SET_STATE,
                ImmutableList.of(intent, newState)));
        return this;
    }

    public BatchWrite setInstallableIntents(IntentId intentId, List<Intent> installableIntents) {
        operations.add(Operation.of(OpType.SET_INSTALLABLE,
                ImmutableList.of(intentId, installableIntents)));
        return this;
    }

    public BatchWrite removeInstalledIntents(IntentId intentId) {
        operations.add(Operation.of(OpType.REMOVE_INSTALLED,
                ImmutableList.of(intentId)));
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("operations", operations)
                .toString();
    }

    public static class Operation {
        final OpType type;
        final ImmutableList<Object> args;

        public static Operation of(OpType type, List<Object> args) {
            return new Operation(type, args);
        }

        // TODO: consider make it private
        public Operation(OpType type, List<Object> args) {
            this.type = checkNotNull(type);
            this.args = ImmutableList.copyOf(args);
        }

        public OpType type() {
            return type;
        }

        public ImmutableList<Object> args() {
            return args;
        }

        @SuppressWarnings("unchecked")
        public <T> T arg(int i) {
            return (T) args.get(i);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("type", type)
                    .add("args", args)
                    .toString();
        }
    }
}
