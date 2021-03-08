package org.raku.nqp.io;

import java.net.InetSocketAddress;
import inet.ipaddr.IPAddress;

/**
 * Stores an IP address of some sort for the Address REPR.
 */
public abstract class IPAddressStorage<T extends IPAddress> extends AddressStorage<InetSocketAddress> {
    protected final T ipAddress;

    IPAddressStorage(final T ipAddress, final int port) {
        super(new InetSocketAddress(ipAddress.toInetAddress(), port));
        this.ipAddress = ipAddress;
    }

    @Override
    public byte[] getBytes() {
        return ipAddress.getBytes();
    }

    /**
     * Gets the port associated with the stored IP socket address.
     */
    public final int getPort() {
        return address.getPort();
    }
}
