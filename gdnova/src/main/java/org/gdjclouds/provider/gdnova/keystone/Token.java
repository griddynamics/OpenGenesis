package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Objects;

import java.util.Date;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class Token implements Comparable<Token> {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().fromToken(this);
    }

    public static class Builder {
        protected String id;
        protected Date expires;
        protected Tenant tenant;

        /**
         * @see Token#getId()
         */
        public Builder id(String id) {
            this.id = checkNotNull(id, "id");
            return this;
        }

        /**
         * @see Token#getExpires()
         */
        public Builder expires(Date expires) {
            this.expires = checkNotNull(expires, "expires");
            return this;
        }

        /**
         * @see Token#getTenant()
         */
        public Builder tenant(Tenant tenant) {
            this.tenant = checkNotNull(tenant, "tenant");
            return this;
        }

        public Token build() {
            return new Token(id, expires, tenant);
        }

        public Builder fromToken(Token from) {
            return id(from.getId()).expires(from.getExpires()).tenant(from.getTenant());
        }
    }

    protected final String id;
    protected final Date expires;
    protected final Tenant tenant;

    public Token(String id, Date expires, Tenant tenant) {
        this.id = checkNotNull(id, "id");
        this.expires = checkNotNull(expires, "expires");
        this.tenant = checkNotNull(tenant, "tenant");
    }

    /**
     * When providing an ID, it is assumed that the token exists in the current OpenStack deployment
     *
     * @return the id of the token in the current OpenStack deployment
     */
    public String getId() {
        return id;
    }

    /**
     * @return the expires of the token
     */
    public Date getExpires() {
        return expires;
    }

    /**
     * @return the tenant assigned to the token
     */
    public Tenant getTenant() {
        return tenant;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Token) {
            final Token other = Token.class.cast(object);
            return equal(id, other.id) && equal(expires, other.expires) && equal(tenant, other.tenant);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, expires, tenant);
    }

    @Override
    public String toString() {
        return toStringHelper("").add("id", id).add("expires", expires).add("tenant", tenant).toString();
    }

    @Override
    public int compareTo(Token that) {
        if (that == null)
            return 1;
        if (this == that)
            return 0;
        return this.id.compareTo(that.id);
    }

}
