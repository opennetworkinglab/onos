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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.rest.AbstractInjectionResource;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiPreferencesService;
import org.onosproject.ui.UiSessionToken;
import org.onosproject.ui.UiTokenService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Resource for serving the dynamically composed index.html.
 */
@Path("/")
public class MainIndexResource extends AbstractInjectionResource {

    private static final String INDEX = "index.html";
    private static final String NOT_READY = "not-ready.html";

    private static final String INJECT_USER_START = "<!-- {INJECTED-USER-START} -->";
    private static final String INJECT_USER_END = "<!-- {INJECTED-USER-END} -->";

    private static final String INJECT_CSS_START = "<!-- {INJECTED-STYLESHEETS-START} -->";
    private static final String INJECT_CSS_END = "<!-- {INJECTED-STYLESHEETS-END} -->";

    private static final String INJECT_JS_START = "<!-- {INJECTED-JAVASCRIPT-START} -->";
    private static final String INJECT_JS_END = "<!-- {INJECTED-JAVASCRIPT-END} -->";

    private static final byte[] SCRIPT_START = "\n<script>\n".getBytes();
    private static final byte[] SCRIPT_END = "</script>\n\n".getBytes();

    @Context
    private SecurityContext ctx;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getMainIndex() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        UiExtensionService service;
        UiTokenService tokens;

        try {
            service = get(UiExtensionService.class);
            tokens = get(UiTokenService.class);

        } catch (ServiceNotFoundException e) {
            return Response.ok(classLoader.getResourceAsStream(NOT_READY)).build();
        }

        InputStream indexTemplate = classLoader.getResourceAsStream(INDEX);
        String index = new String(toByteArray(indexTemplate));

        int p0s = split(index,   0, INJECT_USER_START) - INJECT_USER_START.length();
        int p0e = split(index, p0s, INJECT_USER_END);
        int p1s = split(index, p0e, INJECT_JS_START) - INJECT_JS_START.length();
        int p1e = split(index, p1s, INJECT_JS_END);
        int p2s = split(index, p1e, INJECT_CSS_START) - INJECT_CSS_START.length();
        int p2e = split(index, p2s, INJECT_CSS_END);
        int p3s = split(index, p2e, null);


        // FIXME: use global opaque auth token to allow secure failover

        // for now, just use the user principal name...
        String userName = ctx.getUserPrincipal().getName();

        // get a session token to use for UI-web-socket authentication
        UiSessionToken token = tokens.issueToken(userName);

        String auth = "var onosUser='" + userName + "',\n" +
                      "    onosAuth='" + token + "';\n";

        StreamEnumeration streams =
                new StreamEnumeration(of(stream(index, 0, p0s),
                        new ByteArrayInputStream(SCRIPT_START),
                        stream(auth, 0, auth.length()),
                        userPreferences(userName),
                        userConsoleLog(userName),
                        new ByteArrayInputStream(SCRIPT_END),
                        stream(index, p0e, p1s),
                        includeJs(service),
                        stream(index, p1e, p2s),
                        includeCss(service),
                        stream(index, p2e, p3s)));

        return Response.ok(new SequenceInputStream(streams)).build();
    }

    private InputStream userConsoleLog(String userName) {
        String code = "console.log('Logging in as user >" + userName + "<');\n";
        return new ByteArrayInputStream(code.getBytes());
    }

    // Produces an input stream including user preferences.
    private InputStream userPreferences(String userName) {
        UiPreferencesService service = get(UiPreferencesService.class);
        ObjectNode prefs = mapper().createObjectNode();
        service.getPreferences(userName).forEach(prefs::set);
        String string = "var userPrefs = " + prefs.toString() + ";\n";
        return new ByteArrayInputStream(string.getBytes());
    }

    // Produces an input stream including JS injections from all extensions.
    private InputStream includeJs(UiExtensionService service) {
        Builder<InputStream> builder = ImmutableList.builder();
        service.getExtensions().forEach(ext -> {
            add(builder, ext.js());
            add(builder, new NewlineInputStream());
        });
        return new SequenceInputStream(new StreamEnumeration(builder.build()));
    }

    // Produces an input stream including CSS injections from all extensions.
    private InputStream includeCss(UiExtensionService service) {
        Builder<InputStream> builder = ImmutableList.builder();
        service.getExtensions().forEach(ext -> {
            add(builder, ext.css());
            add(builder, new NewlineInputStream());
        });
        return new SequenceInputStream(new StreamEnumeration(builder.build()));
    }

    // Safely adds the stream to the list builder only if stream is not null.
    private void add(Builder<InputStream> builder, InputStream inputStream) {
        if (inputStream != null) {
            builder.add(inputStream);
        }
    }

    private static final String NL = String.format("%n");
    private static final byte[] NL_BYTES = NL.getBytes();

    private static class NewlineInputStream extends InputStream {
        private int index = 0;

        @Override
        public int read() throws IOException {
            if (index == NL_BYTES.length) {
                return -1;
            }
            return NL_BYTES[index++];
        }
    }

}
