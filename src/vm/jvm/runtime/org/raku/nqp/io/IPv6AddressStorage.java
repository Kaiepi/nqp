package org.raku.nqp.io;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.VMArrayREPRData;
import org.raku.nqp.sixmodel.reprs.VMArrayInstance_i;
import org.raku.nqp.sixmodel.reprs.VMArrayInstance_u8;
import org.raku.nqp.sixmodel.reprs.VMArrayInstance_u16;
import org.raku.nqp.sixmodel.reprs.VMArrayInstance_u32;

public final class IPv6AddressStorage extends IPAddressStorage<Inet6Address> {
    public IPv6AddressStorage(final Inet6Address address, final int port) {
        super(SocketFamily.getByName("PF_INET6"), address, port);
    }

    /**
     * Gets the scope ID of the stored IPv6 address.
     */
    public final int getScopeId() {
        return address.getScopedInterface().getIndex();
    }

    /**
     * Creates a VMArray buffer for the stored IPv6 address' network address.
     * The buffer type's REPR must be confirmed to be VMArray before being
     * passed to this.
     */
    @Override
    public final SixModelObject toBuffer(final ThreadContext tc, final SixModelObject bufType) {
        final SixModelObject bufObj = bufType.st.REPR.allocate(tc, bufType.st);
        final ByteBuffer     bb     = ByteBuffer.allocate(16);
        bb.put(address.getAddress());
        bb.rewind();
        if (bufObj instanceof VMArrayInstance_u8) {
            final VMArrayInstance_u8 buf = (VMArrayInstance_u8)bufObj;
            buf.elems = 16;
            buf.slots = new byte[16];
            bb.get(buf.slots);
            return buf;
        } else if (bufObj instanceof VMArrayInstance_u16) {
            final VMArrayInstance_u16 buf = (VMArrayInstance_u16)bufObj;
            buf.elems = 8;
            buf.slots = new short[8];
            bb.asShortBuffer().get(buf.slots);
            return buf;
        } else if (bufObj instanceof VMArrayInstance_u32) {
            final VMArrayInstance_u32 buf = (VMArrayInstance_u32)bufObj;
            buf.elems = 4;
            buf.slots = new int[4];
            bb.asIntBuffer().get(buf.slots);
            return buf;
        } else if (bufObj instanceof VMArrayInstance_i && ((VMArrayREPRData)bufObj.st.REPRData).ss.is_unsigned != 0) {
            final VMArrayInstance_i buf = (VMArrayInstance_i)bufObj;
            buf.elems = 2;
            buf.slots = new long[2];
            bb.asLongBuffer().get(buf.slots);
            return buf;
        } else {
            throw ExceptionHandling.dieInternal(tc,
                "IPv6 address buffer type must be an array of uint8, uint16, uint32, uint64");
        }
    }

    /**
     * Creates storage for an IPv4 address from an IPv4 literal.
     */
    public static final IPv6AddressStorage fromString(final ThreadContext tc, final String literal, final int port) {
        try {
            final InetAddress address = InetAddress.getByName(literal);
            if (address instanceof Inet6Address) {
                return new IPv6AddressStorage((Inet6Address)address, port);
            } else {
                throw new UnknownHostException(literal);
            }
        } catch (UnknownHostException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    /**
     * Creates storage for an IPv6 address from a JVM buffer.
     */
    public static final IPv6AddressStorage fromNativeBuffer(
        final ThreadContext tc,
        final String        hostname,
        final byte[]        raw,
        final int           port,
        final int           scopeId
    ) {
        /* InetAddress.getHostAddress will include the scope ID instead of the
         * interface name in literals if we create an Inet6Address with the
         * scope ID itself.  This doesn't match MoarVM's behaviour, so we need
         * to get the interface for the scope ID given and use that instead: */
        final NetworkInterface nif;
        try {
            nif = NetworkInterface.getByIndex(scopeId);
        } catch (SocketException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        } catch (IllegalArgumentException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }

        try {
            final Inet6Address address = Inet6Address.getByAddress(hostname, raw, nif);
            return new IPv6AddressStorage(address, port);
        } catch (UnknownHostException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    /**
     * Creates storage for an IPv6 address from a VMArray buffer. The buffer's
     * REPR must be confirmed to be VMArray before being passed to this.
     */
    public static final IPv6AddressStorage fromBuffer(
        final ThreadContext  tc,
        final SixModelObject obj,
        final int            port,
        final int            scopeId
    ) {
        final ByteBuffer bb = ByteBuffer.allocate(16);
        if (obj instanceof VMArrayInstance_u8) {
            final VMArrayInstance_u8 buf = (VMArrayInstance_u8)obj;
            if (buf.elems == 16) {
                bb.put(buf.slots, 0, buf.elems);
            } else {
                throw ExceptionHandling.dieInternal(tc, "IPv6 address uint8 buffer must have 16 elements");
            }
        } else if (obj instanceof VMArrayInstance_u16) {
            final VMArrayInstance_u16 buf = (VMArrayInstance_u16)obj;
            if (buf.elems == 8) {
                bb.asShortBuffer().put(buf.slots, 0, buf.elems);
            } else {
                throw ExceptionHandling.dieInternal(tc, "IPv6 address uint16 buffer must have 8 elements");
            }
        } else if (obj instanceof VMArrayInstance_u32) {
            final VMArrayInstance_u32 buf = (VMArrayInstance_u32)obj;
            if (buf.elems == 4) {
                bb.asIntBuffer().put(buf.slots, 0, buf.elems);
            } else {
                throw ExceptionHandling.dieInternal(tc, "IPv6 address uint32 buffer must have 4 elements");
            }
        } else if (obj instanceof VMArrayInstance_i && ((VMArrayREPRData)obj.st.REPRData).ss.is_unsigned != 0) {
            final VMArrayInstance_i buf = (VMArrayInstance_i)obj;
            if (buf.elems == 2) {
                bb.asLongBuffer().put(buf.slots, 0, buf.elems);
            } else {
                throw ExceptionHandling.dieInternal(tc, "IPv6 address uint64 buffer must have 2 elements");
            }
        } else {
            throw ExceptionHandling.dieInternal(tc,
                "IPv6 address buffer must be an array of uint8, uint16, uint32, uint64");
        }

        return fromNativeBuffer(tc, "", bb.array(), port, scopeId);
    }
}
