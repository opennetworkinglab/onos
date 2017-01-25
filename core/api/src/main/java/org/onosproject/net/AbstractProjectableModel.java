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
package org.onosproject.net;

import com.google.common.annotations.Beta;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.driver.Projectable;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base model entity, capable of being extended via projection mechanism.
 */
@Beta
public abstract class AbstractProjectableModel extends AbstractModel implements Projectable {

    private static Logger log = LoggerFactory.getLogger(AbstractProjectableModel.class);

    protected static final String NO_DRIVER_SERVICE = "Driver service not bound yet";
    protected static final String NO_DRIVER = "Driver has not been bound to %s";

    // Static reference to the driver service; injected via setDriverService
    private static DriverService driverService;

    private transient Driver driver;

    // For serialization
    public AbstractProjectableModel() {
    }

    /**
     * Creates a model entity attributed to the specified provider and
     * optionally annotated.
     *
     * @param providerId  identity of the provider
     * @param annotations optional key/value annotations
     */
    public AbstractProjectableModel(ProviderId providerId, Annotations[] annotations) {
        super(providerId, annotations);
    }

    /**
     * Injects the driver service reference for use during projections into
     * various behaviours.
     * <p>
     * This is a privileged call; unauthorized invocations will result in
     * illegal state exception
     *
     * @param key           opaque admin key object
     * @param driverService injected driver service
     * @throws IllegalStateException when invoked sans authorization
     */
    public static void setDriverService(Object key, DriverService driverService) {
        // TODO: Rework this once we have means to enforce access to admin services in general
//        checkState(AbstractProjectableModel.driverService == key, "Unauthorized invocation");
        AbstractProjectableModel.driverService = driverService;
    }

    /**
     * Returns the currently bound driver service reference.
     *
     * @return driver service
     */
    protected static DriverService driverService() {
        return driverService;
    }

    /**
     * Returns the currently bound driver or null if no driver is bound.
     *
     * @return bound driver; null if none
     */
    public Driver driver() {
        return driver;
    }

    @Override
    public <B extends Behaviour> B as(Class<B> projectionClass) {
        bindAndCheckDriver();
        return driver.createBehaviour(asData(), projectionClass);
    }

    @Override
    public <B extends Behaviour> boolean is(Class<B> projectionClass) {
        bindDriver();
        return driver != null && driver.hasBehaviour(projectionClass);
    }

    /**
     * Locates the driver to be used by this entity.
     * <p>
     * The default implementation derives the driver based on the {@code driver}
     * annotation value.
     *
     * @return driver for alternate projections of this model entity or null
     * if no driver is expected or driver is not found
     */
    protected Driver locateDriver() {
        Annotations annotations = annotations();
        String driverName = annotations != null ? annotations.value(AnnotationKeys.DRIVER) : null;
        if (driverName != null) {
            try {
                return driverService.getDriver(driverName);
            } catch (ItemNotFoundException e) {
                log.warn("Driver {} not found.", driverName);
            }
        }
        return null;
    }

    /**
     * Attempts to binds the driver, if not already bound.
     */
    protected final void bindDriver() {
        checkState(driverService != null, NO_DRIVER_SERVICE);
        if (driver == null) {
            driver = locateDriver();
        }
    }

    /**
     * Attempts to bind the driver, if not already bound and checks that the
     * driver is bound.
     *
     * @throws IllegalStateException if driver cannot be bound
     */
    protected final void bindAndCheckDriver() {
        bindDriver();
        checkState(driver != null, NO_DRIVER, this);
    }

    /**
     * Returns self as an immutable driver data instance.
     *
     * @return self as driver data
     */
    protected DriverData asData() {
        return new AnnotationDriverData();
    }


    /**
     * Projection of the parent entity as a driver data entity.
     */
    protected class AnnotationDriverData implements DriverData {
        @Override
        public Driver driver() {
            return driver;
        }

        @Override
        public DeviceId deviceId() {
            throw new UnsupportedOperationException("Entity not a device");
        }

        @Override
        public MutableAnnotations set(String key, String value) {
            throw new UnsupportedOperationException("Entity is immutable");
        }

        @Override
        public MutableAnnotations clear(String... keys) {
            throw new UnsupportedOperationException("Entity is immutable");
        }

        @Override
        public Set<String> keys() {
            return annotations().keys();
        }

        @Override
        public String value(String key) {
            return annotations().value(key);
        }
    }

}
