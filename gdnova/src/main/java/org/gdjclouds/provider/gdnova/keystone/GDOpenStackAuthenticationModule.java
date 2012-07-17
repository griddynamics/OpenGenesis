package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.gdjclouds.provider.gdnova.v100.AccessWrapper;
import org.gdjclouds.provider.gdnova.v100.RetryOnTimeOutExceptionFunction;
import org.jclouds.Constants;
import org.jclouds.date.TimeStamp;
import org.jclouds.domain.Credentials;
import org.jclouds.http.RequiresHttp;
import org.jclouds.location.Provider;
import org.jclouds.openstack.Authentication;
import org.jclouds.rest.AsyncClientFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static com.google.common.base.Throwables.propagate;
import static org.jclouds.openstack.OpenStackAuthAsyncClient.AuthenticationResponse;

@RequiresHttp
public class GDOpenStackAuthenticationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<Function<Credentials, AuthenticationResponse>>() {
        }).to(GetAuthenticationResponse.class);
    }

    /**
     * borrowing concurrency code to ensure that caching takes place properly
     */
    @Provides
    @Singleton
    @Authentication
    protected Supplier<String> provideAuthenticationTokenCache(final Supplier<AuthenticationResponse> supplier)
            throws InterruptedException, ExecutionException, TimeoutException {
        return new Supplier<String>() {
            public String get() {
                return supplier.get().getAuthToken();
            }
        };
    }

    @Provides
    @Provider
    protected Credentials provideAuthenticationCredentials(@Named(Constants.PROPERTY_IDENTITY) String user,
                                                           @Named(Constants.PROPERTY_CREDENTIAL) String key) {
        return new Credentials(user, key);
    }

    @Singleton
    public static class GetAuthenticationResponse extends
            RetryOnTimeOutExceptionFunction<Credentials, AuthenticationResponse> {

        @Inject
        public GetAuthenticationResponse(final AsyncClientFactory factory) {
            super(new Function<Credentials, AuthenticationResponse>() {

                @Override
                public AuthenticationResponse apply(Credentials input) {
                    try {
                        String[] tenantAndName = input.identity.split(":");
                        if (tenantAndName.length == 1) {
                            tenantAndName = new String[] {"", tenantAndName[0]};
                        }
                        ListenableFuture<AccessWrapper> accessListenableFuture = factory.create(AuthServiceAsyncClient.class).
                                authenticateTenantWithCredentials(tenantAndName[0],
                                        new PasswordCredentials(tenantAndName[1], input.credential));
                        Access access = accessListenableFuture.get(30, TimeUnit.SECONDS).access;
                        Map<String, URI> services = new HashMap<String, URI>();
                        for (Service service : access.getServiceCatalog()) {
                            Set<Endpoint> endpoints = service.getEndpoints();
                            for (Endpoint endpoint : endpoints) {
                                if (service.getType().equals("compute")) {
                                    services.put("X-Server-Management-Url", endpoint.getPublicURL());
                                }
                                break;
                            }
                        }
                        return new AuthenticationResponse(access.getToken().getId(), services);
                    } catch (Exception e) {
                        throw propagate(e);
                    }
                }

                @Override
                public String toString() {
                    return "authenticate()";
                }
            });

        }
    }

    @Provides
    @Singleton
    public Cache<Credentials, AuthenticationResponse> provideAuthenticationResponseCache2(
            Function<Credentials, AuthenticationResponse> getAuthenticationResponse) {
        return CacheBuilder.newBuilder().expireAfterWrite(23, TimeUnit.HOURS).build(
                CacheLoader.from(getAuthenticationResponse));
    }

    @Provides
    @Singleton
    protected Supplier<AuthenticationResponse> provideAuthenticationResponseSupplier(
            final Cache<Credentials, AuthenticationResponse> cache, @Provider final Credentials creds) {
        return new Supplier<AuthenticationResponse>() {
            @Override
            public AuthenticationResponse get() {
                try {
                    return cache.get(creds);
                } catch (ExecutionException e) {
                    throw propagate(e.getCause());
                }
            }
        };
    }

    @Provides
    @Singleton
    @TimeStamp
    protected Supplier<Date> provideCacheBusterDate() {
        return memoizeWithExpiration(new Supplier<Date>() {
            public Date get() {
                return new Date();
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Provides
    @Singleton
    protected AuthenticationResponse provideAuthenticationResponse(Supplier<AuthenticationResponse> supplier)
            throws InterruptedException, ExecutionException, TimeoutException {
        return supplier.get();
    }

}
