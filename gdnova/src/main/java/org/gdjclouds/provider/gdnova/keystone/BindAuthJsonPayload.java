package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.jclouds.http.HttpRequest;
import org.jclouds.json.Json;
import org.jclouds.rest.MapBinder;
import org.jclouds.rest.binders.BindToJsonPayload;
import org.jclouds.rest.internal.GeneratedHttpRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

@Singleton
public class BindAuthJsonPayload extends BindToJsonPayload implements MapBinder {
    @Inject
    public BindAuthJsonPayload(Json jsonBinder) {
        super(jsonBinder);
    }

    @Override
    public <R extends HttpRequest> R bindToRequest(R request, Object toBind) {
        throw new IllegalStateException("BindAuthToJsonPayload needs parameters");
    }

    protected void addCredentialsInArgsOrNull(GeneratedHttpRequest<?> gRequest, ImmutableMap.Builder<String, Object> builder) {
        for (Object arg : gRequest.getArgs()) {
            if (!(arg instanceof String)) {
                builder.put("passwordCredentials", PasswordCredentials.class.cast(arg));
            }
        }
    }

    @Override
    public <R extends HttpRequest> R bindToRequest(R request, Map<String, String> postParams) {
        checkArgument(checkNotNull(request, "request") instanceof GeneratedHttpRequest<?>,
                "this binder is only valid for GeneratedHttpRequests!");
        GeneratedHttpRequest<?> gRequest = (GeneratedHttpRequest<?>) request;
        checkState(gRequest.getArgs() != null, "args should be initialized at this point");

        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();
        addCredentialsInArgsOrNull(gRequest, builder);
        // TODO: is tenantName permanent? or should we switch to tenantId at some point. seems most tools
        // still use tenantName
        if (Strings.emptyToNull(postParams.get("tenantName")) != null)
            builder.put("tenantName", postParams.get("tenantName"));
        return super.bindToRequest(request, ImmutableMap.of("auth", builder.build()));
    }

}
