package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class Access implements Comparable<Access> {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().fromAccess(this);
    }

    public static class Builder {
        protected Token token;
        protected User user;
        protected Set<Service> serviceCatalog = ImmutableSet.of();

        /**
         * @see Access#getToken()
         */
        public Builder token(Token token) {
            this.token = checkNotNull(token, "token");
            return this;
        }

        /**
         * @see Access#getUser()
         */
        public Builder user(User user) {
            this.user = checkNotNull(user, "user");
            return this;
        }

        /**
         * @see Access#getServiceCatalog()
         */
        public Builder serviceCatalog(Service... serviceCatalog) {
            return serviceCatalog(ImmutableSet.copyOf(checkNotNull(serviceCatalog, "serviceCatalog")));
        }

        /**
         * @see Access#getServiceCatalog()
         */
        public Builder serviceCatalog(Set<Service> serviceCatalog) {
            this.serviceCatalog = ImmutableSet.copyOf(checkNotNull(serviceCatalog, "serviceCatalog"));
            return this;
        }

        public Access build() {
            return new Access(token, user, serviceCatalog);
        }

        public Builder fromAccess(Access from) {
            return token(from.getToken()).user(from.getUser()).serviceCatalog(from.getServiceCatalog());
        }
    }

    protected final Token token;
    protected final User user;
    protected final Set<Service> serviceCatalog;

    public Access(Token token, User user, Set<Service> serviceCatalog) {
        this.token = checkNotNull(token, "token");
        this.user = checkNotNull(user, "user");
        this.serviceCatalog = ImmutableSet.copyOf(checkNotNull(serviceCatalog, "serviceCatalog"));
    }

    /**
     * TODO
     */
    public Token getToken() {
        return token;
    }

    /**
     * TODO
     */
    public User getUser() {
        return user;
    }

    /**
     * TODO
     */
    public Set<Service> getServiceCatalog() {
        return serviceCatalog;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Access) {
            final Access other = Access.class.cast(object);
            return equal(token, other.token) && equal(user, other.user) && equal(serviceCatalog, other.serviceCatalog);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(token, user, serviceCatalog);
    }

    @Override
    public String toString() {
        return toStringHelper("").add("token", token).add("user", user).add("serviceCatalog", serviceCatalog).toString();
    }

    @Override
    public int compareTo(Access that) {
        if (that == null)
            return 1;
        if (this == that)
            return 0;
        return this.token.compareTo(that.token);
    }

}
