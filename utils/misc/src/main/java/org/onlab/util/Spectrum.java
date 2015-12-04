/*
 * Copyright 2015 Open Networking Laboratory
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
    public static final Frequency CENTER_FREQUENCY = Frequency.ofTHz(193.1);

    // O band (original): 1260 to 1360 nm
    public static final Frequency O_BAND_MIN = Frequency.ofTHz(220.436);
    public static final Frequency O_BAND_MAX = Frequency.ofTHz(237.931);

    // E band (extended): 1360 to 1460 nm
    public static final Frequency E_BAND_MIN = Frequency.ofTHz(205.337);
    public static final Frequency E_BAND_MAX = Frequency.ofTHz(220.436);

    // S band (short wavelength): 1460 to 1530 nm
    public static final Frequency S_BAND_MIN = Frequency.ofTHz(195.943);
    public static final Frequency S_BAND_MAX = Frequency.ofTHz(205.337);

    // C band (conventional): 1530 to 1565 nm
    public static final Frequency C_BAND_MIN = Frequency.ofTHz(191.561);
    public static final Frequency C_BAND_MAX = Frequency.ofTHz(195.943);

    // L band (long wavelength): 1565 to 1625 nm
    public static final Frequency L_BAND_MIN = Frequency.ofTHz(184.488);
    public static final Frequency L_BAND_MAX = Frequency.ofTHz(191.561);

    // U band (ultra-long wavelength): 1625 to 1675 nm
    public static final Frequency U_BAND_MIN = Frequency.ofTHz(178.981);
    public static final Frequency U_BAND_MAX = Frequency.ofTHz(184.488);

    private Spectrum() {
    }
}
