package org.raku.nqp.io;

import java.net.SocketAddress;

/**
 * Stores a socket address of some sort for the Address REPR.
 */
public abstract class AddressStorage<T extends SocketAddress> {
    protected final T address;

    AddressStorage(final T address) {
        this.address = address;
    }

    /**
     * Gets the protocol family associated with the stored socket address.
     */
    abstract public int getFamily();

    /**
     * Gets the stored socket address.
     */
    public final T getAddress() {
        return address;
    }

    /**
     * Gets a binary representation of the stored socket address.
     */
    abstract public byte[] getBytes();
}
