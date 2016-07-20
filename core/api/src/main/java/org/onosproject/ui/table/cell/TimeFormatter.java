/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * Formats time values using {@link DateTimeFormatter}.
 */
public final class TimeFormatter extends AbstractCellFormatter {

    private DateTimeFormatter dtf;

    // NOTE: Unlike other formatters in this package, this one is not
    //       implemented as a Singleton, because instances may be
    //       decorated with alternate locale and/or timezone.

    /**
     * Constructs a time formatter that uses the default locale and timezone.
     */
    public TimeFormatter() {
        dtf = DateTimeFormat.longTime();
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
    public TimeFormatter withZone(DateTimeZone zone) {
        dtf = dtf.withZone(zone);
        return this;
    }

    @Override
    protected String nonNullFormat(Object value) {
        return dtf.print((DateTime) value);
    }

}
