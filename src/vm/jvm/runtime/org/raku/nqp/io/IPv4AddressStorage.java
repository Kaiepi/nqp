package org.raku.nqp.io;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.VMArrayInstance_u8;
import org.raku.nqp.sixmodel.reprs.VMArrayInstance_u16;
import org.raku.nqp.sixmodel.reprs.VMArrayInstance_u32;

public final class IPv4AddressStorage extends IPAddressStorage<Inet4Address> {
    public IPv4AddressStorage(final Inet4Address address, final int port) {
        super(SocketFamily.getByName("PF_INET"), address, port);
    }

    /**
     * Creates a VMArray buffer for the stored IPv4 address' network address.
     * The buffer type's REPR must be confirmed to be VMArray before being
     * passed to this.
     */
    @Override
    public final SixModelObject toBuffer(final ThreadContext tc, final SixModelObject bufType) {
        final SixModelObject bufObj = bufType.st.REPR.allocate(tc, bufType.st);
        final ByteBuffer     bb     = ByteBuffer.allocate(4);
        bb.put(address.getAddress());
        bb.rewind();
        if (bufObj instanceof VMArrayInstance_u8) {
            final VMArrayInstance_u8 buf = (VMArrayInstance_u8)bufObj;
            buf.elems = 4;
            buf.slots = new byte[4];
            bb.get(buf.slots);
            return buf;
        } else if (bufObj instanceof VMArrayInstance_u16) {
            final VMArrayInstance_u16 buf = (VMArrayInstance_u16)bufObj;
            buf.elems = 2;
            buf.slots = new short[2];
            bb.asShortBuffer().get(buf.slots);
            return buf;
        } else if (bufObj instanceof VMArrayInstance_u32) {
            final VMArrayInstance_u32 buf = (VMArrayInstance_u32)bufObj;
            buf.elems = 1;
            buf.slots = new int[1];
            bb.asIntBuffer().get(buf.slots);
            return buf;
        } else {
            throw ExceptionHandling.dieInternal(tc,
                "IPv4 address buffer type must be an array of uint8, uint16, or uint32");
        }
    }

    /**
     * Creates storage for an IPv4 address from an IPv4 literal.
     */
    public static final IPv4AddressStorage fromString(final ThreadContext tc, final String literal, final int port) {
        try {
            final InetAddress address = InetAddress.getByName(literal);
            if (address instanceof Inet4Address) {
                return new IPv4AddressStorage((Inet4Address)address, port);
            } else {
                throw new UnknownHostException(literal);
            }
        } catch (UnknownHostException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    /**
     * Creates storage for an IPv4 address from a JVM buffer.
     */
    public static final IPv4AddressStorage fromNativeBuffer(
        final ThreadContext tc,
        final String        hostname,
        final byte[]        raw,
        final int           port
    ) {
        try {
            final InetAddress address = InetAddress.getByAddress(raw);
            if (address instanceof Inet4Address) {
                return new IPv4AddressStorage((Inet4Address)address, port);
            } else {
                throw new UnknownHostException(address.getHostAddress());
            }
        } catch (UnknownHostException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    /**
     * Creates storage for an IPv4 address from a VMArray buffer. The buffer's
     * REPR must be confirmed to be VMArray before being passed to this.
     */
    public static final IPv4AddressStorage fromBuffer(
        final ThreadContext  tc,
        final SixModelObject obj,
        final int            port
    ) {
        final ByteBuffer bb = ByteBuffer.allocate(4);
        if (obj instanceof VMArrayInstance_u8) {
            final VMArrayInstance_u8 buf = (VMArrayInstance_u8)obj;
            if (buf.elems == 4) {
                bb.put(buf.slots, 0, buf.elems);
            } else {
                throw ExceptionHandling.dieInternal(tc, "IPv4 address uint8 buffer must have 4 elements");
            }
        } else if (obj instanceof VMArrayInstance_u16) {
            final VMArrayInstance_u16 buf = (VMArrayInstance_u16)obj;
            if (buf.elems == 2) {
                bb.asShortBuffer().put(buf.slots, 0, buf.elems);
            } else {
                throw ExceptionHandling.dieInternal(tc, "IPv4 address uint16 buffer must have 2 elements");
            }
        } else if (obj instanceof VMArrayInstance_u32) {
            final VMArrayInstance_u32 buf = (VMArrayInstance_u32)obj;
            if (buf.elems == 1) {
                bb.asIntBuffer().put(buf.slots, 0, buf.elems);
            } else {
                throw ExceptionHandling.dieInternal(tc, "IPv4 address uint32 buffer must have 1 element");
            }
        } else {
            throw ExceptionHandling.dieInternal(tc, "IPv4 address buffer must be an array of uint8, uint16, or uint32");
        }

        return fromNativeBuffer(tc, "", bb.array(), port);
    }
}
