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
 *
 * Application developed by Elisa Rojas Sanchez
 */
package org.ctpd.closfwd;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.ctpd.closfwd.OltEndpoint;
import org.ctpd.closfwd.OltControlEndpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ServiceEndpoint;
import org.ctpd.closfwd.VpdcEndpoint;
import org.ctpd.closfwd.VpdcHostEndpoint;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.rest.AbstractWebResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ctpd.closfwd.ExceptionHandler;
import org.ctpd.closfwd.CustomiceException;
import org.ctpd.closfwd.ControllerEndpoint;

import java.util.Iterator;

/**
 * CLOSFWD REST APIs.
 */

@Path("closfwdapp")
public class ClosFwdWebResource extends AbstractWebResource {

	private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("register")
	public Response register(InputStream stream) {
		List<Endpoint> devices = null;
		JsonFactory factory = new JsonFactory().enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
		// .enable(JsonParser.Feature.ALLOW_MISSING_VALUES)
		// .enable(JsonParser.Feature.IGNORE_UNDEFINED);
		ObjectMapper mapper = new ObjectMapper(factory);
		ObjectNode objNode = mapper.createObjectNode();
		try {
			devices = mapper.readValue(stream,
					mapper.getTypeFactory().constructCollectionType(List.class, Endpoint.class));

			ControllerEndpoint.EntryArgument(devices);

		} catch (Exception e) {
			log.error("Exception in register endpoint", e);
			return Response.status(400).entity(objNode.put("response", e.toString())).build();
			// throw new CustomiceException("Error al registrar en endpoint", e);
		}

		ClosDeviceService service = get(ClosDeviceService.class);
		Map<UUID, Endpoint> data = new LinkedHashMap<UUID, Endpoint>();
		try {
			for (Endpoint device : devices) {
				data.put(service.addEndpoint(device), device);
			}
		} catch (CustomiceException e) {
			log.error("Exception in endpoint registration", e);
			return Response.status(400).entity(objNode.put("response", e.toString())).build();
		}
		log.debug("Register enpoint: " + data.toString());

		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

		try {
			String result = mapper
					.writerFor(mapper.getTypeFactory().constructMapType(Map.class, UUID.class, Endpoint.class))
					.writeValueAsString(data);
			log.debug("Successful endpoint registration");
			return ok(result).build();
		} catch (Exception e) {
			log.error("Exception in endpoint registration", e);
			return Response.status(500).entity(objNode.put("response", e.toString())).build();
			// new CustomiceException("no se ha creado el endpoint, error de argumentos
			// pasados",e);

		}
		// return null;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}")
	public Response getDevice(@PathParam("id") UUID id) {
		log.debug("Get endpoint " + id);
		ClosDeviceService service = get(ClosDeviceService.class);
		Endpoint device = service.getEndpoint(id);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objNode = mapper.createObjectNode();
		if (device != null) {
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			try {
				String result = mapper.writerFor(Endpoint.class).writeValueAsString(device);
				log.debug("Endpoint " + id + " successfully provided");
				return ok(result).build();
			} catch (Exception e) {
				log.error("Error when providing endpoint " + id);
				return Response.status(500).entity(objNode.put("response", "SERIALIZING ERROR: " + e.toString()))
						.build();
			}
		}
		log.debug("Endpoint " + id + " not found");
		return Response.status(404)
				.entity(objNode.put("response", String.format("No endpoint with id %s", id.toString()))).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}")
	public Response removeRegistry(@PathParam("id") UUID id) {
		log.debug("Delete endpoint " + id);

		ClosDeviceService service = get(ClosDeviceService.class);
		log.debug("Begin to remove");
		// Endpoint device = service.getEndpoint(id);

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objNode = mapper.createObjectNode();
		Endpoint device;
		Endpoint dev;

		try {
			device = service.removeEndpoint(id);
		} catch (CustomiceException e) {
			log.error("Error when deleteing endpoint " + id);
			return Response.status(400).entity(objNode.put("response", e.toString())).build();
		}
		// device=service.removeEndpoint(id);

		if (device != null) {
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			try {
				String result = mapper.writerFor(Endpoint.class).writeValueAsString(device);
				log.debug("Endpoint " + id + " successfully deleted");
				return Response.status(204).build();
			} catch (Exception e) {
				log.error("Error when deleteing endpoint " + id);
				return Response.status(500).entity(objNode.put("response", "SERIALIZING ERROR:\n" + e.toString()))
						.build();
			}
		}
		/*
		 * try{ throw new
		 * CustomiceException("El endpoint no existe o está referenciado") }
		 */
		// catch(Exception e){
		log.debug("Endpoint " + id + " not found");
		return Response.status(404).entity(objNode.put("response", String.format("No endpoint with id %s\n", id)))
				.build();
		// }

	}

}
