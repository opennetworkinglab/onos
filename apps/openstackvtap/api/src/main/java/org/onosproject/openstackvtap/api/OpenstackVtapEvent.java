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
package org.onosproject.openstackvtap.api;

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;

/**
 * Describes openstack vtap event.
 */
public class OpenstackVtapEvent extends AbstractEvent<OpenstackVtapEvent.Type, Object> {

    private static final String INVALID_OBJ_TYPE = "Invalid OpenstackVtapEvent object type of %";

    /**
     * Type of openstack vtap events.
     */
    public enum Type {

        /**
         * Signifies that a new openstack vtap network has been added.
         */
        VTAP_NETWORK_ADDED,

        /**
         * Signifies that a openstack vtap network has been changed.
         */
        VTAP_NETWORK_UPDATED,

        /**
         * Signifies that a openstack vtap network has been removed.
         */
        VTAP_NETWORK_REMOVED,

        /**
         * Signifies that a new openstack vtap has been added.
         */
        VTAP_ADDED,

        /**
         * Signifies that a openstack vtap has been changed.
         */
        VTAP_UPDATED,

        /**
         * Signifies that a openstack vtap has been removed.
         */
        VTAP_REMOVED,
    }

    private final Object prevSubject;

    /**
     * Creates an event with previous openstack vtap network subject.
     *
     * The openstack vtap network subject is null if the type is removed
     * The previous openstack vtap network subject is null if the type is added
     *
     * @param type            openstack vtap event type
     * @param vtapNetwork     event openstack vtap network subject
     * @param prevVtapNetwork previous openstack vtap network subject
     */
    public OpenstackVtapEvent(Type type,
                              OpenstackVtapNetwork vtapNetwork,
                              OpenstackVtapNetwork prevVtapNetwork) {
        super(type, vtapNetwork);
        prevSubject = prevVtapNetwork;
    }

    /**
     * Creates an event of a given type and for the specified openstack vtap network and time.
     *
     * @param type            openstack vtap event type
     * @param vtapNetwork     event openstack vtap network subject
     * @param prevVtapNetwork previous openstack vtap network subject
     * @param time            occurrence time
     */
    public OpenstackVtapEvent(Type type,
                              OpenstackVtapNetwork vtapNetwork,
                              OpenstackVtapNetwork prevVtapNetwork,
                              long time) {
        super(type, vtapNetwork, time);
        prevSubject = prevVtapNetwork;
    }

    /**
     * Creates an event with previous openstack vtap subject.
     *
     * The openstack vtap subject is null if the type is removed
     * The previous openstack vtap subject is null if the type is added
     *
     * @param type     openstack vtap event type
     * @param vtap     event openstack vtap subject
     * @param prevVtap previous openstack vtap subject
     */
    public OpenstackVtapEvent(Type type, OpenstackVtap vtap, OpenstackVtap prevVtap) {
        super(type, vtap);
        prevSubject = prevVtap;
    }

    /**
     * Creates an event of a given type and for the specified openstack vtap and time.
     *
     * @param type     openstack vtap event type
     * @param vtap     event openstack vtap subject
     * @param prevVtap previous openstack vtap subject
     * @param time     occurrence time
     */
    public OpenstackVtapEvent(Type type, OpenstackVtap vtap, OpenstackVtap prevVtap, long time) {
        super(type, vtap, time);
        prevSubject = prevVtap;
    }

    /**
     * Gets the previous subject in this openstack vtap event.
     *
     * @return the previous subject, or null if previous subject is not
     *         specified.
     */
    public Object prevSubject() {
        return prevSubject;
    }

    /**
     * Gets the openstack vtap network in this openstack vtap event.
     *
     * @return the subject, or null if the type is removed
     */
    public OpenstackVtapNetwork openstackVtapNetwork() {
        Object obj = subject();
        checkState(obj == null || obj instanceof OpenstackVtapNetwork, INVALID_OBJ_TYPE, obj);
        return (OpenstackVtapNetwork) obj;
    }

    /**
     * Gets the previous openstack vtap network in this openstack vtap event.
     *
     * @return the previous subject, or null if type is added
     */
    public OpenstackVtapNetwork prevOpenstackVtapNetwork() {
        Object obj = prevSubject;
        checkState(obj == null || obj instanceof OpenstackVtapNetwork, INVALID_OBJ_TYPE, obj);
        return (OpenstackVtapNetwork) obj;
    }

    /**
     * Gets the openstack vtap in this openstack vtap event.
     *
     * @return the subject, or null if the type is removed
     */
    public OpenstackVtap openstackVtap() {
        Object obj = subject();
        checkState(obj == null || obj instanceof OpenstackVtap, INVALID_OBJ_TYPE, obj);
        return (OpenstackVtap) obj;
    }

    /**
     * Gets the previous openstack vtap in this openstack vtap event.
     *
     * @return the previous subject, or null if type is added
     */
    public OpenstackVtap prevOpenstackVtap() {
        Object obj = prevSubject;
        checkState(obj == null || obj instanceof OpenstackVtap, INVALID_OBJ_TYPE, obj);
        return (OpenstackVtap) obj;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("prevSubject", prevSubject())
                .toString();
    }
}
