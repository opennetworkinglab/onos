/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onosproject.ui.impl;

import com.google.common.io.ByteStreams;
import org.onosproject.rest.AbstractInjectionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Resource for serving up post-processed raw data files.
 */
@Path("/")
public class FooResource extends AbstractInjectionResource {

    private static final String ROOT = "/raw/";
    private static final String PNG = "png";
    private static final byte UMASK = -16;
    private static final byte LMASK = 15;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static void clean(ByteBuffer bb, byte b1, byte b2) {
        bb.put((byte) ((b1 & UMASK) | (b2 & LMASK)));
    }

    private static ByteBuffer decodeBin(byte[] bytes) {
        int size = bytes.length;
        ByteBuffer bb = ByteBuffer.allocate(size / 2);
        for (int i = 0; i < size; i += 2) {
            clean(bb, bytes[i], bytes[i + 1]);
        }
        return bb;
    }

    private static void watermark(BufferedImage bi) {
        // to be implemented...
    }

    private static byte[] decodeAndMark(byte[] bytes) throws IOException {
        ByteBuffer bb = decodeBin(bytes);
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bb.array()));
        watermark(bi);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, PNG, baos);
        return baos.toByteArray();
    }

    @Path("{resource}")
    @GET
    @Produces("image/png")
    public Response getBinResource(@PathParam("resource") String resource)
            throws IOException {

        String path = ROOT + resource;
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);

        if (is == null) {
            log.warn("Didn't find resource {}", path);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        byte[] bytes = ByteStreams.toByteArray(is);
        log.info("Processing resource {} ({} bytes)", path, bytes.length);
        return Response.ok(decodeAndMark(bytes)).build();
    }
}
