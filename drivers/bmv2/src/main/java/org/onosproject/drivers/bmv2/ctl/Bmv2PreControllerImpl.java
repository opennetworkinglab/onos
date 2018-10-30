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
 *
 */

package org.onosproject.drivers.bmv2.ctl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.onlab.util.Tools;
import org.onosproject.bmv2.thriftapi.SimplePreLAG;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.drivers.bmv2.api.Bmv2DeviceAgent;
import org.onosproject.drivers.bmv2.api.Bmv2PreController;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onosproject.drivers.bmv2.ctl.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * BMv2 PRE controller implementation.
 */
@Component(immediate = true, service = Bmv2PreController.class,
        property = {
                 NUM_CONNECTION_RETRIES + ":Integer=" + NUM_CONNECTION_RETRIES_DEFAULT,
                TIME_BETWEEN_RETRIES + ":Integer=" + TIME_BETWEEN_RETRIES_DEFAULT,
                DEVICE_LOCK_WAITING_TIME_IN_SEC + ":Integer=" + DEVICE_LOCK_WAITING_TIME_IN_SEC_DEFAULT,
        })
public class Bmv2PreControllerImpl implements Bmv2PreController {

    private static final int DEVICE_LOCK_CACHE_EXPIRE_TIME_IN_MIN = 10;
    private static final String THRIFT_SERVICE_NAME = "simple_pre_lag";
    private final Logger log = getLogger(getClass());
    private final Map<DeviceId, Pair<TTransport, Bmv2DeviceThriftClient>> clients = Maps.newHashMap();
    //TODO consider a timeout mechanism for locks to limit the retention time
    private final LoadingCache<DeviceId, ReadWriteLock> deviceLocks = CacheBuilder.newBuilder()
            .expireAfterAccess(DEVICE_LOCK_CACHE_EXPIRE_TIME_IN_MIN, TimeUnit.MINUTES)
            .build(new CacheLoader<DeviceId, ReadWriteLock>() {
                @Override
                public ReadWriteLock load(DeviceId deviceId) {
                    return new ReentrantReadWriteLock();
                }
            });
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /** Number of connection retries after a network error. */
    private int numConnectionRetries = NUM_CONNECTION_RETRIES_DEFAULT;

    /** Time between retries in milliseconds. */
    private int timeBetweenRetries = TIME_BETWEEN_RETRIES_DEFAULT;

    /** Waiting time for a read/write lock in seconds. */
    private int deviceLockWaitingTime = DEVICE_LOCK_WAITING_TIME_IN_SEC_DEFAULT;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Integer newNumConnRetries = Tools.getIntegerProperty(properties, "numConnectionRetries");
        if (newNumConnRetries != null && newNumConnRetries >= 0) {
            numConnectionRetries = newNumConnRetries;
        } else {
            log.warn("numConnectionRetries must be equal to or greater than 0");
        }

        Integer newTimeBtwRetries = Tools.getIntegerProperty(properties, "timeBetweenRetries");
        if (newTimeBtwRetries != null && newTimeBtwRetries >= 0) {
            timeBetweenRetries = newTimeBtwRetries;
        } else {
            log.warn("timeBetweenRetries must be equal to or greater than 0");
        }

        Integer newDeviceLockWaitingTime = Tools.getIntegerProperty(properties, "deviceLockWaitingTime");
        if (newDeviceLockWaitingTime != null && newDeviceLockWaitingTime >= 0) {
            deviceLockWaitingTime = newDeviceLockWaitingTime;
        } else {
            log.warn("deviceLockWaitingTime must be equal to or greater than 0");
        }
    }

    @Override
    public boolean createPreClient(DeviceId deviceId, String thriftServerIp, Integer thriftServerPort) {
        checkNotNull(deviceId);
        checkNotNull(thriftServerIp);
        checkNotNull(thriftServerPort);

        if (!acquireWriteLock(deviceId)) {
            //reason is already logged during the lock acquisition
            return false;
        }

        log.info("Creating PRE client for {} through Thrift server {}:{}", deviceId, thriftServerIp, thriftServerPort);

        try {
            if (clients.containsKey(deviceId)) {
                throw new IllegalStateException(format("A client already exists for %s", deviceId));
            } else {
                return doCreateClient(deviceId, thriftServerIp, thriftServerPort);
            }
        } finally {
            releaseWriteLock(deviceId);
        }
    }

    @Override
    public Bmv2DeviceAgent getPreClient(DeviceId deviceId) {
        if (!acquireReadLock(deviceId)) {
            return null;
        }
        try {
            return clients.containsKey(deviceId) ? clients.get(deviceId).getRight() : null;
        } finally {
            releaseReadLock(deviceId);
        }
    }

    @Override
    public void removePreClient(DeviceId deviceId) {
        if (!acquireWriteLock(deviceId)) {
            //reason is already logged during the lock acquisition
            return;
        }

        try {
            if (clients.containsKey(deviceId)) {
                TTransport transport = clients.get(deviceId).getLeft();
                if (transport.isOpen()) {
                    transport.close();
                }
                clients.remove(deviceId);
            }
        } finally {
            releaseWriteLock(deviceId);
        }
    }

    private boolean acquireWriteLock(DeviceId deviceId) {
        try {
            return deviceLocks.getUnchecked(deviceId).writeLock().tryLock(deviceLockWaitingTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Unable to acquire write lock for device {} due to {}", deviceId, e.toString());
        }
        return false;
    }

    private boolean acquireReadLock(DeviceId deviceId) {
        try {
            return deviceLocks.getUnchecked(deviceId).readLock().tryLock(deviceLockWaitingTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Unable to acquire read lock for device {} due to {}", deviceId, e.toString());
        }
        return false;
    }

    private void releaseWriteLock(DeviceId deviceId) {
        deviceLocks.getUnchecked(deviceId).writeLock().unlock();
    }

    private void releaseReadLock(DeviceId deviceId) {
        deviceLocks.getUnchecked(deviceId).readLock().unlock();
    }

    private boolean doCreateClient(DeviceId deviceId, String thriftServerIp, Integer thriftServerPort) {
        SafeThriftClient.Options options = new SafeThriftClient.Options(numConnectionRetries, timeBetweenRetries);

        try {
            // Make the expensive call
            TTransport transport = new TSocket(thriftServerIp, thriftServerPort);

            TProtocol protocol = new TBinaryProtocol(transport);
            // Create a client for simple_pre service.
            SimplePreLAG.Client simplePreClient = new SimplePreLAG.Client(
                    new TMultiplexedProtocol(protocol, THRIFT_SERVICE_NAME));

            SimplePreLAG.Iface safeSimplePreClient = SafeThriftClient.wrap(simplePreClient,
                                                                           SimplePreLAG.Iface.class,
                                                                           options);

            Bmv2DeviceThriftClient client = new Bmv2DeviceThriftClient(deviceId, safeSimplePreClient);
            clients.put(deviceId, Pair.of(transport, client));
            return true;

        } catch (RuntimeException e) {
            log.warn("Failed to create Thrift client for BMv2 device. deviceId={}, cause={}", deviceId, e);
            return false;
        }
    }
}