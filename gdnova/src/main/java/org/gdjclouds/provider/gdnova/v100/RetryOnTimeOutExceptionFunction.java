package org.gdjclouds.provider.gdnova.v100;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingObject;
import org.jclouds.util.Throwables2;

import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * @author Adrian Cole
 */
public class RetryOnTimeOutExceptionFunction<K, V> extends ForwardingObject implements Function<K, V> {
    private final Function<K, V> delegate;

    public RetryOnTimeOutExceptionFunction(Function<K, V> delegate) {
        this.delegate = checkNotNull(delegate, "delegate");
    }

    //TODO: backoff limited retry handler
    @Override
    public V apply(K key) {
        TimeoutException ex = null;
        for (int i = 0; i < 3; i++) {
            try {
                ex = null;
                return delegate().apply(key);
            } catch (Exception e) {
                if ((ex = Throwables2.getFirstThrowableOfType(e, TimeoutException.class)) != null)
                    continue;
                throw propagate(e);
            }
        }
        if (ex != null)
            throw propagate(ex);
        assert false;
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    protected Function<K, V> delegate() {
        return delegate;
    }

}