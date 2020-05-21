/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.memory;

import java.util.Map;
import java.util.HashMap;

/**
 * Representation of main memory types.
 */
public enum MemoryType {

    /**
     * Main memory types are divided into two main
     * categories, i.e., RAM and ROM.
     *
     * RAM memories are further divided into:
     * |->      SRAM: Static Random Access Memory
     * |->      DRAM: Dynamic Random Access Memory
     * |->     SDRAM: Synchronous DRAM
     * |-> DDR-SDRAM: Double data-rate synchronous DRAM
     *     |--> DDR2: 2nd Generation DDR
     *     |--> DDR3: 3rd Generation DDR
     *     |--> DDR4: 4th Generation DDR
     *     |--> DDR5: 5th Generation DDR (coming soon)
     * |->     CDRAM: Cached DRAM
     * |->     EDRAM: Embedded DRAM
     * |->     SGRAM: Synchronous Graphics RAM
     * |->      VRAM: Video DRAM
     *
     * ROM memories are further divided into:
     * |->   PROM: Programmable read-only memory
     * |->  EPROM: Erasable Programmable read only memory
     * |-> FEPROM: Flash Erasable Programmable Read Only Memory
     * |-> EEPROM: Electrically erasable programmable read only memory
     */
    SRAM("SRAM"),
    DRAM("DRAM"),
    SDRAM("SDRAM"),
    DDR("DDR"),
    DDR2("DDR2"),
    DDR3("DDR3"),
    DDR4("DDR4"),
    DDR5("DDR5"),
    CDRAM("CDRAM"),
    EDRAM("EDRAM"),
    SGRAM("SGRAM"),
    VRAM("VRAM"),
    PROM("PROM"),
    EPROM("EPROM"),
    FEPROM("FEPROM"),
    EEPROM("EEPROM");

    private String memoryType;

    // Statically maps main memory types to enum types
    private static final Map<String, MemoryType> MAP =
        new HashMap<String, MemoryType>();
    static {
        for (MemoryType mt : MemoryType.values()) {
            MAP.put(mt.toString().toLowerCase(), mt);
        }
    }

    private MemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public static MemoryType getByName(String mt) {
        return MAP.get(mt.toLowerCase());
    }

    public static boolean isRam(MemoryType memoryType) {
        return (memoryType == DRAM) || (memoryType == SRAM);
    }

    public static boolean isRom(MemoryType memoryType) {
        return !isRam(memoryType);
    }

    @Override
    public String toString() {
        return this.memoryType;
    }

}
