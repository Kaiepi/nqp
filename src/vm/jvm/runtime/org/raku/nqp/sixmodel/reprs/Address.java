package org.raku.nqp.sixmodel.reprs;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

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
        AddressInstance address = (AddressInstance)obj;
        switch (address.family = reader.readInt32()) {
            case AddressInstance.FAMILY_INET: {
                String      hostname      = reader.readStr();
                byte[]      raw           = reader.readBytes();
                int         port          = reader.readInt32();
                InetAddress nativeAddress = null;
                try {
                    nativeAddress = Inet4Address.getByAddress(hostname, raw);
                } catch (UnknownHostException e) {
                    throw ExceptionHandling.dieInternal(tc, e);
                }
                address.storage = new InetSocketAddress(nativeAddress, port);
                break;
            }
            case AddressInstance.FAMILY_INET6: {
                String      hostname      = reader.readStr();
                byte[]      raw           = reader.readBytes();
                int         port          = reader.readInt32();
                int         scopeId       = reader.readInt32();
                InetAddress nativeAddress = null;
                try {
                    nativeAddress = Inet6Address.getByAddress(hostname, raw, scopeId);
                } catch (UnknownHostException e) {
                    throw ExceptionHandling.dieInternal(tc, e);
                }
                address.storage = new InetSocketAddress(nativeAddress, port);
                break;
            }
            default:
                throw ExceptionHandling.dieInternal(tc, "Unsupported address family: " + address.family);
        }
    }

    @Override
    public void serialize(ThreadContext tc, SerializationWriter writer, SixModelObject obj) {
        AddressInstance address = (AddressInstance)obj;
        writer.writeInt32(address.family);
        switch (address.family) {
            case AddressInstance.FAMILY_INET: {
                Inet4Address nativeAddress = (Inet4Address)address.storage.getAddress();
                int          port          = address.storage.getPort();
                writer.writeStr(nativeAddress.getHostName());
                writer.writeBytes(nativeAddress.getAddress());
                writer.writeInt32(port);
                break;
            }
            case AddressInstance.FAMILY_INET6: {
                Inet6Address nativeAddress = (Inet6Address)address.storage.getAddress();
                int          port          = address.storage.getPort();
                writer.writeStr(nativeAddress.getHostName());
                writer.writeBytes(nativeAddress.getAddress());
                writer.writeInt32(port);
                writer.writeInt32(nativeAddress.getScopeId());
                break;
            }
            default:
                throw ExceptionHandling.dieInternal(tc, "Unsupported address family: " + address.family);
        }
    }
}
