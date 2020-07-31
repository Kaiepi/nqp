package org.raku.nqp.io;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.Ops;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

/**
 * Manages a DNS resolution context. DNS resolution may be performed by the OS
 * or via manual DNS queries.
 */
public class ResolutionContext {
    private static final SocketFamily PF_UNSPEC = SocketFamily.getByName("PF_UNSPEC");
    private static final SocketFamily PF_INET   = SocketFamily.getByName("PF_INET");
    private static final SocketFamily PF_INET6  = SocketFamily.getByName("PF_INET6");

    private static final SocketType SOCK_ANY    = SocketType.getByName("SOCK_ANY");
    private static final SocketType SOCK_STREAM = SocketType.getByName("SOCK_STREAM");
    private static final SocketType SOCK_DGRAM  = SocketType.getByName("SOCK_DGRAM");
    private static final SocketType SOCK_RAW    = SocketType.getByName("SOCK_RAW");

    private static final SocketProtocol IPPROTO_ANY = SocketProtocol.getByName("IPPROTO_ANY");
    private static final SocketProtocol IPPROTO_TCP = SocketProtocol.getByName("IPPROTO_TCP");
    private static final SocketProtocol IPPROTO_UDP = SocketProtocol.getByName("IPPROTO_UDP");

    /**
     * Represents a solution for DNS resolution family/type/protocol hints for
     * the resolve method. This contains logic borrowed from Linux, FreeBSD,
     * and OpenBSD's implementations of getaddrinfo(3).
     */
    private static final class Solution {
        private final SocketFamily   family;
        private final SocketType     type;
        private final SocketProtocol protocol;

        Solution(final SocketFamily family, final SocketType type, final SocketProtocol protocol) {
            this.family   = family;
            this.type     = type;
            this.protocol = protocol;
        }

        public final SocketFamily getFamily() {
            return family;
        }

        public final SocketType getType() {
            return type;
        }

        public final SocketProtocol getProtocol() {
            return protocol;
        }

        private final Boolean accepts(
            final SocketFamily   family,
            final SocketType     type,
            final SocketProtocol protocol
        ) {
            return (family.equals(PF_UNSPEC) || family.equals(this.family))
                && (type.equals(SOCK_ANY) && !this.type.equals(SOCK_RAW) || type.equals(this.type))
                && (protocol.equals(IPPROTO_ANY) || protocol.equals(this.protocol));
        }

        private static final List<Solution> solutions = new ArrayList<>() {{
            final SocketFamily[] families;
            if (System.getProperty("java.net.preferIPv4Stack").equals("true")) {
                families = new SocketFamily[] { PF_INET, PF_INET6 };
            } else {
                families = new SocketFamily[] { PF_INET6, PF_INET };
            }
            for (SocketFamily family : families) {
                add(new Solution(family, SOCK_DGRAM,  IPPROTO_UDP));
                add(new Solution(family, SOCK_STREAM, IPPROTO_TCP));
                add(new Solution(family, SOCK_RAW,    IPPROTO_ANY));
            }
        }};

        public static final Solution[] findAll(
            final SocketFamily   family,
            final SocketType     type,
            final SocketProtocol protocol
        ) {
            return solutions.stream()
                            .filter(s -> s.accepts(family, type, protocol))
                            .toArray(Solution[]::new);
        }
    }

    /**
     * Performs a native DNS resolution, returning a list of address info
     * tuples. The hostname given may be null, which will result in an
     * appropriate local address.
     */
    public static final SixModelObject resolve(
        final ThreadContext  tc,
              String         hostname,
        final int            port,
        final SocketFamily   family,
        final SocketType     type,
        final SocketProtocol protocol,
        final Boolean        isPassive
    ) {
        if (!family.equals(PF_UNSPEC) && !family.equals(PF_INET) && !family.equals(PF_INET6)) {
            throw ExceptionHandling.dieInternal(tc,
                "dnsresolve socket family must be PF_UNSPEC, PF_INET, or PF_INET6");
        }
        if (!type.equals(SOCK_ANY) && !type.equals(SOCK_DGRAM) && !type.equals(SOCK_STREAM) && !type.equals(SOCK_RAW)) {
            throw ExceptionHandling.dieInternal(tc,
                "dnsresolve socket type must be SOCK_DGRAM, SOCK_STREAM, or SOCK_RAW");
        }
        if (protocol.equals(SOCK_RAW) && port != 0) {
            throw ExceptionHandling.dieInternal(tc,
                "dnsresolve cannot accept a port when resolving addresses for raw sockets");
        }

        if (hostname == null) {
            if (family.equals(PF_INET)) {
                hostname = isPassive ? "0.0.0.0" : "127.0.0.1";
            } else if (family.equals(PF_INET6)) {
                hostname = isPassive ? "::" : "::1";
            } else if (System.getProperty("java.net.preferIPv4Stack").equals("true")) {
                hostname = isPassive ? "0.0.0.0" : "127.0.0.1";
            } else {
                hostname = isPassive ? "::" : "::1";
            }
        }

        final InetAddress[] nativeAddresses;
        final Solution[]    solutions;
        try {
            nativeAddresses = InetAddress.getAllByName(hostname);
            solutions       = Solution.findAll(family, type, protocol);
        } catch (UnknownHostException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        } catch (SecurityException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }

        final SixModelObject BOOTArray   = tc.gc.BOOTArray;
        final SixModelObject BOOTAddress = tc.gc.BOOTAddress;
        final SixModelObject BOOTInt     = tc.gc.BOOTInt;
        final SixModelObject result      = BOOTArray.st.REPR.allocate(tc, BOOTArray.st);
        for (final InetAddress nativeAddress : nativeAddresses) {
            final AddressInstance address = (AddressInstance)BOOTAddress.st.REPR.allocate(tc, BOOTAddress.st);
            if (nativeAddress instanceof Inet6Address) {
                if (family.equals(PF_INET)) continue;
                address.storage = new IPv6AddressStorage((Inet6Address)nativeAddress, port);
            } else {
                if (family.equals(PF_INET6)) continue;
                address.storage = new IPv4AddressStorage((Inet4Address)nativeAddress, port);
            }

            final SocketFamily   addressFamily    = address.storage.getFamily();
            final SixModelObject addressFamilyBox = Ops.box_i((long)addressFamily.getValue(), BOOTInt, tc);
            for (final Solution solution : solutions) {
                if (solution.getFamily().equals(addressFamily)) {
                    final SixModelObject addressInfo = BOOTArray.st.REPR.allocate(tc, BOOTArray.st);
                    final SixModelObject familyBox   = Ops.box_i((long)solution.getFamily().getValue(), BOOTInt, tc);
                    final SixModelObject typeBox     = Ops.box_i((long)solution.getType().getValue(), BOOTInt, tc);
                    final SixModelObject protocolBox = Ops.box_i((long)solution.getProtocol().getValue(), BOOTInt, tc);
                    addressInfo.push_boxed(tc, addressFamilyBox);
                    addressInfo.push_boxed(tc, address);
                    addressInfo.push_boxed(tc, familyBox);
                    addressInfo.push_boxed(tc, typeBox);
                    addressInfo.push_boxed(tc, protocolBox);
                    result.push_boxed(tc, addressInfo);
                }
            }
        }
        return result;
    }
}
