package org.raku.nqp.io;

import java.net.Inet4Address;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.AddressValueException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IPAddressStringParameters;
import inet.ipaddr.ipv4.IPv4Address;

/**
 * Stores an IPv4 socket address for the Address REPR.
 */
public final class IPv4AddressStorage extends IPAddressStorage<IPv4Address> {
    IPv4AddressStorage(final IPv4Address ipAddress, final int port) {
        super(ipAddress, port);
    }

    IPv4AddressStorage(final Inet4Address nativeAddress, final int port) {
        super(new IPv4Address(nativeAddress), port);
    }

    @Override
    public int getFamily() {
        return ProtocolFamily.INET;
    }

    static final IPAddressStringParameters PF_INET;
    static {
        final IPAddressStringParameters.Builder ipBuilder = new IPAddressStringParameters.Builder();
        ipBuilder.allowIPv4(true);
        PF_INET = ipBuilder.toParams();
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

    /**
     * Stores a new IPv4 socket address created from a presentation-format string.
     */
    public static IPv4AddressStorage fromPresentation(
        final String presentation,
        final int    port
    ) throws AddressStringException {
        final IPAddressString ipLiteral = new IPAddressString(presentation, PF_INET);
        final IPAddress       ipAddress = ipLiteral.toAddress();
        return new IPv4AddressStorage(ipAddress.toIPv4(), port);
    }
}
