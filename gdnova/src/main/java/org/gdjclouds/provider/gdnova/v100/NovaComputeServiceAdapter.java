package org.gdjclouds.provider.gdnova.v100;

import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.location.suppliers.JustProvider;
import org.jclouds.openstack.nova.domain.Flavor;
import org.jclouds.openstack.nova.domain.RebootType;
import org.jclouds.openstack.nova.options.ListOptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.openstack.nova.options.ListOptions.Builder.withDetails;

/**
 * defines the connection between the {@link org.jclouds.openstack.nova.NovaClient} implementation and the jclouds
 * {@link org.jclouds.compute.ComputeService}
 */
@Singleton
public class NovaComputeServiceAdapter implements ComputeServiceAdapter<Server, Flavor, Image, Location> {

    protected final GDNovaClient client;
    private JustProvider provider;

    @Inject
    protected NovaComputeServiceAdapter(GDNovaClient client, JustProvider provider) {
        this.client = checkNotNull(client, "client");
        this.provider = checkNotNull(provider, "provider");
    }

    @Override
    public Server createNodeWithGroupEncodedIntoNameThenStoreCredentials(String tag, String name, Template template, Map<String, Credentials> credentialStore) {
        GDNovaCreateServerOptions createServerOptions = new GDNovaCreateServerOptions();
        if (template.getOptions() instanceof GDNovaTemplateOptions) {
            GDNovaTemplateOptions options = (GDNovaTemplateOptions) template.getOptions();
            createServerOptions = createServerOptions.withKeyPair(options.getKeyPair());
        }
        try {
            Server server = client.createServer(name, template.getImage().getId(), template.getHardware().getId(),
                    createServerOptions);
            if(server.getHostId() != null) {
                return server;
            } else {
                return client.getServer(server.getId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<Flavor> listHardwareProfiles() {
        return client.listFlavors(withDetails());
    }

    @Override
    public Iterable<Image> listImages() {
        return client.listImages(withDetails());
    }

    @Override
    public Iterable<Server> listNodes() {
        return client.listServers(ListOptions.Builder.withDetails());
    }

    @Override
    public Iterable<Location> listLocations() {
        return (Iterable<Location>) provider.get();
    }

    @Override
    public Server getNode(String id) {
        return client.getServer(id);
    }

    @Override
    public void destroyNode(String id) {
        client.deleteServer(id);
    }

    @Override
    public void rebootNode(String id) {
        client.rebootServer(id, RebootType.HARD);
    }

    @Override
    public void resumeNode(String id) {
        throw new UnsupportedOperationException("suspend not supported");
    }

    @Override
    public void suspendNode(String id) {
        throw new UnsupportedOperationException("suspend not supported");
    }

}