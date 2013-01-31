package org.gdjclouds.provider.gdnova.v100;

import com.google.common.cache.Cache;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jclouds.domain.Credentials;
import org.jclouds.http.HttpCommand;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpRetryHandler;
import org.jclouds.logging.Logger;
import org.jclouds.openstack.OpenStackAuthAsyncClient;
import org.jclouds.openstack.reference.AuthHeaders;

import javax.annotation.Resource;

import static org.jclouds.http.HttpUtils.closeClientButKeepContentStream;
import static org.jclouds.http.HttpUtils.releasePayload;

@Singleton
public class RetryOnRenew implements HttpRetryHandler {
    @Resource
    protected Logger logger = Logger.NULL;

    private final Cache<Credentials, OpenStackAuthAsyncClient.AuthenticationResponse> authenticationResponseCache;

    @Inject
    protected RetryOnRenew(Cache<Credentials, OpenStackAuthAsyncClient.AuthenticationResponse> authenticationResponseCache) {
        this.authenticationResponseCache = authenticationResponseCache;
    }

    @Override
    public boolean shouldRetryRequest(HttpCommand command, HttpResponse response) {
        boolean retry = false; // default
        try {
            switch (response.getStatusCode()) {
                case 401:
                    // Do not retry on 401 from authentication request
                    Multimap<String, String> headers = command.getCurrentRequest().getHeaders();
                    if (headers != null && headers.containsKey(AuthHeaders.AUTH_USER)
                            && headers.containsKey(AuthHeaders.AUTH_KEY) && !headers.containsKey(AuthHeaders.AUTH_TOKEN)) {
                        retry = false;
                    } else {
                        byte[] content = closeClientButKeepContentStream(response);
                        if (content != null && new String(content).contains("lease renew")) {
                            logger.debug("invalidating authentication token");
                            authenticationResponseCache.invalidateAll();
                            retry = true;
                        } else {
                            if (authenticationResponseCache.size() > 0) {
                                authenticationResponseCache.invalidateAll();
                                retry = true;
                            } else {
                                retry = false;
                            }
                        }
                    }
                    break;
            }
            return retry;
        } finally {
            releasePayload(response);
        }
    }
}
