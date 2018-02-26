/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.soam;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A representation of a ratio in milli-percent.
 */
public final class MilliPct extends Number {

    private static final long serialVersionUID = 1476288484705687568L;
    private static NumberFormat pf = DecimalFormat.getPercentInstance();

    private int value;

    private MilliPct(int value) {
        this.value = value;
        pf.setMaximumFractionDigits(3);
    }

    public static MilliPct ofMilliPct(int value) {
        return new MilliPct(value);
    }

    public static MilliPct ofPercent(float value) {
        return new MilliPct(Float.valueOf(value * 1000f).intValue());
    }

    public static MilliPct ofRatio(float value) {
        return new MilliPct(Float.valueOf(value * 100000f).intValue());
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public float percentValue() {
        return value / 1000f;
    }

    public float ratioValue() {
        return value / 100000f;
    }

    @Override
    public String toString() {
        return pf.format(ratioValue());
    }
}
