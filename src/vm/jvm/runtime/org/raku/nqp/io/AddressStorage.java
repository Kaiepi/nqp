package org.raku.nqp.io;

import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;

/**
 * Manages an address contained by the Address REPR.
 */
public abstract class AddressStorage<T> {
    protected final SocketFamily family;
    protected final T            address;

    AddressStorage(final SocketFamily family,final T address) {
        this.family  = family;
        this.address = address;
    }

    /**
     * Gets the family of the stored address.
     */
    public final SocketFamily getFamily() {
        return family;
    }

    /**
     * Gets the stored address.
     */
    public final T getAddress() {
        return address;
    }

    /**
     * Creates a string representing the stored address.
     */
    public abstract String toString(final ThreadContext tc);

    /**
     * Creates a VMArray buffer representing the stored address.
     */
    public abstract SixModelObject toBuffer(final ThreadContext tc, final SixModelObject bufType);
}
