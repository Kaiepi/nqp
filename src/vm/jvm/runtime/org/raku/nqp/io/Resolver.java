package org.raku.nqp.io;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.Ops;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

/**
 * Performs DNS resolutions.
 */
public final class Resolver {
    /**
     * Represents a solution for DNS resolution family/type/protocol hints for
     * the resolve method. This contains logic borrowed from OpenBSD's
     * implementations of getaddrinfo(3).
     */
    static final class Solution {
        public final int family;
        public final int type;
        public final int protocol;

        Solution(final int family, final int type, final int protocol) {
            this.family   = family;
            this.type     = type;
            this.protocol = protocol;
        }

        final boolean accepts(final int family, final int type, final int protocol) {
            return (family == ProtocolFamily.UNSPEC || family == this.family)
                && ((type == SocketType.ANY && this.type != SocketType.RAW) || type == this.type)
                && (protocol == ProtocolType.ANY || protocol == this.protocol);
        }

        static final List<Solution> solutions = new ArrayList<>() {{
            final int[] families;
            if (System.getProperty("java.net.preferIPv4Stack").equals("true")) {
                families = new int[] { ProtocolFamily.INET, ProtocolFamily.INET6 };
            } else {
                families = new int[] { ProtocolFamily.INET6, ProtocolFamily.INET };
            }
            for (final int family : families) {
                add(new Solution(family, SocketType.DGRAM,  ProtocolType.UDP));
                add(new Solution(family, SocketType.STREAM, ProtocolType.TCP));
                add(new Solution(family, SocketType.RAW,    ProtocolType.ANY));
            }
        }};

        public static List<Solution> findAll(final int family, final int type, final int protocol) {
            return solutions.stream()
                            .filter(s -> s.accepts(family, type, protocol))
                            .collect(Collectors.toList());
        }
    }

    /**
     * Performs a native DNS resolution, returning a list of address info
     * tuples. The hostname given may be null, which will result in an
     * appropriate local address.
     */
    public static final SixModelObject lookup(
        final ThreadContext  tc,
        final String         hostname,
        final int            port,
        final int            family,
        final int            type,
        final int            protocol,
        final long           flags
    ) {
        if (family != ProtocolFamily.UNSPEC && family != ProtocolFamily.INET && family != ProtocolFamily.INET6)
            throw ExceptionHandling.dieInternal(tc,
                "dnsresolve socket family must be PF_UNSPEC, PF_INET, or PF_INET6");
        if (type != SocketType.ANY && type != SocketType.DGRAM && type != SocketType.STREAM && type != SocketType.RAW)
            throw ExceptionHandling.dieInternal(tc,
                "dnsresolve socket type must be SOCK_DGRAM, SOCK_STREAM, or SOCK_RAW");
        if (type == SocketType.RAW && port != 0)
            throw ExceptionHandling.dieInternal(tc,
                "dnsresolve cannot accept a port when resolving addresses for raw sockets");

        /* Determine what level of support for PF_INET and PF_INET6 exists
         * locally. */
        final boolean addressConfig = (flags & 0b01) == 0b01;
              boolean supportsInet;
              boolean supportsInet6;
        if (addressConfig)
            try {
                final List<InetAddress> addresses = Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .flatMap(nif -> Collections.list(nif.getInetAddresses()).stream())
                    .collect(Collectors.toList());
                supportsInet  = addresses.stream().anyMatch(address -> address instanceof Inet4Address);
                supportsInet6 = addresses.stream().anyMatch(address -> address instanceof Inet6Address);
            } catch (final SocketException e) {
                supportsInet  = false;
                supportsInet6 = false;
            }
        else {
            supportsInet  = true;
            supportsInet6 = true;
        }

        /* A null hostname, if passed directly to InetAddress#getAllByName,
         * only resolves loopback addresses. We need the unspecified ones too! */
        final List<String> hostnames = new ArrayList<>();
        if (hostname == null) {
            final boolean isPassive = (flags & 0b10) == 0b10;
            if (family == ProtocolFamily.INET) {
                hostnames.add(isPassive ? "0.0.0.0" : "127.0.0.1");
            } else if (family == ProtocolFamily.INET6) {
                hostnames.add(isPassive ? "::" : "::1");
            } else {
                if (supportsInet6)
                    hostnames.add(isPassive ? "::" : "::1");
                if (supportsInet)
                    hostnames.add(isPassive ? "0.0.0.0" : "127.0.0.1");
            }
        }
        else
            hostnames.add(hostname);

        /* Perform the DNS resolution. */
        final List<InetAddress> nativeAddresses = new ArrayList<>();
        final List<Solution>    solutions       = Solution.findAll(family, type, protocol);
        for (final String hn : hostnames)
            try {
                for (final InetAddress nativeAddress : InetAddress.getAllByName(hn))
                    nativeAddresses.add(nativeAddress);
            } catch (final Exception e) {
                throw ExceptionHandling.dieInternal(tc, "Error resolving hostname: " + e.getMessage());
            }

        /* Box and return the addresses received. */
        final SixModelObject BOOTArray   = tc.gc.BOOTArray;
        final SixModelObject BOOTAddress = tc.gc.BOOTAddress;
        final SixModelObject BOOTInt     = tc.gc.BOOTInt;
        final SixModelObject result      = BOOTArray.st.REPR.allocate(tc, BOOTArray.st);
        for (final InetAddress nativeAddress : nativeAddresses) {
            final AddressInstance address = (AddressInstance)BOOTAddress.st.REPR.allocate(tc, BOOTAddress.st);
            if (nativeAddress instanceof Inet6Address) {
                if (family == ProtocolFamily.INET) continue;
                if (!supportsInet6) continue;
                address.storage = new IPv6AddressStorage((Inet6Address)nativeAddress, port);
            } else {
                if (family == ProtocolFamily.INET6) continue;
                if (!supportsInet) continue;
                address.storage = new IPv4AddressStorage((Inet4Address)nativeAddress, port);
            }

            final int addressFamily = address.storage.getFamily();
            for (final Solution solution : solutions) {
                if (solution.family == addressFamily) {
                    result.push_boxed(tc, Ops.box_i((long)addressFamily, BOOTInt, tc));
                    result.push_boxed(tc, address);
                    result.push_boxed(tc, Ops.box_i((long)solution.family, BOOTInt, tc));
                    result.push_boxed(tc, Ops.box_i((long)solution.type, BOOTInt, tc));
                    result.push_boxed(tc, Ops.box_i((long)solution.protocol, BOOTInt, tc));
                }
            }
        }
        return result;
    }
}
