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
package org.onosproject.alarm;

import org.onosproject.net.DeviceId;

/**
 * Representation of an Alarm. At a given instant there can be only one alarm
 * with the same deviceId + description + source combination.
 */
public interface Alarm {

    /**
     * Returns the unique alarm id within this ONOS instance.
     *
     * @return alarm identifier
     */
    AlarmId id();

    /**
     * The device to which this alarm is related.
     *
     * @return a device id
     */
    DeviceId deviceId();

    /**
     * Returns a description of alarm.
     * <p>
     * It may encapsulate Event Type as described by ITU Recommendation X.736
     * ITU, Quoting https://tools.ietf.org/html/rfc3877 these include: other,
     * communicationsAlarm, qualityOfServiceAlarm, processingErrorAlarm,
     * equipmentAlarm, environmentalAlarm, integrityViolation,
     * operationalViolation, physicalViolation,
     * securityServiceOrMechanismViolation, timeDomainViolation
     * <p>
     * It may encapsulate Probable Cause as described by ITU Recommendation
     * X.736 ITU, Quoting
     * https://www.iana.org/assignments/ianaitualarmtc-mib/ianaitualarmtc-mib
     * these include : aIS, callSetUpFailure, degradedSignal,
     * farEndReceiverFailure, framingError, and hundreds more constants.
     * <p>
     * It may encapsulate a vendor-specific description of the underlying fault.
     *
     * @return description of alarm
     */
    String description();

    /**
     * Returns an entity within the context of this alarm's device. It may be
     * null if deviceId sufficiently identifies the location. As an example, the
     * source may indicate a port number
     *
     * @return source of alarm within the alarm's referenced Device.
     */
    AlarmEntityId source();

    /**
     * Returns the time when raised.
     *
     * @return time when raised, in milliseconds since start of epoch
     */
    long timeRaised();

    /**
     * Returns time at which the alarm was updated most recently, due to some
     * change in the device, or ONOS. If the alarm has been cleared, this is the
     * time at which the alarm was cleared.
     *
     * @return time when last updated, in milliseconds since start of epoch
     */
    long timeUpdated();

    /**
     * Returns the time when cleared. Null indicated no clear time, i.e. the
     * alarm is still active.
     *
     * @return time when cleared, in milliseconds since start of epoch or null
     * if uncleared.
     */
    Long timeCleared();

    /**
     * Returns the severity. Note, that cleared alarms may have EITHER
     * SeverityLevel = CLEARED, or may be not present; both scenarios should be
     * handled.
     *
     * @return severity of the alarm
     */
    SeverityLevel severity();

    /**
     * Returns true if alarm is service affecting Note: Whilst X.733 combines
     * service-affecting state with severity (where severities of critical and
     * major are deemed service-affecting) ONOS keeps these attributes separate.
     *
     * @return whether service affecting (true indicates it is)
     */
    boolean serviceAffecting();

    /**
     * Returns a flag to indicate if this alarm has been acknowledged. All
     * alarms are unacknowledged until and unless an ONOS user takes action to
     * indicate so.
     *
     * @return whether alarm is currently acknowledged (true indicates it is)
     */
    boolean acknowledged();

    /**
     * Returns a flag to indicate if this alarm has been cleared. All
     * alarms are not cleared until and unless an ONOS user or app takes action to
     * indicate so.
     *
     * @return whether alarm is currently cleared (true indicates it is)
     */
    default boolean cleared() {
        return false;
    }

    /**
     * Returns a flag to indicate if this alarm is manually-cleared by a user action within ONOS. Some stateless events
     * e.g. backup-failure or upgrade-failure, may be mapped by ONOS to alarms, and these may be deemed manually-
     * clearable. The more typical case is that an alarm represents a persistent fault on or related to a device and
     * such alarms are never manually clearable, i.e. a configuration or operational state must occur for the alarm to
     * clear.
     *
     * @return whether it may be cleared by a user action (true indicates it is)
     */
    boolean manuallyClearable();

    /**
     * Returns the user to whom this alarm is assigned; this is for future use
     * and always returns null in this release. It is anticipated that in future ONOS
     * releases, the existing JAAS user/key/role configuration will be extended
     * to include a mechanism whereby some groups of users may allocate alarms
     * to other users for bookkeeping and administrative purposes, and that ONOS
     * will additionally provide a REST based mechanism, to retrieve from JAAS,
     * the set of users to whom alarm assignment is possible for the current
     * user.
     *
     * @return the assigned user; always null in this release.
     */
    String assignedUser();

    /**
     * Represents the severity level on an alarm, as per ITU-T X.733
     * specifications.
     * <p>
     * The precedence is as follows for : Critical &gt; Major &gt; Minor &gt; Warning.
     */
    enum SeverityLevel {

        /**
         * From X.733: This indicates the clearing of one or more previously
         * reported alarms. This alarm clears all alarms for this managed object
         * that have the same Alarm type, Probable cause and Specific problems
         * (if given). Multiple associated notifications may be cleared by using
         * the Correlated notifications parameter (defined below). This
         * Recommendation | International Standard does not require that the
         * clearing of previously reported alarms be reported. Therefore, a
         * managing system cannot assume that the absence of an alarm with the
         * Cleared severity level means that the condition that caused the
         * generation of previous alarms is still present. Managed object
         * definers shall state if, and under which conditions, the Cleared
         * severity level is used.
         */
        CLEARED,
        /**
         * From X.733: This indicates that the severity level cannot be
         * determined.
         */
        INDETERMINATE,
        /**
         * From X.733: This indicates that a service affecting condition has
         * occurred and an immediate corrective action is required. Such a
         * severity can be reported, for example, when a managed object becomes
         * totally out of service and its capability must be restored.
         */
        CRITICAL,
        /**
         * X.733 definition: This indicates that a service affecting condition
         * has developed and an urgent corrective action is required. Such a
         * severity can be reported, for example, when there is a severe
         * degradation in the capability of the managed object and its full
         * capability must be restored.
         */
        MAJOR,
        /**
         * From X.733: This indicates the existence of a non-service affecting
         * fault condition and that corrective action should be taken in order
         * to prevent a more serious (for example, service affecting) fault.
         * Such a severity can be reported, for example, when the detected alarm
         * condition is not currently degrading the capacity of the managed
         * object.
         */
        MINOR,
        /**
         * From X.733: This indicates the detection of a potential or impending
         * service affecting fault, before any significant effects have been
         * felt. Action should be taken to further diagnose (if necessary) and
         * correct the problem in order to prevent it from becoming a more
         * serious service affecting fault.
         */
        WARNING

    }

}
