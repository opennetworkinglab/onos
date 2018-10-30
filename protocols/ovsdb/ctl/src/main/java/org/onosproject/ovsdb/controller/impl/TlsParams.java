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
package org.onosproject.ovsdb.controller.impl;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

import static org.onosproject.ovsdb.controller.impl.OsgiPropertyConstants.*;

/**
 * TlsParams Class for properties required for configuring OVSDB TLS Connection.
 */
public class TlsParams {

    private static final Logger log = LoggerFactory
            .getLogger(Controller.class);

    /**
     * Options for Activated / Deactivated TLS Mode.
     */
    enum TlsMode {
        /**
         * Signifies that TLS is enabled.
         */
        ENABLED,
        /**
         * Signifies that TLS is disabled.
         */
        DISABLED
    }

    protected static final EnumSet<TlsMode> TLS_ENABLED = EnumSet.of(TlsMode.ENABLED);

    final TlsMode mode;
    final String ksLocation;
    final String tsLocation;
    final String ksPwd;
    final String tsPwd;
    final byte[] ksSignature;
    final byte[] tsSignature;

    /**
     * Default Constructor.
     */
    TlsParams() {
        this.mode = TlsMode.DISABLED;
        this.ksLocation = KS_FILE_DEFAULT;
        this.tsLocation = TS_FILE_DEFAULT;
        this.ksPwd = KS_PASSWORD_DEFAULT;
        this.tsPwd = TS_PASSWORD_DEFAULT;
        this.ksSignature = getSha1Checksum(ksLocation);
        this.tsSignature = getSha1Checksum(tsLocation);
    }

    /**
     * Creates new Tls params.
     *
     * @param mode TlsMode
     * @param ksLocation keyStore Location
     * @param tsLocation trustStore Location
     * @param ksPwd keyStore Password
     * @param tsPwd trustStore Password
     */
    TlsParams(TlsMode mode, String ksLocation, String tsLocation,
              String ksPwd, String tsPwd) {
        this.mode = mode;
        this.ksLocation = ksLocation;
        this.tsLocation = tsLocation;
        this.ksPwd = ksPwd;
        this.tsPwd = tsPwd;
        this.ksSignature = getSha1Checksum(ksLocation);
        this.tsSignature = getSha1Checksum(tsLocation);
    }

    /**
     * Exposes the keyStore password in char[] format.
     *
     * @return the keyStorePassword as a char array
     */
    public char[] ksPwd() {
        return ksPwd.toCharArray();
    }

    /**
     * Exposes the trustStore password in char[] format.
     *
     * @return the trustStorePassword as a char array
     */
    public char[] tsPwd() {
        return tsPwd.toCharArray();
    }

    /**
     * Returns whether TLS is enabled or not.
     *
     * @return true if TLS is enabled otherwise false
     */
    public boolean isTlsEnabled() {
        return TLS_ENABLED.contains(mode);
    }

    /**
     * Returns SHA1 Checksum from a JKS.
     *
     * @param filepath JKS FilePath
     * @return byte[] sha1checksum
     */
    public byte[] getSha1Checksum(String filepath) {
        if (filepath == null) {
            return new byte[0];
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            File f = new File(filepath);
            FileInputStream is = new FileInputStream(f);
            DigestInputStream dis = new DigestInputStream(is, digest);
            byte[] buffer = new byte[1024];
            while (dis.read(buffer) > 0) {
                // nothing to do :)
            }
            is.close();
            return dis.getMessageDigest().digest();
        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm SHA1 Not found");
        } catch (IOException e) {
            log.info("Error reading file file: {}", filepath);
        }
        return new byte[0];
    }

    @Override
    public int hashCode() {
        if (mode == TlsMode.DISABLED) {
            return Objects.hash(mode);
        }
        return Objects.hash(mode, ksLocation, tsLocation,
                            ksPwd, tsPwd,
                            Arrays.hashCode(ksSignature),
                            Arrays.hashCode(tsSignature));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TlsParams) {
            final TlsParams that = (TlsParams) obj;
            if (this.getClass() != that.getClass()) {
                return false;
            } else if (this.mode == that.mode && this.mode == TlsMode.DISABLED) {
                // All disabled objects should be equal regardless of other params
                return true;
            }
            return this.mode == that.mode &&
                    Objects.equals(this.ksLocation, that.ksLocation) &&
                    Objects.equals(this.tsLocation, that.tsLocation) &&
                    Objects.equals(this.ksPwd, that.ksPwd) &&
                    Objects.equals(this.tsPwd, that.tsPwd) &&
                    Arrays.equals(this.ksSignature, that.ksSignature) &&
                    Arrays.equals(this.tsSignature, that.tsSignature);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tlsMode", mode.toString().toLowerCase())
                .add("ksLocation", ksLocation)
                .add("tsLocation", tsLocation)
                .toString();
    }
}