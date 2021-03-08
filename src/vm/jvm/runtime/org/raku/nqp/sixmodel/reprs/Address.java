package org.raku.nqp.sixmodel.reprs;

import org.raku.nqp.io.IPAddressStorage;
import org.raku.nqp.io.IPv4AddressStorage;
import org.raku.nqp.io.IPv6AddressStorage;
import org.raku.nqp.io.ProtocolFamily;
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
    public void deserialize_finish(
        ThreadContext       tc,
        STable              st,
        SerializationReader reader,
        SixModelObject      obj
    ) {
        final AddressInstance address = (AddressInstance)obj;
        final int             family  = reader.readInt32();
        switch (family) {
            case ProtocolFamily.INET: {
                final byte[] networkAddress = reader.readBytes();
                final int    port           = reader.readInt32();
                try {
                    address.storage = IPv4AddressStorage.fromBytes(networkAddress, port);
                } catch (final Exception e) {
                    throw ExceptionHandling.dieInternal(tc, "Error deserializing IPv4 address: " + e.getMessage());
                }
            }
            case ProtocolFamily.INET6: {
                final byte[] networkAddress = reader.readBytes();
                final int    port           = reader.readInt32();
                final int    scopeId        = reader.readInt32();
                try {
                    address.storage = IPv6AddressStorage.fromBytes(networkAddress, port, scopeId);
                } catch (final Exception e) {
                    throw ExceptionHandling.dieInternal(tc, "Error deserializing IPv6 address: " + e.getMessage());
                }
            }
            default:
                throw ExceptionHandling.dieInternal(tc, "Invalid address family: " + family);
        }
    }

    @Override
    public void serialize(ThreadContext tc, SerializationWriter writer, SixModelObject obj) {
        final AddressInstance address = (AddressInstance)obj;
        final int             family  = address.storage.getFamily();
        writer.writeInt32(family);
        switch (family) {
            case ProtocolFamily.INET: {
                final IPv4AddressStorage storage = (IPv4AddressStorage)address.storage;
                writer.writeBytes(storage.getBytes());
                writer.writeInt32(storage.getPort());
                break;
            }
            case ProtocolFamily.INET6: {
                final IPv6AddressStorage storage = (IPv6AddressStorage)address.storage;
                writer.writeBytes(storage.getBytes());
                writer.writeInt32(storage.getPort());
                writer.writeInt32(storage.getScopeId());
                break;
            }
            default:
                throw ExceptionHandling.dieInternal(tc, "Invalid address family: " + family);
        }
    }
}
