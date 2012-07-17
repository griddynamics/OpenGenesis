package org.gdjclouds.provider.gdnova.keystone;

import com.google.common.base.Objects;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class PasswordCredentials {
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().fromPasswordCredentials(this);
    }

    public static PasswordCredentials createWithUsernameAndPassword(String username, String password) {
        return builder().password(password).username(username).build();
    }

    public static class Builder {
        protected String username;
        protected String password;

        /**
         * @see PasswordCredentials#getUsername()
         */
        protected Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * @see PasswordCredentials#getPassword()
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public PasswordCredentials build() {
            return new PasswordCredentials(username, password);
        }

        public Builder fromPasswordCredentials(PasswordCredentials from) {
            return username(from.getUsername()).password(from.getPassword());
        }
    }

    protected final String username;
    protected final String password;

    protected PasswordCredentials(String username, String password) {
        this.username = checkNotNull(username, "username");
        this.password = checkNotNull(password, "password");
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof PasswordCredentials) {
            final PasswordCredentials other = PasswordCredentials.class.cast(object);
            return equal(username, other.username) && equal(password, other.password);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username, password);
    }

    @Override
    public String toString() {
        return toStringHelper("").add("username", username).add("password", password).toString();
    }

}
