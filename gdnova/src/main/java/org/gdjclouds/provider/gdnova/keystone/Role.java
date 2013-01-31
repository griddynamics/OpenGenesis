package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

import javax.annotation.Nullable;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class Role implements Comparable<Role> {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().fromRole(this);
    }

    public static class Builder {
        protected String id;
        protected String name;
        protected String serviceId;
        protected String tenantId;

        /**
         * @see Role#getId()
         */
        public Builder id(String id) {
            this.id = checkNotNull(id, "id");
            return this;
        }

        /**
         * @see Role#getName()
         */
        public Builder name(String name) {
            this.name = checkNotNull(name, "name");
            return this;
        }

        /**
         * @see Role#getServiceId()
         */
        public Builder serviceId(@Nullable String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        /**
         * @see Role#getTenantId()
         */
        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Role build() {
            return new Role(id, name, serviceId, tenantId);
        }

        public Builder fromRole(Role from) {
            return id(from.getId()).name(from.getName()).serviceId(from.getServiceId()).tenantId(from.getTenantId());
        }
    }

    protected final String id;
    protected final String name;
    protected final String serviceId;
    // renamed half-way through
    @Deprecated
    protected String tenantName;
    protected final String tenantId;

    protected Role(String id, String name, @Nullable String serviceId, @Nullable String tenantId) {
        this.id = checkNotNull(id, "id");
        this.name = checkNotNull(name, "name");
        this.serviceId = serviceId;
        this.tenantId = tenantId;
    }

    /**
     * When providing an ID, it is assumed that the role exists in the current OpenStack deployment
     *
     * @return the id of the role in the current OpenStack deployment
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name of the role
     */
    public String getName() {
        return name;
    }

    /**
     * @return the service id of the role or null, if not present
     */
    @Nullable
    public String getServiceId() {
        return serviceId;
    }

    /**
     * @return the tenant id of the role or null, if not present
     */
    @Nullable
    public String getTenantId() {
        return tenantId != null ? tenantId : tenantName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Role) {
            final Role other = Role.class.cast(object);
            return equal(id, other.id) && equal(name, other.name) && equal(serviceId, other.serviceId)
                    && equal(getTenantId(), other.getTenantId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, serviceId, getTenantId());
    }

    @Override
    public String toString() {
        return toStringHelper("").add("id", id).add("name", name).add("serviceId", serviceId).add("tenantId", getTenantId())
                .toString();
    }

    @Override
    public int compareTo(Role that) {
        return ComparisonChain.start().compare(this.id, that.id).result();
    }

}
