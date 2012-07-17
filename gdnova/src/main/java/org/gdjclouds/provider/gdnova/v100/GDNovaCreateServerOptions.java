package org.gdjclouds.provider.gdnova.v100;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jclouds.encryption.internal.Base64;
import org.jclouds.http.HttpRequest;
import org.jclouds.openstack.nova.options.CreateServerOptions;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class GDNovaCreateServerOptions extends CreateServerOptions {
    static class File {
        private final String path;
        private final String contents;

        public File(String path, byte[] contents) {
            this.path = checkNotNull(path, "path");
            this.contents = Base64.encodeBytes(checkNotNull(contents, "contents"));
            checkArgument(path.getBytes().length < 255, String.format(
                    "maximum length of path is 255 bytes.  Path specified %s is %d bytes", path, path.getBytes().length));
            checkArgument(contents.length < 10 * 1024, String.format(
                    "maximum size of the file is 10KB.  Contents specified is %d bytes", contents.length));
        }

        public String getContents() {
            return contents;
        }

        public String getPath() {
            return path;
        }

    }


    private Map<String, String> metadata = Maps.newHashMap();
    private List<File> files = Lists.newArrayList();
    private String keyPair;

    public GDNovaCreateServerOptions withKeyPair(String keyPair) {
        this.keyPair = keyPair;
        return this;
    }

    @Override
    public <R extends HttpRequest> R bindToRequest(R request, Map<String, String> postParams) {
        ServerRequest server = new ServerRequest(checkNotNull(postParams.get("name"), "name parameter not present"),
                checkNotNull(postParams.get("imageRef"), "imageRef parameter not present"), checkNotNull(postParams
                .get("flavorRef"), "flavorRef parameter not present"));
        if (metadata.size() > 0)
            server.metadata = metadata;
        if (files.size() > 0)
            server.personality = files;
        server.key_name = keyPair;
        return bindToRequest(request, ImmutableMap.of("server", server));
    }

    @SuppressWarnings("unused")
    private class ServerRequest {
        final String name;
        final String imageRef;
        final String flavorRef;
        Map<String, String> metadata;
        List<File> personality;
        String key_name;

        private ServerRequest(String name, String imageRef, String flavorRef) {
            this.name = name;
            this.imageRef = imageRef;
            this.flavorRef = flavorRef;
        }

    }

}
