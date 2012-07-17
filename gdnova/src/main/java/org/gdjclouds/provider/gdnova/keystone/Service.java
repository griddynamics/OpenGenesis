package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class Service implements Comparable<Service> {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().fromService(this);
    }

    public static class Builder {
        protected String type;
        protected String name;
        protected Set<Endpoint> endpoints = ImmutableSet.of();

        /**
         * @see Service#getId()
         */
        public Builder type(String type) {
            this.type = checkNotNull(type, "type");
            return this;
        }

        /**
         * @see Service#getName()
         */
        public Builder name(String name) {
            this.name = checkNotNull(name, "name");
            return this;
        }

        /**
         * @see Service#getEndpoints()
         */
        public Builder endpoints(Endpoint... endpoints) {
            return endpoints(ImmutableSet.copyOf(checkNotNull(endpoints, "endpoints")));
        }

        /**
         * @see Service#getEndpoints()
         */
        public Builder endpoints(Set<Endpoint> endpoints) {
            this.endpoints = ImmutableSet.copyOf(checkNotNull(endpoints, "endpoints"));
            return this;
        }

        public Service build() {
            return new Service(type, name, endpoints);
        }

        public Builder fromService(Service from) {
            return type(from.getType()).name(from.getName()).endpoints(from.getEndpoints());
        }
    }

    protected final String type;
    protected final String name;
    protected final Set<Endpoint> endpoints;

    public Service(String type, String name, Set<Endpoint> endpoints) {
        this.type = checkNotNull(type, "type");
        this.name = checkNotNull(name, "name");
        this.endpoints = ImmutableSet.copyOf(checkNotNull(endpoints, "endpoints"));
    }

    /**
     * such as {@code provider} (Nova), {@code object-store} (Swift), or {@code image} (Glance)
     *
     * @return the type of the service in the current OpenStack deployment
     */
    public String getType() {
        return type;
    }

    /**
     * @return the name of the service
     */
    public String getName() {
        return name;
    }

    /**
     * @return the endpoints assigned to the service
     */
    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Service) {
            final Service other = Service.class.cast(object);
            return equal(type, other.type) && equal(name, other.name) && equal(endpoints, other.endpoints);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, name, endpoints);
    }

    @Override
    public String toString() {
        return toStringHelper("").add("type", type).add("name", name).add("endpoints", endpoints).toString();
    }

    @Override
    public int compareTo(Service that) {
        if (that == null)
            return 1;
        if (this == that)
            return 0;
        return this.type.compareTo(that.type);
    }

}
