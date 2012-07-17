package org.gdjclouds.provider.gdnova;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import org.gdjclouds.provider.gdnova.v100.*;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.config.ComputeServiceAdapterContextModule;
import org.jclouds.compute.domain.*;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.internal.ComputeServiceContextImpl;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Location;
import org.jclouds.functions.IdentityFunction;
import org.jclouds.location.suppliers.OnlyLocationOrFirstZone;
import org.jclouds.openstack.nova.compute.functions.FlavorToHardware;
import org.jclouds.openstack.nova.domain.Flavor;
import org.jclouds.openstack.nova.domain.ServerStatus;
import org.jclouds.rest.RestContext;
import org.jclouds.rest.internal.RestContextImpl;

import javax.inject.Singleton;
import java.util.Map;

//import org.jclouds.openstack.nova.provider.strategy.NovaComputeServiceAdapter;

/**
 * @author Adrian Cole
 */
public class NovaComputeServiceContextModule
        extends
        ComputeServiceAdapterContextModule<GDNovaClient, GDNovaAsyncClient, Server, Flavor, org.gdjclouds.provider.gdnova.v100.Image, Location> {
    public NovaComputeServiceContextModule() {
        super(GDNovaClient.class, GDNovaAsyncClient.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configure() {
        super.configure();
//1.3.1
        bind(new TypeLiteral<ComputeServiceAdapter<Server, Flavor, org.gdjclouds.provider.gdnova.v100.Image, Location>>() {
        }).to(NovaComputeServiceAdapter.class);

        //1.0.1
        bind(new TypeLiteral<ComputeServiceContext>() {
        }).to(new TypeLiteral<ComputeServiceContextImpl<GDNovaClient, GDNovaAsyncClient>>() {
        }).in(Scopes.SINGLETON);
        bind(new TypeLiteral<RestContext<GDNovaClient, GDNovaAsyncClient>>() {
        }).to(new TypeLiteral<RestContextImpl<GDNovaClient, GDNovaAsyncClient>>() {
        }).in(Scopes.SINGLETON);


        bind(new TypeLiteral<Function<org.gdjclouds.provider.gdnova.v100.Server, NodeMetadata>>() {
        }).to(GDServerToNodeMetadata.class);

        bind(new TypeLiteral<Function<org.gdjclouds.provider.gdnova.v100.Image, Image>>() {
        }).to(GDNovaImageToImage.class);
        bind(new TypeLiteral<Function<org.gdjclouds.provider.gdnova.v100.Image, OperatingSystem>>() {
        }).to(GDNovaImageToOperatingSystem.class);

        bind(new TypeLiteral<Function<Flavor, Hardware>>() {
        }).to(FlavorToHardware.class);

        // we aren't converting location from a provider-specific type
        bind(new TypeLiteral<Function<Location, Location>>() {
        }).to((Class) IdentityFunction.class);

        // there are no locations except the provider
        bind(new TypeLiteral<Supplier<Location>>() {
        }).to(OnlyLocationOrFirstZone.class);

        bind(new TypeLiteral<TemplateOptions>() {
        }).toProvider(new SuperTempateOptionsProvider());

    }

    public static class SuperTempateOptionsProvider implements Provider<TemplateOptions> {
        @Override
        public TemplateOptions get() {
            return new GDNovaTemplateOptions();
        }
    }

    @VisibleForTesting
    public static final Map<ServerStatus, NodeState> serverToNodeState = ImmutableMap
            .<ServerStatus, NodeState>builder().put(ServerStatus.ACTIVE, NodeState.RUNNING)//
            .put(ServerStatus.SUSPENDED, NodeState.SUSPENDED)//
            .put(ServerStatus.DELETED, NodeState.TERMINATED)//
            .put(ServerStatus.QUEUE_RESIZE, NodeState.PENDING)//
            .put(ServerStatus.PREP_RESIZE, NodeState.PENDING)//
            .put(ServerStatus.RESIZE, NodeState.PENDING)//
            .put(ServerStatus.VERIFY_RESIZE, NodeState.PENDING)//
            .put(ServerStatus.RESCUE, NodeState.PENDING)//
            .put(ServerStatus.BUILD, NodeState.PENDING)//
            .put(ServerStatus.PASSWORD, NodeState.PENDING)//
            .put(ServerStatus.REBUILD, NodeState.PENDING)//
            .put(ServerStatus.DELETE_IP, NodeState.PENDING)//
            .put(ServerStatus.REBOOT, NodeState.PENDING)//
            .put(ServerStatus.HARD_REBOOT, NodeState.PENDING)//
            .put(ServerStatus.UNKNOWN, NodeState.UNRECOGNIZED)//
            .put(ServerStatus.UNRECOGNIZED, NodeState.UNRECOGNIZED).build();

    @Singleton
    @Provides
    Map<ServerStatus, NodeState> provideServerToNodeState() {
        return serverToNodeState;
    }

}
