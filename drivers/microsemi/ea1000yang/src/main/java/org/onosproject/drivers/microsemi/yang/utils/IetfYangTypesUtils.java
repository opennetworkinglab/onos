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
package org.onosproject.drivers.microsemi.yang.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev20130715.ietfyangtypes.DateAndTime;

/**
 * A utility class to change various YANG types to general purpose classes.
 */
public final class IetfYangTypesUtils {
    private IetfYangTypesUtils() {
        //Hiding the public constructor for this utility class
    }

    /**
     * Convert from Date and Time in a ietf-yang-types format to the Java Time API.
     * @param dateAndTime A date and time from a YANG object
     * @return A Date and Time with a Time Zone offset
     */
    public static OffsetDateTime fromYangDateTime(DateAndTime dateAndTime) {
        return OffsetDateTime.parse(dateAndTime.toString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Convert a from Date and Time in a ietf-yang-types format to the Java Time API and rezone to a given Time Zone.
     * @param dateAndTime A date and time from a YANG object
     * @param zoneId The time zone to rezone the time and date to
     * @return The rezoned time and date
     */
    public static ZonedDateTime fromYangDateTimeZoned(DateAndTime dateAndTime, ZoneId zoneId) {
        return OffsetDateTime.parse(dateAndTime.toString(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME).atZoneSameInstant(zoneId);
    }

    /**
     * Convert a from Date and Time in a ietf-yang-types format to the Java Time API rezoned to the local Time Zone.
     * @param dateAndTime A date and time from a YANG object
     * @return The date and time in the zone of this local machine
     */
    public static LocalDateTime fromYangDateTimeToLocal(DateAndTime dateAndTime) {
        OffsetDateTime odt = OffsetDateTime.parse(dateAndTime.toString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return LocalDateTime.ofInstant(odt.toInstant(), ZoneId.systemDefault());
    }
}
