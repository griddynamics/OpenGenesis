package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class User implements Comparable<User> {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().fromUser(this);
    }

    public static class Builder {
        protected String id;
        protected String name;
        protected Set<Role> roles = ImmutableSet.of();

        /**
         * @see User#getId()
         */
        public Builder id(String id) {
            this.id = checkNotNull(id, "id");
            return this;
        }

        /**
         * @see User#getName()
         */
        public Builder name(String name) {
            this.name = checkNotNull(name, "name");
            return this;
        }

        /**
         * @see User#getRoles()
         */
        public Builder roles(Role... roles) {
            return roles(ImmutableSet.copyOf(checkNotNull(roles, "roles")));
        }

        /**
         * @see User#getRoles()
         */
        public Builder roles(Set<Role> roles) {
            this.roles = ImmutableSet.copyOf(checkNotNull(roles, "roles"));
            return this;
        }

        public User build() {
            return new User(id, name, roles);
        }

        public Builder fromUser(User from) {
            return id(from.getId()).name(from.getName()).roles(from.getRoles());
        }
    }

    protected final String id;
    protected final String name;
    protected final Set<Role> roles;

    public User(String id, String name, Set<Role> roles) {
        this.id = checkNotNull(id, "id");
        this.name = checkNotNull(name, "name");
        this.roles = ImmutableSet.copyOf(checkNotNull(roles, "roles"));
    }

    /**
     * When providing an ID, it is assumed that the user exists in the current OpenStack deployment
     *
     * @return the id of the user in the current OpenStack deployment
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * @return the roles assigned to the user
     */
    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof User) {
            final User other = User.class.cast(object);
            return equal(id, other.id) && equal(name, other.name) && equal(roles, other.roles);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, roles);
    }

    @Override
    public String toString() {
        return toStringHelper("").add("id", id).add("name", name).add("roles", roles).toString();
    }

    @Override
    public int compareTo(User that) {
        if (that == null)
            return 1;
        if (this == that)
            return 0;
        return this.id.compareTo(that.id);
    }

}
