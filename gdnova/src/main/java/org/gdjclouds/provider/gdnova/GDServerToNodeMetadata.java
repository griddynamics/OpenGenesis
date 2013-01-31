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
package org.gdjclouds.provider.gdnova;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import org.gdjclouds.provider.gdnova.v100.Server;
import org.jclouds.collect.Memoized;
import org.jclouds.compute.domain.*;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.logging.Logger;
import org.jclouds.openstack.nova.domain.Address;
import org.jclouds.openstack.nova.domain.ServerStatus;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.compute.util.ComputeServiceUtils.parseGroupFromName;

@Singleton
public class GDServerToNodeMetadata implements Function<Server, NodeMetadata> {
    @Resource
    @Named(ComputeServiceConstants.COMPUTE_LOGGER)
    protected Logger logger = Logger.NULL;

    protected final Supplier<Location> location;
    protected final Map<ServerStatus, NodeState> serverToNodeState;
    protected final Supplier<Set<? extends Image>> images;
    protected final Supplier<Set<? extends Hardware>> hardwares;

    private static class FindImageForServer implements Predicate<Image> {
        private final Server instance;

        private FindImageForServer(Server instance) {
            this.instance = instance;
        }

        @Override
        public boolean apply(Image input) {
            return input.getId().equals("" + instance.getImageRef());
        }
    }

    private static class FindHardwareForServer implements Predicate<Hardware> {
        private final Server instance;

        private FindHardwareForServer(Server instance) {
            this.instance = instance;
        }

        @Override
        public boolean apply(Hardware input) {
            return input.getId().equals("" + instance.getFlavorRef());
        }
    }

    @Inject
    GDServerToNodeMetadata(Map<ServerStatus, NodeState> serverStateToNodeState,
                         @Memoized Supplier<Set<? extends Image>> images, Supplier<Location> location,
                         @Memoized Supplier<Set<? extends Hardware>> hardwares) {
        this.serverToNodeState = checkNotNull(serverStateToNodeState, "serverStateToNodeState");
        this.images = checkNotNull(images, "images");
        this.location = checkNotNull(location, "location");
        this.hardwares = checkNotNull(hardwares, "hardwares");
    }

    @Override
    public NodeMetadata apply(Server from) {
        NodeMetadataBuilder builder = new NodeMetadataBuilder();
        builder.ids(from.getId() + "");
        builder.name(from.getName());
        builder.location(new LocationBuilder().scope(LocationScope.HOST).id(from.getHostId()).description(
            from.getHostId()).parent(location.get()).build());
        builder.userMetadata(from.getMetadata());
        builder.group(parseGroupFromName(from.getName()));
        Image image = parseImage(from);
        if (image != null) {
            builder.imageId(image.getId());
            builder.operatingSystem(image.getOperatingSystem());
        }
        builder.hardware(parseHardware(from));
        builder.state(serverToNodeState.get(from.getStatus()));

        builder.publicAddresses(Iterables.transform(from.getAddresses().getPublicAddresses(), Address.newAddress2StringFunction()));
        builder.privateAddresses(Iterables.transform(from.getAddresses().getPrivateAddresses(), Address.newAddress2StringFunction()));
        builder.uri(from.getURI());
        return builder.build();
    }

    protected Hardware parseHardware(Server from) {
        try {
            return Iterables.find(hardwares.get(), new FindHardwareForServer(from));
        } catch (NoSuchElementException e) {
            logger.warn("could not find a matching hardware for server %s", from);
        }
        return null;
    }

    protected Image parseImage(Server from) {
        try {
            return Iterables.find(images.get(), new FindImageForServer(from));
        } catch (NoSuchElementException e) {
            logger.warn("could not find a matching image for server %s in location %s", from, location);
        }
        return null;
    }
}
