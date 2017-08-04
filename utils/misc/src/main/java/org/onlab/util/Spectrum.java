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
package org.onlab.util;

/**
 * Telecom optical wavelength bands: O, E, S, C, L and U bands.
 *
 * See ITU-T G-Series Recommendations, Supplement 39
 * See ITU-T G.694.1 for center frequency definition.
 */
public final class Spectrum {

    // Center frequency
    public static final Frequency CENTER_FREQUENCY = Frequency.ofGHz(193_100);

    // O band (original): 1260 to 1360 nm
    public static final Frequency O_BAND_MIN = Frequency.ofGHz(220_436);
    public static final Frequency O_BAND_MAX = Frequency.ofGHz(237_931);

    // E band (extended): 1360 to 1460 nm
    public static final Frequency E_BAND_MIN = Frequency.ofGHz(205_337);
    public static final Frequency E_BAND_MAX = Frequency.ofGHz(220_436);

    // S band (short wavelength): 1460 to 1530 nm
    public static final Frequency S_BAND_MIN = Frequency.ofGHz(195_943);
    public static final Frequency S_BAND_MAX = Frequency.ofGHz(205_337);

    // C band (conventional): 1530 to 1565 nm
    public static final Frequency C_BAND_MIN = Frequency.ofGHz(191_561);
    public static final Frequency C_BAND_MAX = Frequency.ofGHz(195_943);

    // L band (long wavelength): 1565 to 1625 nm
    public static final Frequency L_BAND_MIN = Frequency.ofGHz(184_488);
    public static final Frequency L_BAND_MAX = Frequency.ofGHz(191_561);

    // U band (ultra-long wavelength): 1625 to 1675 nm
    public static final Frequency U_BAND_MIN = Frequency.ofGHz(178_981);
    public static final Frequency U_BAND_MAX = Frequency.ofGHz(184_488);

    private Spectrum() {
    }
}
