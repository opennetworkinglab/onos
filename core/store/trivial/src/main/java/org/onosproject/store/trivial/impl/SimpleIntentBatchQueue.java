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
package org.onosproject.store.trivial.impl;

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentBatchListener;
import org.onosproject.net.intent.IntentBatchService;
import org.onosproject.net.intent.IntentOperations;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
public class SimpleIntentBatchQueue implements IntentBatchService {

    private final Logger log = getLogger(getClass());
    private final Queue<IntentOperations> pendingBatches = new LinkedList<>();
    private final Set<IntentOperations> currentBatches = Sets.newHashSet();
    private IntentBatchDelegate delegate;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addIntentOperations(IntentOperations operations) {
        checkState(delegate != null, "No delegate set");
        synchronized (this) {
            pendingBatches.add(operations);
            if (currentBatches.isEmpty()) {
                IntentOperations work = pendingBatches.poll();
                currentBatches.add(work);
                delegate.execute(work);
            }
        }
    }

    @Override
    public void removeIntentOperations(IntentOperations operations) {
        // we allow at most one outstanding batch at a time
        synchronized (this) {
            checkState(currentBatches.remove(operations), "Operations not found in current ops.");
            checkState(currentBatches.isEmpty(), "More than one outstanding batch.");
            IntentOperations work = pendingBatches.poll();
            if (work != null) {
                currentBatches.add(work);
                delegate.execute(work);
            }
        }
    }

    @Override
    public Set<IntentOperations> getPendingOperations() {
        synchronized (this) {
            Set<IntentOperations> set = Sets.newHashSet(pendingBatches);
            set.addAll(currentBatches); // TODO refactor this current vs. pending
            return set;
        }
    }

    @Override
    public boolean isLocalLeader(ApplicationId applicationId) {
        return true;
    }

    @Override
    public void setDelegate(IntentBatchDelegate delegate) {
        this.delegate = checkNotNull(delegate, "Delegate cannot be null");
    }

    @Override
    public void unsetDelegate(IntentBatchDelegate delegate) {
        if (this.delegate != null && this.delegate.equals(delegate)) {
            this.delegate = null;
        }
    }

    @Override
    public void addListener(IntentBatchListener listener) {
        // no-op
        //TODO: we are always the master
    }

    @Override
    public void removeListener(IntentBatchListener listener) {
        // no-op
        //TODO: we are always the master
    }
}
