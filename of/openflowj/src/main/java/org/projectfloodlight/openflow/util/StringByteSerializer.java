/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.projectfloodlight.openflow.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;

public class StringByteSerializer {
    public static String readFrom(final ChannelBuffer data, final int length) {
        byte[] stringBytes = new byte[length];
        data.readBytes(stringBytes);
        // find the first index of 0
        int index = 0;
        for (byte b : stringBytes) {
            if (0 == b)
                break;
            ++index;
        }
        return new String(Arrays.copyOf(stringBytes, index), Charset.forName("ascii"));
    }

    public static void writeTo(final ChannelBuffer data, final int length,
            final String value) {
        try {
            byte[] name = value.getBytes("ASCII");
            if (name.length < length) {
                data.writeBytes(name);
                for (int i = name.length; i < length; ++i) {
                    data.writeByte((byte) 0);
                }
            } else {
                data.writeBytes(name, 0, length - 1);
                data.writeByte((byte) 0);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }
}
