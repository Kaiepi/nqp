package org.raku.nqp.io;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.AddressValueException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IPAddressStringParameters;
import inet.ipaddr.ipv6.IPv6Address;
import inet.ipaddr.ipv6.IPv6AddressStringParameters;
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
        final IPv6Zone zone = ipAddress.getIPv6Zone();
        return zone == null ? 0 : zone.getAssociatedScopeId();
    }

    @Override
    public String toString() {
        final String literal = super.toString();
        return ipAddress.getIPv6Zone() == null ?
               literal :
               literal.substring(0, literal.indexOf("%"));
    }

    static final IPAddressStringParameters PF_INET6;
    static final IPAddressStringParameters PF_INET6_ZONE;
    static {
        final IPAddressStringParameters.Builder   ipBuilder   = new IPAddressStringParameters.Builder();
        final IPv6AddressStringParameters.Builder ipv6Builder = ipBuilder.getIPv6AddressParametersBuilder();
        ipBuilder.allowIPv6(true);
        PF_INET6_ZONE = ipBuilder.toParams();
        ipv6Builder.allowZone(false);
        PF_INET6 = ipBuilder.toParams();
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

    /**
     * Stores a new IPv6 socket address created from a presentation-format string.
     */
    public static IPv6AddressStorage fromPresentation(
        final String presentation,
        final int    port,
        final String zoneId
    ) throws AddressStringException {
        final IPAddressString ipLiteral = (zoneId == null)
                                        ? new IPAddressString(presentation, PF_INET6)
                                        : new IPAddressString(presentation + "%" + zoneId, PF_INET6_ZONE);
        final IPAddress       ipAddress = ipLiteral.toAddress();
        return new IPv6AddressStorage(ipAddress.toIPv6(), port);
    }
}
