/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import io.atomix.resource.ResourceStateMachine;
import org.onlab.util.CountDownCompleter;
import org.onlab.util.Match;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Clear;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.ContainsEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Entries;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Get;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.IsEmpty;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.KeySet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Keys;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.MultiRemove;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.MultimapCommand;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Put;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.RemoveAll;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Replace;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Size;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Values;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * State Machine for {@link AtomixConsistentSetMultimap} resource.
 */
public class AtomixConsistentSetMultimapState extends ResourceStateMachine
        implements SessionListener, Snapshottable {

    private final Logger log = getLogger(getClass());
    private final AtomicLong globalVersion = new AtomicLong(1);
    //TODO Add listener map here
    private final Map<String, MapEntryValue> backingMap = Maps.newHashMap();

    public AtomixConsistentSetMultimapState(Properties properties) {
        super(properties);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
    }

    @Override
    public void install(SnapshotReader reader) {
    }

    @Override
    protected void configure(StateMachineExecutor executor) {
        executor.register(Size.class, this::size);
        executor.register(IsEmpty.class, this::isEmpty);
        executor.register(ContainsKey.class, this::containsKey);
        executor.register(ContainsValue.class, this::containsValue);
        executor.register(ContainsEntry.class, this::containsEntry);
        executor.register(Clear.class, this::clear);
        executor.register(KeySet.class, this::keySet);
        executor.register(Keys.class, this::keys);
        executor.register(Values.class, this::values);
        executor.register(Entries.class, this::entries);
        executor.register(Get.class, this::get);
        executor.register(RemoveAll.class, this::removeAll);
        executor.register(MultiRemove.class, this::multiRemove);
        executor.register(Put.class, this::put);
        executor.register(Replace.class, this::replace);
    }

    /**
     * Handles a Size commit.
     *
     * @param commit Size commit
     * @return number of unique key value pairs in the multimap
     */
    protected int size(Commit<? extends Size> commit) {
        try {
            return backingMap.values()
                    .stream()
                    .map(valueCollection -> valueCollection.values().size())
                    .collect(Collectors.summingInt(size -> size));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles an IsEmpty commit.
     *
     * @param commit IsEmpty commit
     * @return true if the multimap contains no key-value pairs, else false
     */
    protected boolean isEmpty(Commit<? extends IsEmpty> commit) {
        try {
            return backingMap.isEmpty();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a contains key commit.
     *
     * @param commit ContainsKey commit
     * @return returns true if the key is in the multimap, else false
     */
    protected boolean containsKey(Commit<? extends ContainsKey> commit) {
        try {
            return backingMap.containsKey(commit.operation().key());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a ContainsValue commit.
     *
     * @param commit ContainsValue commit
     * @return true if the value is in the multimap, else false
     */
    protected boolean containsValue(Commit<? extends ContainsValue> commit) {
        try {
            if (backingMap.values().isEmpty()) {
                return false;
            }
            Match<byte[]> match = Match.ifValue(commit.operation().value());
            return backingMap
                    .values()
                    .stream()
                    .anyMatch(valueList ->
                                      valueList
                                              .values()
                                              .stream()
                                              .anyMatch(byteValue ->
                                                    match.matches(byteValue)));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a ContainsEntry commit.
     *
     * @param commit ContainsEntry commit
     * @return true if the key-value pair exists, else false
     */
    protected boolean containsEntry(Commit<? extends ContainsEntry> commit) {
        try {
            MapEntryValue entryValue =
                    backingMap.get(commit.operation().key());
            if (entryValue == null) {
                return false;
            } else {
                Match valueMatch = Match.ifValue(commit.operation().value());
                return entryValue
                        .values()
                        .stream()
                        .anyMatch(byteValue -> valueMatch.matches(byteValue));
            }
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a Clear commit.
     *
     * @param commit Clear commit
     */
    protected void clear(Commit<? extends Clear> commit) {
        try {
            backingMap.clear();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a KeySet commit.
     *
     * @param commit KeySet commit
     * @return a set of all keys in the multimap
     */
    protected Set<String> keySet(Commit<? extends KeySet> commit) {
        try {
            return ImmutableSet.copyOf(backingMap.keySet());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a Keys commit.
     *
     * @param commit Keys commit
     * @return a multiset of keys with each key included an equal number of
     * times to the total key-value pairs in which that key participates
     */
    protected Multiset<String> keys(Commit<? extends Keys> commit) {
        try {
            Multiset keys = HashMultiset.create();
            backingMap.forEach((key, mapEntryValue) -> {
                keys.add(key, mapEntryValue.values().size());
            });
            return keys;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a Values commit.
     *
     * @param commit Values commit
     * @return the set of values in the multimap with duplicates included
     */
    protected Multiset<byte[]> values(Commit<? extends Values> commit) {
        try {
            return backingMap
                    .values()
                    .stream()
                    .collect(new HashMultisetValueCollector());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles an Entries commit.
     *
     * @param commit Entries commit
     * @return a set of all key-value pairs in the multimap
     */
    protected Collection<Map.Entry<String, byte[]>> entries(
            Commit<? extends Entries> commit) {
        try {
            return backingMap
                    .entrySet()
                    .stream()
                    .collect(new EntrySetCollector());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a Get commit.
     *
     * @param commit Get commit
     * @return the collection of values associated with the key or an empty
     * list if none exist
     */
    protected Versioned<Collection<? extends byte[]>> get(
            Commit<? extends Get> commit) {
        try {
            MapEntryValue mapEntryValue = backingMap.get(commit.operation().key());
            return toVersioned(backingMap.get(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a removeAll commit, and returns the previous mapping.
     *
     * @param commit removeAll commit
     * @return collection of removed values
     */
    protected Versioned<Collection<? extends byte[]>> removeAll(
            Commit<? extends RemoveAll> commit) {
        if (!backingMap.containsKey(commit.operation().key())) {
            commit.close();
            return new Versioned<>(Sets.newHashSet(), -1);
        } else {
            return backingMap.get(commit.operation().key()).addCommit(commit);
        }
    }

    /**
     * Handles a multiRemove commit, returns true if the remove results in any
     * change.
     * @param commit multiRemove commit
     * @return true if any change results, else false
     */
    protected boolean multiRemove(Commit<? extends MultiRemove> commit) {
        if (!backingMap.containsKey(commit.operation().key())) {
            commit.close();
            return false;
        } else {
            return (backingMap
                    .get(commit.operation().key())
                    .addCommit(commit)) != null;
        }
    }

    /**
     * Handles a put commit, returns true if any change results from this
     * commit.
     * @param commit a put commit
     * @return true if this commit results in a change, else false
     */
    protected boolean put(Commit<? extends Put> commit) {
        if (commit.operation().values().isEmpty()) {
            return false;
        }
        if (!backingMap.containsKey(commit.operation().key())) {
            backingMap.put(commit.operation().key(),
                           new NonTransactionalCommit(1));
        }
        return backingMap
                .get(commit.operation().key())
                .addCommit(commit) != null;
    }

    protected Versioned<Collection<? extends byte[]>> replace(
            Commit<? extends Replace> commit) {
        if (!backingMap.containsKey(commit.operation().key())) {
            backingMap.put(commit.operation().key(),
                           new NonTransactionalCommit(1));
        }
        return backingMap.get(commit.operation().key()).addCommit(commit);
    }

    private interface MapEntryValue {

        /**
         * Returns the list of raw {@code byte[]'s}.
         *
         * @return list of raw values
         */
        Collection<? extends byte[]> values();

        /**
         * Returns the version of the value.
         *
         * @return version
         */
        long version();

        /**
         * Discards the value by invoke appropriate clean up actions.
         */
        void discard();

        /**
         * Add a new commit and modifies the set of values accordingly.
         * In the case of a replace or removeAll it returns the set of removed
         * values. In the case of put or multiRemove it returns null for no
         * change and a set of the added or removed values respectively if a
         * change resulted.
         *
         * @param commit the commit to be added
         */
        Versioned<Collection<? extends byte[]>> addCommit(
                Commit<? extends MultimapCommand> commit);
    }

    private class NonTransactionalCommit implements MapEntryValue {
        private long version;
        private final TreeMap<byte[], CountDownCompleter<Commit>>
                valueCountdownMap = Maps.newTreeMap(new ByteArrayComparator());
        /*This is a mapping of commits that added values to the commits
        * removing those values, they will not be circular because keys will
        * be exclusively Put and Replace commits and values will be exclusively
        * Multiremove commits, each time a Put or replace is removed it should
        * as part of closing go through and countdown each of the remove
        * commits depending on it.*/
        private final HashMultimap<Commit, CountDownCompleter<Commit>>
                additiveToRemovalCommits = HashMultimap.create();

        public NonTransactionalCommit(
                long version) {
            //Set the version to current it will only be updated once this is
            // populated
            this.version = globalVersion.get();
        }

        @Override
        public Collection<? extends byte[]> values() {
            return ImmutableSet.copyOf(valueCountdownMap.keySet());
        }

        @Override
        public long version() {
            return version;
        }

        @Override
        public void discard() {
            valueCountdownMap.values().forEach(completer ->
                                                   completer.object().close());
        }

        @Override
        public Versioned<Collection<? extends byte[]>> addCommit(
                Commit<? extends MultimapCommand> commit) {
            Preconditions.checkNotNull(commit);
            Preconditions.checkNotNull(commit.operation());
            Versioned<Collection<? extends byte[]>> retVersion;

            if (commit.operation() instanceof Put) {
                //Using a treeset here sanitizes the input, removing duplicates
                Set<byte[]> valuesToAdd =
                        Sets.newTreeSet(new ByteArrayComparator());
                ((Put) commit.operation()).values().forEach(value -> {
                    if (!valueCountdownMap.containsKey(value)) {
                        valuesToAdd.add(value);
                    }
                });
                if (valuesToAdd.isEmpty()) {
                    //Do not increment or add the commit if no change resulted
                    commit.close();
                    return null;
                }
                //When all values from a commit have been removed decrement all
                //removal commits relying on it and remove itself from the
                //mapping of additive commits to the commits removing the
                //values it added. (Only multiremoves will be dependent)
                CountDownCompleter<Commit> completer =
                        new CountDownCompleter<>(commit, valuesToAdd.size(),
                        c -> {
                            if (additiveToRemovalCommits.containsKey(c)) {
                                additiveToRemovalCommits.
                                        get(c).
                                        forEach(countdown ->
                                                        countdown.countDown());
                                additiveToRemovalCommits.removeAll(c);
                            }
                            c.close();
                        });
                retVersion = new Versioned<>(valuesToAdd, version);
                valuesToAdd.forEach(value -> valueCountdownMap.put(value,
                                                                   completer));
                version++;
                return retVersion;

            } else if (commit.operation() instanceof Replace) {
                //Will this work??  Need to check before check-in!
                Set<byte[]> removedValues = Sets.newHashSet();
                removedValues.addAll(valueCountdownMap.keySet());
                retVersion = new Versioned<>(removedValues, version);
                valueCountdownMap.values().forEach(countdown ->
                                                   countdown.countDown());
                valueCountdownMap.clear();
                Set<byte[]> valuesToAdd =
                        Sets.newTreeSet(new ByteArrayComparator());
                ((Replace) commit.operation()).values().forEach(value -> {
                    valuesToAdd.add(value);
                });
                if (valuesToAdd.isEmpty()) {
                    version = globalVersion.incrementAndGet();
                    backingMap.remove(((Replace) commit.operation()).key());
                    //Order is important here, the commit must be closed last
                    //(or minimally after all uses)
                    commit.close();
                    return retVersion;
                }
                CountDownCompleter<Commit> completer =
                        new CountDownCompleter<>(commit, valuesToAdd.size(),
                                     c -> {
                                         if (additiveToRemovalCommits
                                             .containsKey(c)) {
                                            additiveToRemovalCommits.
                                                 get(c).
                                                 forEach(countdown ->
                                                     countdown.countDown());
                                             additiveToRemovalCommits.
                                                     removeAll(c);
                                         }
                                         c.close();
                                     });
                valuesToAdd.forEach(value ->
                                    valueCountdownMap.put(value, completer));
                version = globalVersion.incrementAndGet();
                return retVersion;

            } else if (commit.operation() instanceof RemoveAll) {
                Set<byte[]> removed = Sets.newHashSet();
                //We can assume here that values only appear once and so we
                //do not need to sanitize the return for duplicates.
                removed.addAll(valueCountdownMap.keySet());
                retVersion = new Versioned<>(removed, version);
                valueCountdownMap.values().forEach(countdown ->
                                                   countdown.countDown());
                valueCountdownMap.clear();
                //In the case of a removeAll all commits will be removed and
                //unlike the multiRemove case we do not need to consider
                //dependencies among additive and removal commits.

                //Save the key for use after the commit is closed
                String key = ((RemoveAll) commit.operation()).key();
                commit.close();
                version = globalVersion.incrementAndGet();
                backingMap.remove(key);
                return retVersion;

            } else if (commit.operation() instanceof MultiRemove) {
                //Must first calculate how many commits the removal depends on.
                //At this time we also sanitize the removal set by adding to a
                //set with proper handling of byte[] equality.
                Set<byte[]> removed = Sets.newHashSet();
                Set<Commit> commitsRemovedFrom = Sets.newHashSet();
                ((MultiRemove) commit.operation()).values().forEach(value -> {
                    if (valueCountdownMap.containsKey(value)) {
                        removed.add(value);
                        commitsRemovedFrom
                                .add(valueCountdownMap.get(value).object());
                    }
                });
                //If there is nothing to be removed no action should be taken.
                if (removed.isEmpty()) {
                    //Do not increment or add the commit if no change resulted
                    commit.close();
                    return null;
                }
                //When all additive commits this depends on are closed this can
                //be closed as well.
                CountDownCompleter<Commit> completer =
                        new CountDownCompleter<>(commit,
                                                 commitsRemovedFrom.size(),
                                                 c -> c.close());
                commitsRemovedFrom.forEach(commitRemovedFrom -> {
                    additiveToRemovalCommits.put(commitRemovedFrom, completer);
                });
                //Save key in case countdown results in closing the commit.
                String removedKey = ((MultiRemove) commit.operation()).key();
                removed.forEach(removedValue -> {
                    valueCountdownMap.remove(removedValue).countDown();
                });
                //The version is updated locally as well as globally even if
                //this object will be removed from the map in case any other
                //party still holds a reference to this object.
                retVersion = new Versioned<>(removed, version);
                version = globalVersion.incrementAndGet();
                if (valueCountdownMap.isEmpty()) {
                    backingMap
                            .remove(removedKey);
                }
                return retVersion;

            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * A collector that creates MapEntryValues and creates a multiset of all
     * values in the map an equal number of times to the number of sets in
     * which they participate.
     */
    private class HashMultisetValueCollector implements
            Collector<MapEntryValue,
                    HashMultiset<byte[]>,
                    HashMultiset<byte[]>> {
        private HashMultiset<byte[]> multiset = null;

        @Override
        public Supplier<HashMultiset<byte[]>> supplier() {
            return () -> {
                if (multiset == null) {
                    multiset = HashMultiset.create();
                }
                return multiset;
            };
        }

        @Override
        public BiConsumer<HashMultiset<byte[]>, MapEntryValue> accumulator() {
            return (multiset, mapEntryValue) ->
                    multiset.addAll(mapEntryValue.values());
        }

        @Override
        public BinaryOperator<HashMultiset<byte[]>> combiner() {
            return (setOne, setTwo) -> {
                setOne.addAll(setTwo);
                return setOne;
            };
        }

        @Override
        public Function<HashMultiset<byte[]>,
                HashMultiset<byte[]>> finisher() {
            return (unused) -> multiset;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.UNORDERED);
        }
    }

    /**
     * A collector that creates Entries of {@code <String, MapEntryValue>} and
     * creates a set of entries all key value pairs in the map.
     */
    private class EntrySetCollector implements
            Collector<Map.Entry<String, MapEntryValue>,
                    Set<Map.Entry<String, byte[]>>,
                    Set<Map.Entry<String, byte[]>>> {
        private Set<Map.Entry<String, byte[]>> set = null;

        @Override
        public Supplier<Set<Map.Entry<String, byte[]>>> supplier() {
            return () -> {
                if (set == null) {
                    set = Sets.newHashSet();
                }
                return set;
            };
        }

        @Override
        public BiConsumer<Set<Map.Entry<String, byte[]>>,
                Map.Entry<String, MapEntryValue>> accumulator() {
            return (set, entry) -> {
                entry
                    .getValue()
                    .values()
                    .forEach(byteValue ->
                             set.add(Maps.immutableEntry(entry.getKey(),
                                                         byteValue)));
            };
        }

        @Override
        public BinaryOperator<Set<Map.Entry<String, byte[]>>> combiner() {
            return (setOne, setTwo) -> {
                setOne.addAll(setTwo);
                return setOne;
            };
        }

        @Override
        public Function<Set<Map.Entry<String, byte[]>>,
                Set<Map.Entry<String, byte[]>>> finisher() {
            return (unused) -> set;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.UNORDERED);
        }
    }
    /**
     * Utility for turning a {@code MapEntryValue} to {@code Versioned}.
     * @param value map entry value
     * @return versioned instance or an empty list versioned -1 if argument is
     * null
     */
    private Versioned<Collection<? extends byte[]>> toVersioned(
            MapEntryValue value) {
        return value == null ? new Versioned<>(Lists.newArrayList(), -1) :
                new Versioned<>(value.values(),
                                value.version());
    }

    private class ByteArrayComparator implements Comparator<byte[]> {

        @Override
        public int compare(byte[] o1, byte[] o2) {
            if (Arrays.equals(o1, o2)) {
                return 0;
            } else {
                for (int i = 0; i < o1.length && i < o2.length; i++) {
                    if (o1[i] < o2[i]) {
                        return -1;
                    } else if (o1[i] > o2[i]) {
                        return 1;
                    }
                }
                return o1.length > o2.length ? 1 : -1;
            }
        }
    }
 }
