package org.gdjclouds.provider.gdnova;

import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import org.gdjclouds.provider.gdnova.keystone.GDOpenStackAuthenticationModule;
import org.gdjclouds.provider.gdnova.v100.GDNovaAsyncClient;
import org.gdjclouds.provider.gdnova.v100.GDNovaClient;
import org.gdjclouds.provider.gdnova.v100.GDNovaTemplateOptions;
import org.jclouds.compute.ComputeServiceContextBuilder;
import org.jclouds.compute.options.TemplateOptions;

import java.util.List;
import java.util.Properties;

public class GDNovaContextBuilder extends ComputeServiceContextBuilder<GDNovaClient, GDNovaAsyncClient> {

    public GDNovaContextBuilder(Properties props) {
        super(GDNovaClient.class, GDNovaAsyncClient.class, props);
    }

    @Provides
    public Provider<TemplateOptions> templateOptions() {
        return new Provider<TemplateOptions>() {
            @Override
            public TemplateOptions get() {
                return new GDNovaTemplateOptions();
            }
        };
    }


    @Override
    protected void addClientModule(List<Module> modules) {
        modules.add(new GDNovaRestClientModule(new GDOpenStackAuthenticationModule()));
    }


    @Override
    protected void addContextModule(List<Module> modules) {
        modules.add(new NovaComputeServiceContextModule());
    }

}
