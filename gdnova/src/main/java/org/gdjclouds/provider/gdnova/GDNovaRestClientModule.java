package org.gdjclouds.provider.gdnova;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import org.gdjclouds.provider.gdnova.keystone.GDOpenStackAuthenticationModule;
import org.gdjclouds.provider.gdnova.v100.*;
import org.jclouds.date.DateService;
import org.jclouds.http.HttpErrorHandler;
import org.jclouds.http.HttpRetryHandler;
import org.jclouds.http.RequiresHttp;
import org.jclouds.http.annotation.ClientError;
import org.jclouds.http.annotation.Redirection;
import org.jclouds.http.annotation.ServerError;
import org.jclouds.json.config.GsonModule;
import org.jclouds.json.internal.EnumTypeAdapterThatReturnsFromValue;
import org.jclouds.openstack.OpenStackAuthAsyncClient;
import org.jclouds.openstack.nova.ServerManagement;
import org.jclouds.openstack.nova.domain.Address;
import org.jclouds.openstack.nova.domain.Addresses;
import org.jclouds.openstack.nova.domain.Flavor;
import org.jclouds.openstack.nova.handlers.ParseNovaErrorFromHttpResponse;
import org.jclouds.openstack.reference.AuthHeaders;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.config.RestClientModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;


@ConfiguresRestClient
@RequiresHttp
public class GDNovaRestClientModule extends RestClientModule<GDNovaClient, GDNovaAsyncClient> {
    private final GDOpenStackAuthenticationModule module;

    public GDNovaRestClientModule(GDOpenStackAuthenticationModule module) {
        super(GDNovaClient.class, GDNovaAsyncClient.class);
        this.module = module;
    }

    public GDNovaRestClientModule() {
        this(new GDOpenStackAuthenticationModule());
    }

    @Override
    protected void configure() {
        install(module);
        bind(GsonModule.DateAdapter.class).to(Iso8601DateAdapter.class);
        super.configure();
    }

    @Provides
    @Singleton
    public Map<Type, Object> provideCustomAdapterBindings() {
        return ImmutableMap.<Type, Object>of(
                Addresses.class, new AddressesAdapter(),
                Server.class, new ServerAdapter(),
                Image.class, new ImageAdapter(),
                Flavor.class, new FlavorAdapter()
        );
    }

    public static class AddressesAdapter implements JsonDeserializer<Addresses> {

        @Override
        public Addresses deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            Map<String, List<Map<String, String>>> deserialize
                    = new ObjectMapTypeAdapter().deserialize(jsonElement, new TypeToken<Map<String, List<Map<String, String>>>>() {}.getType(), jsonDeserializationContext);
            if(deserialize.isEmpty()) {
                return new Addresses(Collections.<Address>emptySet(), Collections.<Address>emptySet());
            }
            Set<Address> privateAddr = new HashSet<Address>();
            for (List<Map<String, String>> addresses: deserialize.values()) {
                for (Map<String, String> address : addresses) {
                    if(address.containsKey("addr")) {
                        privateAddr.add(Address.valueOf(address.get("addr")));
                    }
                }
            }
            return new Addresses(privateAddr, privateAddr);
        }
    }

    public static class ServerAdapter implements JsonDeserializer<Server> {
        @Override
        public Server deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            GsonBuilder builder = getGsonBuilder();
            Server server = builder.create().fromJson(jsonElement, org.gdjclouds.provider.gdnova.v100.Server.class);
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            if(asJsonObject.get("image") != null) {
                server.setImageRef(asJsonObject.get("image").getAsJsonObject().get("id").getAsString());
            }
            if(asJsonObject.get("flavor") != null) {
                server.setFlavorRef(asJsonObject.get("flavor").getAsJsonObject().get("id").getAsString());
            }

            return server;
        }
    }

    public static class ImageAdapter implements JsonDeserializer<Image> {
        @Override
        public Image deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            GsonBuilder builder = getGsonBuilder();

            return builder.create().fromJson(jsonElement, org.gdjclouds.provider.gdnova.v100.Image.class);
        }
    }

    public static class FlavorAdapter implements JsonDeserializer<Flavor> {
        @Override
        public Flavor deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            GsonBuilder builder = getGsonBuilder();
            return builder.create().fromJson(jsonElement, org.gdjclouds.provider.gdnova.v100.Flavor.class);
        }
    }

    private static GsonBuilder getGsonBuilder() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(Enum.class, new EnumTypeAdapterThatReturnsFromValue());
        builder.registerTypeHierarchyAdapter(Map.class, new ObjectMapTypeAdapter());
        builder.registerTypeAdapter(Addresses.class, new AddressesAdapter());
        return builder;
    }

    @Singleton
    public static class Iso8601DateAdapter implements GsonModule.DateAdapter {
        private final DateService dateService;

        @Inject
        private Iso8601DateAdapter(DateService dateService) {
            this.dateService = dateService;
        }

        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateService.iso8601DateFormat(src));
        }

        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String toParse = json.getAsJsonPrimitive().getAsString();
            if(!toParse.endsWith("Z")) {
                toParse = toParse + "Z";
            }
            try {
                return dateService.iso8601DateParse(toParse);
            } catch (RuntimeException e) {
                return dateService.iso8601SecondsDateParse(toParse);
            }
        }

    }

    @Override
    protected void bindErrorHandlers() {
        bind(HttpErrorHandler.class).annotatedWith(Redirection.class).to(ParseNovaErrorFromHttpResponse.class);
        bind(HttpErrorHandler.class).annotatedWith(ClientError.class).to(ParseNovaErrorFromHttpResponse.class);
        bind(HttpErrorHandler.class).annotatedWith(ServerError.class).to(ParseNovaErrorFromHttpResponse.class);
    }

    @Override
    protected void bindRetryHandlers() {
        bind(HttpRetryHandler.class).annotatedWith(ClientError.class).to(RetryOnRenew.class);
    }

    @Provides
    @Singleton
    @ServerManagement
    protected URI provideServerUrl(OpenStackAuthAsyncClient.AuthenticationResponse response) {
        return response.getServices().get(AuthHeaders.SERVER_MANAGEMENT_URL);
    }


}
