/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.util;


import java.util.Arrays;

/**
 * Calculates checksum for ISIS LSP packets.
 */
public class ChecksumCalculator {

    /**
     * Verifies the checksum is valid in given LSP packet bytes.
     *
     * @param lspPacket       lsp as byte array
     * @param lspChecksumPos1 position of checksum bit in packet
     * @param lspChecksumPos2 position of checksum bit in packet
     * @return true if valid else false
     */
    public boolean validateLspCheckSum(byte[] lspPacket, int lspChecksumPos1, int lspChecksumPos2) {

        byte[] checksum = calculateLspChecksum(lspPacket, lspChecksumPos1, lspChecksumPos2);
        if (lspPacket[lspChecksumPos1] == checksum[0] && lspPacket[lspChecksumPos2] == checksum[1]) {
            return true;
        }
        return false;
    }


    /**
     * Calculates the LSP checksum.
     *
     * @param lspBytes        as byte array
     * @param lspChecksumPos1 position of checksum bit in packet
     * @param lspChecksumPos2 position of checksum bit in packet
     * @return checksum bytes
     */
    public byte[] calculateLspChecksum(byte[] lspBytes, int lspChecksumPos1, int lspChecksumPos2) {

        byte[] tempLsaByte = Arrays.copyOf(lspBytes, lspBytes.length);

        int[] checksumOut = {0, 0};
        tempLsaByte[lspChecksumPos1] = 0;
        tempLsaByte[lspChecksumPos2] = 0;
        byte[] byteCheckSum = {0, 0};
        if (lspBytes != null) {
            for (int i = 12; i < tempLsaByte.length; i++) {
                checksumOut[0] = checksumOut[0] + ((int) tempLsaByte[i] & 0xFF);
                checksumOut[1] = checksumOut[1] + checksumOut[0];
            }
            checksumOut[0] = checksumOut[0] % 255;
            checksumOut[1] = checksumOut[1] % 255;
        }
        int byte1 = (int) ((tempLsaByte.length - lspChecksumPos1 - 1) * checksumOut[0] - checksumOut[1]) % 255;
        if (byte1 <= 0) {
            byte1 += 255;
        }
        int byte2 = 510 - checksumOut[0] - byte1;
        if (byte2 > 255) {
            byte2 -= 255;
        }

        byteCheckSum[0] = (byte) byte1;
        byteCheckSum[1] = (byte) byte2;

        return byteCheckSum;
    }
}