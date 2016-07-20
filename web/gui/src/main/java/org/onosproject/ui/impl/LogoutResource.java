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

import org.onlab.rest.BaseResource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Application upload resource.
 */
@Path("logout")
public class LogoutResource extends BaseResource {

    @Context
    private HttpServletRequest servletRequest;

    @GET
    public Response logout() throws IOException, URISyntaxException {
        servletRequest.getSession().invalidate();
        String url = servletRequest.getRequestURL().toString();
        url = url.replaceFirst("/onos/ui/.*", "/onos/ui/login.html");
        return Response.temporaryRedirect(new URI(url)).build();
    }

}
