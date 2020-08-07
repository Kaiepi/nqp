package org.raku.nqp.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetBoundException;
import java.util.Optional;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.HLLConfig;
import org.raku.nqp.runtime.Ops;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.AsyncTaskInstance;
import org.raku.nqp.sixmodel.reprs.ConcBlockingQueueInstance;
import org.raku.nqp.sixmodel.reprs.IOHandleInstance;

public class AsyncServerSocketHandle implements IIOBindable, IIOCancelable, IIOAddressable {

    AsynchronousServerSocketChannel listenChan;

    public AsyncServerSocketHandle(ThreadContext tc) {
        try {
            listenChan = AsynchronousServerSocketChannel.open();
        } catch (IOException e) {
            ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void bind(final ThreadContext tc, final SocketAddress address, final int backlog) {
        try {
            listenChan.bind(address, backlog);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public void accept(final ThreadContext tc, final AsyncTaskInstance task) {
        final HLLConfig      hllConfig = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
        final SixModelObject Array     = hllConfig.listType;
        final SixModelObject Str       = hllConfig.strBoxType;
        final SixModelObject IOType    = hllConfig.ioType;

        final CompletionHandler<AsynchronousSocketChannel, AsyncTaskInstance> handler = new CompletionHandler<>() {
            @Override
            public void completed(final AsynchronousSocketChannel channel, final AsyncTaskInstance task) {
                final ThreadContext    curTC      = tc.gc.getCurrentThreadContext();
                final IOHandleInstance connection = (IOHandleInstance)IOType.st.REPR.allocate(curTC, IOType.st);
                connection.handle = new AsyncSocketHandle(curTC, channel);

                final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, Str);
                result.push_boxed(curTC, IOType);
                result.push_boxed(curTC, connection);
                ((ConcBlockingQueueInstance)task.queue).push_boxed(curTC, result);
                listenChan.accept(task, this);
            }

            @Override
            public void failed(final Throwable exc, final AsyncTaskInstance task) {
                if (!(exc instanceof AsynchronousCloseException) && !(exc instanceof ClosedChannelException)) {
                    final ThreadContext  curTC  = tc.gc.getCurrentThreadContext();
                    final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                    result.push_boxed(curTC, task.schedulee);
                    result.push_boxed(curTC, Ops.box_s(exc.toString(), Str, curTC));
                    result.push_boxed(curTC, IOType);
                    result.push_boxed(curTC, IOType);
                    ((ConcBlockingQueueInstance)task.queue).push_boxed(curTC, result);
                }
            }
        };

        try {
            final IOHandleInstance binding = (IOHandleInstance)IOType.st.REPR.allocate(tc, IOType.st);
            binding.handle = this;

            final SixModelObject result = Array.st.REPR.allocate(tc, Array.st);
            result.push_boxed(tc, task.schedulee);
            result.push_boxed(tc, Str);
            result.push_boxed(tc, binding);
            result.push_boxed(tc, IOType);
            ((ConcBlockingQueueInstance)task.queue).push_boxed(tc, result);
        }
        catch (Exception e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }

        try {
            listenChan.accept(task, handler);
        } catch (NotYetBoundException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public SocketAddress getLocalAddress(final ThreadContext tc) {
        try {
            return Optional.ofNullable(listenChan.getLocalAddress())
                           .orElseThrow(() -> ExceptionHandling.dieInternal(tc,
                               "No local address for this socket exists. " +
                               "It may not be bound yet, or IPv6 may be misconfigured."));
        } catch (ClosedChannelException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public SocketAddress getRemoteAddress(final ThreadContext tc) {
        throw ExceptionHandling.dieInternal(tc, "Cannot get the remote address of a passive socket");
    }

    @Override
    public void cancel(ThreadContext tc) {
        try {
            listenChan.close();
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }
}
