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
package org.onosproject.driver.optical.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.GridType;
import org.onosproject.net.config.BaseConfig;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;

/**
 * Configuration about available lambda resource on a port.
 */
@Beta
public class LambdaConfig extends BaseConfig<ConnectPoint> {

    private static final Logger log = getLogger(LambdaConfig.class);

    /**
     * Configuration key for {@link LambdaConfig}. ({@value #CONFIG_KEY})
     */
    public static final String CONFIG_KEY = "lambdas";

    /**
     * JSON key for GridType. {@value #GRID_TYPE}.
     * Expects a string containing one of {@link GridType} element.
     */
    private static final String GRID_TYPE = "gridType";

    /**
     * JSON key for DWDM ChannelSpacing. {@value #DWDM_SPACING}.
     * Expects a string containing one of {@link ChannelSpacing} element.
     */
    private static final String DWDM_SPACING = "dwdmSpacing";

    /**
     * JSON key for start value of slot index (inclusive). {@value #SLOT_START}.
     *
     * Expects an integer value. (default:1).
     * @see OchSignal#newDwdmSlot(ChannelSpacing, int)
     * @see OchSignal#newFlexGridSlot(int)
     */
    private static final String SLOT_START = "slotStart";

    /**
     * JSON key for step value of slot index. {@value #SLOT_START}.
     *
     * Expects a positive integer value. (default:1).
     * @see OchSignal#newDwdmSlot(ChannelSpacing, int)
     * @see OchSignal#newFlexGridSlot(int)
     */
    private static final String SLOT_STEP = "slotStep";

    /**
     * JSON key for end value of slot index (inclusive). {@value #SLOT_START}.
     *
     * Expects an integer value. (default:1).
     * @see OchSignal#newDwdmSlot(ChannelSpacing, int)
     * @see OchSignal#newFlexGridSlot(int)
     */
    private static final String SLOT_END = "slotEnd";

    @Override
    public boolean isValid() {
        if (!hasField(GRID_TYPE)) {
            log.warn("GridType missing: {}", this);
            return false;
        }
        try {
            GridType type = gridType();
            Optional<ChannelSpacing> dwdmSpacing = dwdmSpacing();
            if (type == GridType.DWDM) {
                if (!dwdmSpacing.isPresent()) {
                    log.warn("DWDM spacing missing: {}", this);
                    return false;
                }
            }

            return slotStep() > 0 &&
                   slotStart() <= slotEnd();
        } catch (NullPointerException | IllegalArgumentException e) {
            log.warn("Invalid netcfg: {}", this, e);
            return false;
        }
    }

    /**
     * Returns available lambda resource type.
     *
     * @return grid type
     */
    public GridType gridType() {
        return GridType.valueOf(get(GRID_TYPE, null));
    }

    /**
     * Returns DWDM channel spacing.
     *
     * @return channel spacing if specified
     */
    public Optional<ChannelSpacing> dwdmSpacing() {
        return Optional.ofNullable(get(DWDM_SPACING, null))
                       .map(ChannelSpacing::valueOf);
    }

    /**
     * Returns start of slot index. (inclusive)
     *
     * @return start of slot index
     */
    public int slotStart() {
        return get(SLOT_START, 1);
    }

    /**
     * Returns positive incremental step of slot index.
     *
     * @return step of slot index
     */
    public int slotStep() {
        return get(SLOT_STEP, 1);
    }

    /**
     * Returns end of slot index. (inclusive)
     *
     * @return end of slot index
     */
    public int slotEnd() {
        return get(SLOT_END, 1);
    }


    /**
     * Create a {@link LambdaConfig}.
     * <p>
     * Note: created instance needs to be initialized by #init(..) before using.
     */
    public LambdaConfig() {
        super();
    }

    /**
     * Create a {@link LambdaConfig} for specified Port.
     * <p>
     * Note: created instance is not bound to NetworkConfigService,
     * cannot use {@link #apply()}. Must be passed to the service
     * using NetworkConfigService#applyConfig
     *
     * @param cp ConnectPoint for a port
     */
    public LambdaConfig(ConnectPoint cp) {
        ObjectMapper mapper = new ObjectMapper();
        init(cp, CONFIG_KEY, mapper.createObjectNode(), mapper, null);
    }

}
