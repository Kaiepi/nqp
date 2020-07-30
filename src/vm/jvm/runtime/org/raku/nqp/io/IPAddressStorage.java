package org.raku.nqp.io;

import java.net.InetAddress;

import org.raku.nqp.runtime.ThreadContext;

/**
 * Manages an IP address contained by the Address REPR.
 */
public abstract class IPAddressStorage<T extends InetAddress> extends AddressStorage<T> {
    protected final int port;

    IPAddressStorage(final SocketFamily family, final T address, final int port) {
        super(family, address);
        this.port = port;
    }

    /**
     * Gets the port of the stored IP address.
     */
    public final int getPort() {
        return port;
    }

    /**
     * Creates an IP literal for the stored IP address.
     */
    @Override
    public final String toString(final ThreadContext tc) {
        return address.getHostAddress();
    }
}
