/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ui.table.cell;


import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formats time values using {@link DateTimeFormatter}.
 */
public final class TimeFormatter extends AbstractCellFormatter {

    private static final Logger log = LoggerFactory.getLogger(TimeFormatter.class);

    private DateTimeFormatter dtf;

    // NOTE: Unlike other formatters in this package, this one is not
    //       implemented as a Singleton, because instances may be
    //       decorated with alternate locale and/or timezone.

    /**
     * Constructs a time formatter that uses the default locale and timezone.
     */
    public TimeFormatter() {
        dtf = new DateTimeFormatterBuilder()
                .appendValue(CLOCK_HOUR_OF_AMPM)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .appendLiteral(' ')
                .appendText(ChronoField.AMPM_OF_DAY)
                .optionalStart()
                .appendLiteral(' ')
                .appendOffset("+HH:MM", "+00:00")
                .toFormatter()
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault());
    }

    /**
     * Sets the locale to use for formatting the time.
     *
     * @param locale locale to use for formatting
     * @return self, for chaining
     */
    public TimeFormatter withLocale(Locale locale) {
        dtf = dtf.withLocale(locale);
        return this;
    }

    /**
     * Sets the time zone to use for formatting the time.
     *
     * @param zone time zone to use
     * @return self, for chaining
     */
    public TimeFormatter withZone(ZoneId zone) {
        dtf = dtf.withZone(zone);
        return this;
    }

    @Override
    protected String nonNullFormat(Object value) {
        if (value instanceof TemporalAccessor) {
            try {
                return dtf.format((TemporalAccessor) value);
            } catch (DateTimeException e) {
                log.error("Failed formatting {} [{}]", value, value.getClass().getSimpleName(), e);
                log.warn("dtf zone was {}", dtf.getZone());
                throw e;
            }
        } else if (value instanceof org.joda.time.DateTime) {
            return dtf.format(Instant.ofEpochMilli(((org.joda.time.DateTime) value).getMillis()));
        }
        // should never reach here
        return String.valueOf(value);
    }

}
