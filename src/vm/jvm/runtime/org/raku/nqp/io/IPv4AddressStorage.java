package org.raku.nqp.io;

import inet.ipaddr.AddressValueException;
import inet.ipaddr.ipv4.IPv4Address;

/**
 * Stores an IPv4 socket address for the Address REPR.
 */
public final class IPv4AddressStorage extends IPAddressStorage<IPv4Address> {
    IPv4AddressStorage(final IPv4Address ipAddress, final int port) {
        super(ipAddress, port);
    }

    @Override
    public int getFamily() {
        return ProtocolFamily.INET;
    }

    /**
     * Stores a new IPv4 socket address created from its network address.
     */
    public static IPv4AddressStorage fromBytes(
        final byte[] networkAddress,
        final int    port
    ) throws AddressValueException {
        final IPv4Address ipAddress = new IPv4Address(networkAddress);
        return new IPv4AddressStorage(ipAddress, port);
    }
}
