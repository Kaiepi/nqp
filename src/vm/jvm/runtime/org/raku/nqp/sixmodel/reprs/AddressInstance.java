package org.raku.nqp.sixmodel.reprs;

import java.net.InetSocketAddress;

import org.raku.nqp.sixmodel.SixModelObject;

public class AddressInstance extends SixModelObject {
    public static final int FAMILY_UNSPEC = 0;
    public static final int FAMILY_INET   = 1;
    public static final int FAMILY_INET6  = 2;
    public static final int FAMILY_UNIX   = 3;

    public static final int TYPE_ANY = 0;
    public static final int TYPE_STREAM = 1;
    public static final int TYPE_DGRAM = 2;
    public static final int TYPE_RAW = 3;
    public static final int TYPE_RDM = 4;
    public static final int TYPE_SEQPACKET = 5;

    public static final int PROTOCOL_ANY = 0;
    public static final int PROTOCOL_TCP = 1;
    public static final int PROTOCOL_UDP = 2;

    public int               family;
    public InetSocketAddress storage;
}
