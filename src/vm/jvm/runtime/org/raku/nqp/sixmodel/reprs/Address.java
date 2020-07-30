package org.raku.nqp.sixmodel.reprs;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;

import org.raku.nqp.io.IPAddressStorage;
import org.raku.nqp.io.IPv4AddressStorage;
import org.raku.nqp.io.IPv6AddressStorage;
import org.raku.nqp.io.SocketFamily;
import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.REPR;
import org.raku.nqp.sixmodel.STable;
import org.raku.nqp.sixmodel.SerializationReader;
import org.raku.nqp.sixmodel.SerializationWriter;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.TypeObject;

public class Address extends REPR {
    @Override
    public SixModelObject type_object_for(ThreadContext tc, SixModelObject HOW) {
        STable         st  = new STable(this, HOW);
        SixModelObject obj = new TypeObject();
        obj.st  = st;
        st.WHAT = obj;
        return st.WHAT;
    }

    @Override
    public SixModelObject allocate(ThreadContext tc, STable st) {
        AddressInstance obj = new AddressInstance();
        obj.st = st;
        return obj;
    }

    @Override
    public SixModelObject deserialize_stub(ThreadContext tc, STable st) {
        AddressInstance obj = new AddressInstance();
        obj.st = st;
        return obj;
    }

    @Override
    public void deserialize_finish(ThreadContext tc, STable st,
                                   SerializationReader reader, SixModelObject obj) {
        final AddressInstance address     = (AddressInstance)obj;
        final short           familyValue = (short)reader.readInt32();
        final SocketFamily    family      = SocketFamily.getByValue(familyValue);
        if (family == null) {
            throw ExceptionHandling.dieInternal(tc, "Unsupported socket family: " + familyValue);
        } else if (family.equals(SocketFamily.getByName("PF_INET"))) {
            final String hostname = reader.readStr();
            final byte[] raw      = reader.readBytes();
            final int    port     = reader.readInt32();
            address.storage = IPv4AddressStorage.fromNativeBuffer(tc, hostname, raw, port);
        } else if (family.equals(SocketFamily.getByName("PF_INET6"))) {
            final String hostname = reader.readStr();
            final byte[] raw      = reader.readBytes();
            final int    port     = reader.readInt32();
            final int    scopeId  = reader.readInt32();
            address.storage = IPv6AddressStorage.fromNativeBuffer(tc, hostname, raw, port, scopeId);
        } else {
            throw ExceptionHandling.dieInternal(tc, "Unsupported address family: " + address.storage.getFamily());
        }
    }

    @Override
    public void serialize(ThreadContext tc, SerializationWriter writer, SixModelObject obj) {
        final AddressInstance address = (AddressInstance)obj;
        writer.writeInt32((int)address.storage.getFamily().getValue());
        if (address.storage instanceof IPv4AddressStorage) {
            final IPv4AddressStorage storage       = (IPv4AddressStorage)address.storage;
            final Inet4Address       nativeAddress = storage.getAddress();
            final int                port          = storage.getPort();
            writer.writeStr(nativeAddress.getHostName());
            writer.writeBytes(nativeAddress.getAddress());
            writer.writeInt32(port);
        } else if (address.storage instanceof IPv6AddressStorage) {
            final IPv6AddressStorage storage       = (IPv6AddressStorage)address.storage;
            final Inet6Address       nativeAddress = storage.getAddress();
            final int                port          = storage.getPort();
            final int                scopeId       = storage.getScopeId();
            writer.writeStr(nativeAddress.getHostName());
            writer.writeBytes(nativeAddress.getAddress());
            writer.writeInt32(port);
            writer.writeInt32(scopeId);
        } else {
            throw ExceptionHandling.dieInternal(tc, "Unsupported address family: " + address.storage.getFamily());
        }
    }
}
