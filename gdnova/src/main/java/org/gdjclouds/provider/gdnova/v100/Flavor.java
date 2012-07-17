package org.gdjclouds.provider.gdnova.v100;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class Flavor extends org.jclouds.openstack.nova.domain.Flavor {
    private List<Map<String, String>> links = Lists.newArrayList();

    @Override
    public URI getURI() {
        for (Map<String, String> linkProperties : links) {
            try {
                if (!Functions.forMap(linkProperties, "").apply("rel").equals("bookmark"))
                    continue;
                return new URI(linkProperties.get("href"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException("URI is not available");

    }
}
