package org.raku.nqp.io;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import inet.ipaddr.AddressValueException;
import inet.ipaddr.ipv6.IPv6Address;
import static inet.ipaddr.ipv6.IPv6Address.IPv6Zone;

/**
 * Stores an IPv6 socket address for the Address REPR.
 */
public final class IPv6AddressStorage extends IPAddressStorage<IPv6Address> {
    IPv6AddressStorage(final IPv6Address ipAddress, final int port) {
        super(ipAddress, port);
    }

    @Override
    public int getFamily() {
        return ProtocolFamily.INET6;
    }

    /**
     * Gets the scope ID associated with the IPv6 socket address.
     */
    public int getScopeId() {
        return ipAddress.getIPv6Zone().getAssociatedScopeId();
    }

    /**
     * Stores a new IPv6 socket address created from a network address.
     */
    public static IPv6AddressStorage fromBytes(
        final byte[] networkAddress,
        final int    port,
        final int    scopeId
    ) throws AddressValueException {
        final IPv6Zone    zone      = new IPv6Zone(scopeId);
        final IPv6Address ipAddress = new IPv6Address(networkAddress, zone);
        return new IPv6AddressStorage(ipAddress, port);
    }
}
