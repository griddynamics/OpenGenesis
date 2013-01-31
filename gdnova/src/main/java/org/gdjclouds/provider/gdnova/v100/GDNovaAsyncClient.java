package org.gdjclouds.provider.gdnova.v100;

import com.google.common.util.concurrent.ListenableFuture;
import org.jclouds.openstack.filters.AddTimestampQuery;
import org.jclouds.openstack.filters.AuthenticateRequest;
import org.jclouds.openstack.nova.ServerManagement;
import org.jclouds.openstack.nova.domain.Addresses;
import org.jclouds.openstack.nova.domain.Flavor;
import org.jclouds.openstack.nova.domain.RebootType;
import org.jclouds.openstack.nova.options.CreateServerOptions;
import org.jclouds.openstack.nova.options.ListOptions;
import org.jclouds.openstack.nova.options.RebuildServerOptions;
import org.jclouds.rest.annotations.Endpoint;
import org.jclouds.rest.annotations.ExceptionParser;
import org.jclouds.rest.annotations.MapBinder;
import org.jclouds.rest.annotations.Payload;
import org.jclouds.rest.annotations.PayloadParam;
import org.jclouds.rest.annotations.QueryParams;
import org.jclouds.rest.annotations.RequestFilters;
import org.jclouds.rest.annotations.SkipEncoding;
import org.jclouds.rest.annotations.Unwrap;
import org.jclouds.rest.functions.ReturnEmptySetOnNotFoundOr404;
import org.jclouds.rest.functions.ReturnFalseOnNotFoundOr404;
import org.jclouds.rest.functions.ReturnNullOnNotFoundOr404;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@SkipEncoding({'/', '='})
@RequestFilters({AuthenticateRequest.class, AddTimestampQuery.class})
@Endpoint(ServerManagement.class)
public interface GDNovaAsyncClient {

    @DELETE
    @ExceptionParser(ReturnFalseOnNotFoundOr404.class)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/servers/{id}")
    ListenableFuture<Boolean> deleteServer(@PathParam("id") String id);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/servers")
    @ExceptionParser(ReturnEmptySetOnNotFoundOr404.class)
    ListenableFuture<? extends Set<Server>> listServers(ListOptions... options);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @ExceptionParser(ReturnNullOnNotFoundOr404.class)
    @Path("/servers/{id}")
    ListenableFuture<Server> getServer(@PathParam("id") String id);

    @POST
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/action")
    @Produces(MediaType.APPLICATION_JSON)
    @Payload("%7B\"reboot\":%7B\"type\":\"{type}\"%7D%7D")
    ListenableFuture<Void> rebootServer(@PathParam("id") String id, @PayloadParam("type") RebootType rebootType);

    @POST
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/action")
    @Produces(MediaType.APPLICATION_JSON)
    @Payload("%7B\"resize\":%7B\"flavorId\":{flavorId}%7D%7D")
    ListenableFuture<Void> resizeServer(@PathParam("id") String id, @PayloadParam("flavorId") int flavorId);

    @POST
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/action")
    @Produces(MediaType.APPLICATION_JSON)
    @Payload("{\"confirmResize\":null}")
    ListenableFuture<Void> confirmResizeServer(@PathParam("id") String id);

    @POST
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/action")
    @Produces(MediaType.APPLICATION_JSON)
    @Payload("{\"revertResize\":null}")
    ListenableFuture<Void> revertResizeServer(@PathParam("id") String id);

    @POST
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/servers")
    @MapBinder(CreateServerOptions.class)
    ListenableFuture<Server> createServer(@PayloadParam("name") String name, @PayloadParam("imageRef") String imageRef,
                                                                            @PayloadParam("flavorRef") String flavorRef, CreateServerOptions... options);

    @POST
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/action")
    @MapBinder(RebuildServerOptions.class)
    ListenableFuture<Void> rebuildServer(@PathParam("id") String id, RebuildServerOptions... options);


    @POST
    @Path("/servers/{id}/action")
    @Produces(MediaType.APPLICATION_JSON)
    @Payload("%7B\"changePassword\":%7B\"adminPass\":\"{adminPass}\"%7D%7D")
    ListenableFuture<Void> changeAdminPass(@PathParam("id") String id, @PayloadParam("adminPass") String adminPass);

    @PUT
    @Path("/servers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Payload("%7B\"server\":%7B\"name\":\"{name}\"%7D%7D")
    ListenableFuture<Void> renameServer(@PathParam("id") String id, @PayloadParam("name") String newName);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/flavors")
    @ExceptionParser(ReturnEmptySetOnNotFoundOr404.class)
    ListenableFuture<? extends Set<org.jclouds.openstack.nova.domain.Flavor>> listFlavors(ListOptions... options);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/flavors/{id}")
    @ExceptionParser(ReturnNullOnNotFoundOr404.class)
    ListenableFuture<Flavor> getFlavor(@PathParam("id") int id);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/images")
    @ExceptionParser(ReturnEmptySetOnNotFoundOr404.class)
    ListenableFuture<? extends Set<Image>> listImages(ListOptions... options);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @ExceptionParser(ReturnNullOnNotFoundOr404.class)
    @QueryParams(keys = "format", values = "json")
    @Path("/images/{id}")
    ListenableFuture<Image> getImage(@PathParam("id") String id);

    @DELETE
    @ExceptionParser(ReturnFalseOnNotFoundOr404.class)
    @Path("/images/{id}")
    ListenableFuture<Boolean> deleteImage(@PathParam("id") String id);

    @POST
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/images")
    @Produces(MediaType.APPLICATION_JSON)
    @Payload("%7B\"image\":%7B\"serverId\":{serverId},\"name\":\"{name}\"%7D%7D")
    ListenableFuture<Image> createImageFromServer(@PayloadParam("name") String imageName,
                                                  @PayloadParam("serverId") String serverId);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/ips")
    ListenableFuture<Addresses> getAddresses(@PathParam("id") String serverId);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/ips/public")
    @ExceptionParser(ReturnEmptySetOnNotFoundOr404.class)
    ListenableFuture<? extends Set<String>> listPublicAddresses(@PathParam("id") String serverId);

    @GET
    @Unwrap
    @Consumes(MediaType.APPLICATION_JSON)
    @QueryParams(keys = "format", values = "json")
    @Path("/servers/{id}/ips/private")
    @ExceptionParser(ReturnEmptySetOnNotFoundOr404.class)
    ListenableFuture<? extends Set<String>> listPrivateAddresses(@PathParam("id") String serverId);

}
