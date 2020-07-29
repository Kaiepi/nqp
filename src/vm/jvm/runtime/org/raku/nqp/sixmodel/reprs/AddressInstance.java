package org.raku.nqp.sixmodel.reprs;

import java.net.InetSocketAddress;

import org.raku.nqp.io.SocketFamily;
import org.raku.nqp.sixmodel.SixModelObject;

public class AddressInstance extends SixModelObject {
    public SocketFamily      family;
    public InetSocketAddress storage;
}
