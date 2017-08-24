/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.rest.server.api;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.api.KubernetesServiceApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = KubernetesApis.class)
@Api(tags = "Kubernetes Test Operations", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/k8s")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class KubernetesApis {
    private static final Logger logger = Logger.getLogger(KubernetesApis.class);

    @Reference
    private KubernetesServiceApi k8sService;

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private UserContextApi userContext;

    @ApiOperation(value = "List all pods with the given label",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/pods/label/{label}")
    @GET
    public Response getPodsByLabel(@Context HttpHeaders headers, @ApiParam(value = "label used to filter the pods",
    required = true) @PathParam("label") String label) throws VmidcException, IOException {
        logger.info("Getting pods by label.");

        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        String response = this.k8sService.getPodsByLabel(label);
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @ApiOperation(value = "Retrieves a pod with the given namespace and name",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/pods/namespace/{namespace}/name/{name}")
    @GET
    public Response getPodsByName(@Context HttpHeaders headers, @ApiParam(value = "The unique name of the targeted pod.",
    required = true) @PathParam("name") String name, @ApiParam(value = "The namespace for the targeted pod.",
    required = true) @PathParam("namespace") String namespace) {
        logger.info("Getting pods by name.");

        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        String response = this.k8sService.getPodsByName(namespace, name);
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @ApiOperation(value = "List all podes with any of the provided labels",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/pods/key/{key}/commaSeparatedLabels/{labels}")
    @GET
    public Response getPodsByLabels(@Context HttpHeaders headers, @ApiParam(value = "the label key",
    required = true) @PathParam("key") String key,  @ApiParam(value = "Comma separated labels",
    required = true) @PathParam("labels") String labels) {
        logger.info("Getting pods by labels.");

        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        String response = this.k8sService.getPodsByLabels(key, labels);
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @ApiOperation(value = "Creates a pod in a namespace with the given name and label",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/pods/namespace/{namespace}/name/{name}/label/{label}")
    @POST
    public void createPods(@Context HttpHeaders headers, @ApiParam(value = "label applied on the created pod",
    required = true) @PathParam("label") String label, @ApiParam(value = "name of the pod",
    required = true) @PathParam("name") String name, @ApiParam(value = "namespace for the pod",
    required = true) @PathParam("namespace") String namespace) {

        logger.info("Creating a pod.");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        this.k8sService.createPod(namespace, label, name);
    }

    @ApiOperation(value = "List all pod events",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/podEvents/{label}")
    @GET
    public Response getPodEvents(@Context HttpHeaders headers, @ApiParam(value = "label applied on the created pod",
    required = true) @PathParam("label") String label) throws InterruptedException {

        logger.info("Listing all pod events.");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        String response = this.k8sService.getPodEvents(label);
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @ApiOperation(value = "Register a kubernetes endpoint.",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/connector/ipaddress/{ipAddress}/port/{port}")
    @POST
    public void registerConnector(@Context HttpHeaders headers, @ApiParam(value = "the k8s ipaddress",
    required = true) @PathParam("ipAddress") String ipAddress, @ApiParam(value = "the k8s port",
    required = true) @PathParam("port") String port) {

        logger.info("Registering a k8s endpoint.");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        this.k8sService.registerConnector(ipAddress, port);
    }
    
    @ApiOperation(value = "Close a kubernetes endpoint.",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/close")
    @POST
    public void registerConnector(@Context HttpHeaders headers) {

        logger.info("Closing a k8s endpoint.");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        this.k8sService.closeConnector();
    }
    
    @ApiOperation(value = "Retrieves a pod with the given uid, name and namespace",
            response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/pods/uid/{uid}/name/{name}/namespace/{namespace}")
    @GET
    public Response getPodsById(@Context HttpHeaders headers, @ApiParam(value = "Unique uid used to filter the pods",
    required = true) @PathParam("uid") String uid, @ApiParam(value = "name of the pod",
    required = true) @PathParam("name") String name, @ApiParam(value = "namespace for the pod",
    required = true) @PathParam("namespace") String namespace) throws VmidcException, IOException {
        logger.info("Getting pod by uid and name.");

        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        String response = this.k8sService.getPodById(uid,name, namespace);
        return Response.status(Response.Status.OK).entity(response).build();
    }
}
