package org.raku.nqp.io;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.UnresolvedAddressException;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.HLLConfig;
import org.raku.nqp.runtime.Ops;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.AddressInstance;
import org.raku.nqp.sixmodel.reprs.AsyncTaskInstance;
import org.raku.nqp.sixmodel.reprs.ConcBlockingQueueInstance;
import org.raku.nqp.sixmodel.reprs.IOHandleInstance;

public class AsyncServerSocketHandle implements IIOBindable, IIOCancelable, IIOAddressable {

    final AsynchronousServerSocketChannel listenChan;

    public AsyncServerSocketHandle(final ThreadContext tc) {
        try {
            this.listenChan = AsynchronousServerSocketChannel.open();
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void bind(final ThreadContext tc, final AddressInstance address, final int backlog) {
        try {
            listenChan.bind(address.storage.getAddress(), backlog);
        } catch (final UnresolvedAddressException uae) {
            ExceptionHandling.dieInternal(tc, "Failed to resolve host name");
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public void accept(final ThreadContext tc, final AsyncTaskInstance task) {
        final HLLConfig      hllConfig = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
        final SixModelObject IOType    = hllConfig.ioType;
        final SixModelObject Array     = hllConfig.listType;
        final SixModelObject Int       = hllConfig.intBoxType;
        final SixModelObject Str       = hllConfig.strBoxType;

        final CompletionHandler<AsynchronousSocketChannel, AsyncTaskInstance> handler
            = new CompletionHandler<AsynchronousSocketChannel, AsyncTaskInstance>() {

            @Override
            public void completed(final AsynchronousSocketChannel channel, final AsyncTaskInstance task) {
                final ThreadContext    curTC          = tc.gc.getCurrentThreadContext();
                final SixModelObject   result         = Array.st.REPR.allocate(curTC, Array.st);
                final IOHandleInstance clientIoHandle = (IOHandleInstance) IOType.st.REPR.allocate(curTC, IOType.st);
                clientIoHandle.handle = new AsyncSocketHandle(curTC, channel);
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, clientIoHandle);
                result.push_boxed(curTC, Str);
                ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);

                listenChan.accept(task, this);
            }

            @Override
            public void failed(final Throwable t, final AsyncTaskInstance task) {
                if (t instanceof AsynchronousCloseException || t instanceof ClosedChannelException)
                    return;

                final ThreadContext  curTC  = tc.gc.getCurrentThreadContext();
                final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, IOType);
                result.push_boxed(curTC, IOType);
                result.push_boxed(curTC, Ops.box_s(t.toString(), Str, curTC));
                ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
            }
        };

        final ThreadContext    curTC          = tc.gc.getCurrentThreadContext();
        final SixModelObject   result         = Array.st.REPR.allocate(curTC, Array.st);
        final IOHandleInstance serverIoHandle = (IOHandleInstance) IOType.st.REPR.allocate(curTC, IOType.st);
        serverIoHandle.handle = this;
        result.push_boxed(curTC, task.schedulee);
        result.push_boxed(curTC, IOType);
        result.push_boxed(curTC, serverIoHandle);
        ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
        try {
            listenChan.accept(task, handler);
        } catch (final NotYetBoundException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void cancel(final ThreadContext tc) {
        try {
            listenChan.close();
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public AddressInstance getSocketAddress(final ThreadContext tc) {
        try {
            return toAddress(tc, listenChan.getLocalAddress());
        } catch (final Exception e) {
            throw ExceptionHandling.dieInternal(tc, "Error getting the local address of a socket: " + e.getMessage());
        }
    }

    @Override
    public AddressInstance getPeerAddress(final ThreadContext tc) {
        throw ExceptionHandling.dieInternal(tc, "Cannot get the remote address of a bound socket");
    }
}
