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
package org.onlab.onos.store.intent.impl;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Verify;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.metrics.MetricsService;
import org.onlab.onos.core.MetricsHelper;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.onlab.onos.net.intent.IntentStore.BatchWrite.Operation;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.serializers.KryoNamespaces;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.onos.store.serializers.StoreSerializer;
import org.onlab.onos.store.service.BatchWriteRequest;
import org.onlab.onos.store.service.BatchWriteRequest.Builder;
import org.onlab.onos.store.service.BatchWriteResult;
import org.onlab.onos.store.service.DatabaseAdminService;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.impl.CMap;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.onlab.onos.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.metrics.MetricsUtil.*;

@Component(immediate = true, enabled = true)
@Service
public class DistributedIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore, MetricsHelper {

    /** Valid parking state, which can transition to INSTALLED. */
    private static final Set<IntentState> PRE_INSTALLED = EnumSet.of(SUBMITTED, INSTALLED, FAILED);

    /** Valid parking state, which can transition to WITHDRAWN. */
    private static final Set<IntentState> PRE_WITHDRAWN = EnumSet.of(INSTALLED, FAILED);

    private final Logger log = getLogger(getClass());

    // Assumption: IntentId will not have synonyms
    private static final String INTENTS_TABLE = "intents";
    private CMap<IntentId, Intent> intents;

    private static final String STATES_TABLE = "intent-states";
    private CMap<IntentId, IntentState> states;

    // TODO left behind transient state issue: ONOS-103
    // Map to store instance local intermediate state transition
    private transient Map<IntentId, IntentState> transientStates = new ConcurrentHashMap<>();

    private static final String INSTALLABLE_TABLE = "installable-intents";
    private CMap<IntentId, List<Intent>> installable;

    private LoadingCache<IntentId, String> keyCache;

    private StoreSerializer serializer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseAdminService dbAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseService dbService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    // TODO make this configurable
    private boolean onlyLogTransitionError = true;

    private Timer createIntentTimer;
    private Timer removeIntentTimer;
    private Timer setInstallableIntentsTimer;
    private Timer getInstallableIntentsTimer;
    private Timer removeInstalledIntentsTimer;
    private Timer setStateTimer;
    private Timer getIntentCountTimer;
    private Timer getIntentsTimer;
    private Timer getIntentTimer;
    private Timer getIntentStateTimer;


    private Timer createResponseTimer(String methodName) {
        return createTimer("IntentStore", methodName, "responseTime");
    }

    @Activate
    public void activate() {
        createIntentTimer = createResponseTimer("createIntent");
        removeIntentTimer = createResponseTimer("removeIntent");
        setInstallableIntentsTimer = createResponseTimer("setInstallableIntents");
        getInstallableIntentsTimer = createResponseTimer("getInstallableIntents");
        removeInstalledIntentsTimer = createResponseTimer("removeInstalledIntents");
        setStateTimer = createResponseTimer("setState");
        getIntentCountTimer = createResponseTimer("getIntentCount");
        getIntentsTimer = createResponseTimer("getIntents");
        getIntentTimer = createResponseTimer("getIntent");
        getIntentStateTimer = createResponseTimer("getIntentState");

        // FIXME: We need a way to add serializer for intents which has been plugged-in.
        // As a short term workaround, relax Kryo config to
        // registrationRequired=false
        serializer = new KryoSerializer() {

            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .setRegistrationRequired(false)
                        .register(KryoNamespaces.API)
                        .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                        .build();
            }
        };

        keyCache = CacheBuilder.newBuilder()
                .softValues()
                .build(new CacheLoader<IntentId, String>() {

                    @Override
                    public String load(IntentId key) {
                        return key.toString();
                    }
                });

        intents = new IntentIdMap<>(dbAdminService, dbService, INTENTS_TABLE, serializer);

        states = new IntentIdMap<>(dbAdminService, dbService, STATES_TABLE, serializer);

        transientStates.clear();

        installable = new IntentIdMap<>(dbAdminService, dbService, INSTALLABLE_TABLE, serializer);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public MetricsService metricsService() {
        return metricsService;
    }

    @Override
    public IntentEvent createIntent(Intent intent) {
        Context timer = startTimer(createIntentTimer);
        try {
            boolean absent = intents.putIfAbsent(intent.id(), intent);
            if (!absent) {
                // duplicate, ignore
                return null;
            } else {
                return this.setState(intent, IntentState.SUBMITTED);
            }
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public void removeIntent(IntentId intentId) {
        Context timer = startTimer(removeIntentTimer);
        checkState(getIntentState(intentId) == WITHDRAWN,
                   "Intent state for {} is not WITHDRAWN.", intentId);
        try {
            intents.remove(intentId);
            states.remove(intentId);
            transientStates.remove(intentId);
            installable.remove(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public long getIntentCount() {
        Context timer = startTimer(getIntentCountTimer);
        try {
            return intents.size();
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public Iterable<Intent> getIntents() {
        Context timer = startTimer(getIntentsTimer);
        try {
            return ImmutableSet.copyOf(intents.values());
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public Intent getIntent(IntentId intentId) {
        Context timer = startTimer(getIntentTimer);
        try {
            return intents.get(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        Context timer = startTimer(getIntentStateTimer);
        try {
            final IntentState localState = transientStates.get(id);
            if (localState != null) {
                return localState;
            }
            return states.get(id);
        } finally {
            stopTimer(timer);
        }
    }

    // FIXME temporary workaround until we fix our state machine
    private void verify(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (onlyLogTransitionError) {
            if (!expression) {
                log.error(errorMessageTemplate.replace("%s", "{}"), errorMessageArgs);
            }
        } else {
            Verify.verify(expression, errorMessageTemplate, errorMessageArgs);
        }
    }

    @Override
    public IntentEvent setState(Intent intent, IntentState state) {
        Context timer = startTimer(setStateTimer);
        try {
            final IntentId id = intent.id();
            IntentEvent.Type evtType = null;
            final IntentState prevParking;
            boolean transitionedToParking = true;
            boolean updated;

            // parking state transition
            switch (state) {
            case SUBMITTED:
                prevParking = states.get(id);
                if (prevParking == null) {
                    updated = states.putIfAbsent(id, SUBMITTED);
                    verify(updated, "Conditional replace %s => %s failed", prevParking, SUBMITTED);
                } else {
                    verify(prevParking == WITHDRAWN,
                            "Illegal state transition attempted from %s to SUBMITTED",
                            prevParking);
                    updated = states.replace(id, prevParking, SUBMITTED);
                    verify(updated, "Conditional replace %s => %s failed", prevParking, SUBMITTED);
                }
                evtType = IntentEvent.Type.SUBMITTED;
                break;

            case INSTALLED:
                prevParking = states.get(id);
                verify(PRE_INSTALLED.contains(prevParking),
                       "Illegal state transition attempted from %s to INSTALLED",
                       prevParking);
                updated = states.replace(id, prevParking, INSTALLED);
                verify(updated, "Conditional replace %s => %s failed", prevParking, INSTALLED);
                evtType = IntentEvent.Type.INSTALLED;
                break;

            case FAILED:
                prevParking = states.get(id);
                updated = states.replace(id, prevParking, FAILED);
                verify(updated, "Conditional replace %s => %s failed", prevParking, FAILED);
                evtType = IntentEvent.Type.FAILED;
                break;

            case WITHDRAWN:
                prevParking = states.get(id);
                verify(PRE_WITHDRAWN.contains(prevParking),
                       "Illegal state transition attempted from %s to WITHDRAWN",
                       prevParking);
                updated = states.replace(id, prevParking, WITHDRAWN);
                verify(updated, "Conditional replace %s => %s failed", prevParking, WITHDRAWN);
                evtType = IntentEvent.Type.WITHDRAWN;
                break;

            default:
                transitionedToParking = false;
                prevParking = null;
                break;
            }
            if (transitionedToParking) {
                log.debug("Parking State change: {} {}=>{}",  id, prevParking, state);
                // remove instance local state
                transientStates.remove(id);
            } else {
                // Update instance local state, which includes non-parking state transition
                final IntentState prevTransient = transientStates.put(id, state);
                log.debug("Transient State change: {} {}=>{}", id, prevTransient, state);
            }

            if (evtType == null) {
                return null;
            }
            return new IntentEvent(evtType, intent);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public void setInstallableIntents(IntentId intentId, List<Intent> result) {
        Context timer = startTimer(setInstallableIntentsTimer);
        try {
            installable.put(intentId, result);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        Context timer = startTimer(getInstallableIntentsTimer);
        try {
            return installable.get(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public void removeInstalledIntents(IntentId intentId) {
        Context timer = startTimer(removeInstalledIntentsTimer);
        try {
            installable.remove(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    protected String strIntentId(IntentId key) {
        return keyCache.getUnchecked(key);
    }

    /**
     * Distributed Map from IntentId to some value.
     *
     * @param <V> Map value type
     */
    final class IntentIdMap<V> extends CMap<IntentId, V> {

        /**
         * Creates a IntentIdMap instance.
         *
         * @param dbAdminService DatabaseAdminService to use for this instance
         * @param dbService DatabaseService to use for this instance
         * @param tableName table which this Map corresponds to
         * @param serializer Value serializer
         */
        public IntentIdMap(DatabaseAdminService dbAdminService,
                         DatabaseService dbService,
                         String tableName,
                         StoreSerializer serializer) {
            super(dbAdminService, dbService, tableName, serializer);
        }

        @Override
        protected String sK(IntentId key) {
            return strIntentId(key);
        }
    }

    @Override
    public List<Operation> batchWrite(BatchWrite batch) {

        List<Operation> failed = new ArrayList<>();
        final Builder builder = BatchWriteRequest.newBuilder();

        for (Operation op : batch.operations()) {
            switch (op.type()) {
            case CREATE_INTENT:
                checkArgument(op.args().size() == 1,
                              "CREATE_INTENT takes 1 argument. %s", op);
                Intent intent = op.arg(0);
                builder.putIfAbsent(INTENTS_TABLE, strIntentId(intent.id()), serializer.encode(intent));
                builder.putIfAbsent(STATES_TABLE, strIntentId(intent.id()), serializer.encode(SUBMITTED));
                break;

            case REMOVE_INTENT:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INTENT takes 1 argument. %s", op);
                IntentId intentId = (IntentId) op.arg(0);
                builder.remove(INTENTS_TABLE, strIntentId(intentId));
                builder.remove(STATES_TABLE, strIntentId(intentId));
                builder.remove(INSTALLABLE_TABLE, strIntentId(intentId));
                break;

            case SET_STATE:
                checkArgument(op.args().size() == 2,
                              "SET_STATE takes 2 arguments. %s", op);
                intent = op.arg(0);
                IntentState newState = op.arg(1);
                builder.put(STATES_TABLE, strIntentId(intent.id()), serializer.encode(newState));
                break;

            case SET_INSTALLABLE:
                checkArgument(op.args().size() == 2,
                              "SET_INSTALLABLE takes 2 arguments. %s", op);
                intentId = op.arg(0);
                List<Intent> installableIntents = op.arg(1);
                builder.put(INSTALLABLE_TABLE, strIntentId(intentId), serializer.encode(installableIntents));
                break;

            case REMOVE_INSTALLED:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INSTALLED takes 1 argument. %s", op);
                intentId = op.arg(0);
                builder.remove(INSTALLABLE_TABLE, strIntentId(intentId));
                break;

            default:
                log.warn("Unknown Operation encountered: {}", op);
                failed.add(op);
                break;
            }
        }

        BatchWriteResult batchWriteResult = dbService.batchWrite(builder.build());
        if (batchWriteResult.isSuccessful()) {
            // no-failure (except for invalid input)
            return failed;
        } else {
            // everything failed
            return batch.operations();
        }
    }
}
