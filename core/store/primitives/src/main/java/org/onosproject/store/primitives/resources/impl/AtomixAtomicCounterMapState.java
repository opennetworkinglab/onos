/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.store.primitives.resources.impl;

import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import io.atomix.resource.ResourceStateMachine;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.AddAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.DecrementAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Get;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.GetAndAdd;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.GetAndDecrement;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.GetAndIncrement;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.IncrementAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.IsEmpty;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Put;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.PutIfAbsent;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Remove;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.RemoveValue;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Replace;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Size;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Atomic counter map state for Atomix.
 * <p>
 * The counter map state is implemented as a snapshottable state machine. Snapshots are necessary
 * since incremental compaction is impractical for counters where the value of a counter is the sum
 * of all its increments. Note that this snapshotting large state machines may risk blocking of the
 * Raft cluster with the current implementation of snapshotting in Copycat.
 */
public class AtomixAtomicCounterMapState extends ResourceStateMachine implements Snapshottable {
    private Map<String, Long> map = new HashMap<>();

    public AtomixAtomicCounterMapState(Properties config) {
        super(config);
    }

    @Override
    protected void configure(StateMachineExecutor executor) {
        executor.register(Put.class, this::put);
        executor.register(PutIfAbsent.class, this::putIfAbsent);
        executor.register(Get.class, this::get);
        executor.register(Replace.class, this::replace);
        executor.register(Remove.class, this::remove);
        executor.register(RemoveValue.class, this::removeValue);
        executor.register(GetAndIncrement.class, this::getAndIncrement);
        executor.register(GetAndDecrement.class, this::getAndDecrement);
        executor.register(IncrementAndGet.class, this::incrementAndGet);
        executor.register(DecrementAndGet.class, this::decrementAndGet);
        executor.register(AddAndGet.class, this::addAndGet);
        executor.register(GetAndAdd.class, this::getAndAdd);
        executor.register(Size.class, this::size);
        executor.register(IsEmpty.class, this::isEmpty);
        executor.register(Clear.class, this::clear);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeObject(map);
    }

    @Override
    public void install(SnapshotReader reader) {
        map = reader.readObject();
    }

    /**
     * Returns the primitive value for the given primitive wrapper.
     */
    private long primitive(Long value) {
        if (value != null) {
            return value;
        } else {
            return 0;
        }
    }

    /**
     * Handles a {@link Put} command which implements {@link AtomixAtomicCounterMap#put(String, long)}.
     *
     * @param commit put commit
     * @return put result
     */
    protected long put(Commit<Put> commit) {
        try {
            return primitive(map.put(commit.operation().key(), commit.operation().value()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link PutIfAbsent} command which implements {@link AtomixAtomicCounterMap#putIfAbsent(String, long)}.
     *
     * @param commit putIfAbsent commit
     * @return putIfAbsent result
     */
    protected long putIfAbsent(Commit<PutIfAbsent> commit) {
        try {
            return primitive(map.putIfAbsent(commit.operation().key(), commit.operation().value()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link Get} query which implements {@link AtomixAtomicCounterMap#get(String)}}.
     *
     * @param commit get commit
     * @return get result
     */
    protected long get(Commit<Get> commit) {
        try {
            return primitive(map.get(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link Replace} command which implements {@link AtomixAtomicCounterMap#replace(String, long, long)}.
     *
     * @param commit replace commit
     * @return replace result
     */
    protected boolean replace(Commit<Replace> commit) {
        try {
            Long value = map.get(commit.operation().key());
            if (value == null) {
                if (commit.operation().replace() == 0) {
                    map.put(commit.operation().key(), commit.operation().value());
                    return true;
                } else {
                    return false;
                }
            } else if (value == commit.operation().replace()) {
                map.put(commit.operation().key(), commit.operation().value());
                return true;
            }
            return false;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link Remove} command which implements {@link AtomixAtomicCounterMap#remove(String)}.
     *
     * @param commit remove commit
     * @return remove result
     */
    protected long remove(Commit<Remove> commit) {
        try {
            return primitive(map.remove(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link RemoveValue} command which implements {@link AtomixAtomicCounterMap#remove(String, long)}.
     *
     * @param commit removeValue commit
     * @return removeValue result
     */
    protected boolean removeValue(Commit<RemoveValue> commit) {
        try {
            Long value = map.get(commit.operation().key());
            if (value == null) {
                if (commit.operation().value() == 0) {
                    map.remove(commit.operation().key());
                    return true;
                }
                return false;
            } else if (value == commit.operation().value()) {
                map.remove(commit.operation().key());
                return true;
            }
            return false;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link GetAndIncrement} command which implements
     * {@link AtomixAtomicCounterMap#getAndIncrement(String)}.
     *
     * @param commit getAndIncrement commit
     * @return getAndIncrement result
     */
    protected long getAndIncrement(Commit<GetAndIncrement> commit) {
        try {
            long value = primitive(map.get(commit.operation().key()));
            map.put(commit.operation().key(), value + 1);
            return value;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link GetAndDecrement} command which implements
     * {@link AtomixAtomicCounterMap#getAndDecrement(String)}.
     *
     * @param commit getAndDecrement commit
     * @return getAndDecrement result
     */
    protected long getAndDecrement(Commit<GetAndDecrement> commit) {
        try {
            long value = primitive(map.get(commit.operation().key()));
            map.put(commit.operation().key(), value - 1);
            return value;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link IncrementAndGet} command which implements
     * {@link AtomixAtomicCounterMap#incrementAndGet(String)}.
     *
     * @param commit incrementAndGet commit
     * @return incrementAndGet result
     */
    protected long incrementAndGet(Commit<IncrementAndGet> commit) {
        try {
            long value = primitive(map.get(commit.operation().key()));
            map.put(commit.operation().key(), ++value);
            return value;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link DecrementAndGet} command which implements
     * {@link AtomixAtomicCounterMap#decrementAndGet(String)}.
     *
     * @param commit decrementAndGet commit
     * @return decrementAndGet result
     */
    protected long decrementAndGet(Commit<DecrementAndGet> commit) {
        try {
            long value = primitive(map.get(commit.operation().key()));
            map.put(commit.operation().key(), --value);
            return value;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link AddAndGet} command which implements {@link AtomixAtomicCounterMap#addAndGet(String, long)}.
     *
     * @param commit addAndGet commit
     * @return addAndGet result
     */
    protected long addAndGet(Commit<AddAndGet> commit) {
        try {
            long value = primitive(map.get(commit.operation().key()));
            value += commit.operation().delta();
            map.put(commit.operation().key(), value);
            return value;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link GetAndAdd} command which implements {@link AtomixAtomicCounterMap#getAndAdd(String, long)}.
     *
     * @param commit getAndAdd commit
     * @return getAndAdd result
     */
    protected long getAndAdd(Commit<GetAndAdd> commit) {
        try {
            long value = primitive(map.get(commit.operation().key()));
            map.put(commit.operation().key(), value + commit.operation().delta());
            return value;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link Size} query which implements {@link AtomixAtomicCounterMap#size()}.
     *
     * @param commit size commit
     * @return size result
     */
    protected int size(Commit<Size> commit) {
        try {
            return map.size();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles an {@link IsEmpty} query which implements {@link AtomixAtomicCounterMap#isEmpty()}.
     *
     * @param commit isEmpty commit
     * @return isEmpty result
     */
    protected boolean isEmpty(Commit<IsEmpty> commit) {
        try {
            return map.isEmpty();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a {@link Clear} command which implements {@link AtomixAtomicCounterMap#clear()}.
     *
     * @param commit clear commit
     */
    protected void clear(Commit<Clear> commit) {
        try {
            map.clear();
        } finally {
            commit.close();
        }
    }
}
