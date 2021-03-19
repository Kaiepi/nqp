package org.raku.nqp.io;

import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

public interface IIOBindable {
    void bind(final ThreadContext tc, final AddressInstance address, final int backlog);
}
