/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package org.gdjclouds.provider.gdnova.v100;

import org.jclouds.concurrent.Timeout;
import org.jclouds.openstack.nova.domain.Addresses;
import org.jclouds.openstack.nova.domain.Flavor;
import org.jclouds.openstack.nova.domain.RebootType;
import org.jclouds.openstack.nova.options.CreateServerOptions;
import org.jclouds.openstack.nova.options.ListOptions;
import org.jclouds.openstack.nova.options.RebuildServerOptions;

import javax.ws.rs.PathParam;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Timeout(duration = 60, timeUnit = TimeUnit.SECONDS)
public interface GDNovaClient {

    Set<Server> listServers(ListOptions... options);

    Server getServer(@PathParam("id") String id);

    boolean deleteServer(@PathParam("id") String id);

    void rebootServer(String id, RebootType rebootType);

    void resizeServer(String id, int flavorId);

    void confirmResizeServer(String id);

    void revertResizeServer(String id);

    Server createServer(String name, String imageRef, String flavorRef, CreateServerOptions... options);

    void rebuildServer(String id, RebuildServerOptions... options);

    void changeAdminPass(String id, String adminPass);

    void renameServer(String id, String newName);

    Set<org.jclouds.openstack.nova.domain.Flavor> listFlavors(ListOptions... options);

    Flavor getFlavor(int id);

    Set<Image> listImages(ListOptions... options);

    Image getImage(String id);

    boolean deleteImage(String id);

    Image createImageFromServer(String imageName, String serverId);

    Addresses getAddresses(String serverId);

    Set<String> listPublicAddresses(String serverId);

    Set<String> listPrivateAddresses(String serverId);
}
