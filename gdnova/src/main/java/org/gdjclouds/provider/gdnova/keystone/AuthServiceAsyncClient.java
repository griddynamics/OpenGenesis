package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.util.concurrent.ListenableFuture;
import org.gdjclouds.provider.gdnova.v100.AccessWrapper;
import org.jclouds.rest.annotations.MapBinder;
import org.jclouds.rest.annotations.PayloadParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

//import org.jclouds.rest.annotations.SelectJson;

@Path("/v2.0")
public interface AuthServiceAsyncClient {

    @POST
//    @SelectJson("access")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tokens")
    @MapBinder(BindAuthJsonPayload.class)
    ListenableFuture<AccessWrapper> authenticateTenantWithCredentials(@PayloadParam("tenantName") String tenantId,
                                                                      PasswordCredentials passwordCredentials);

}
