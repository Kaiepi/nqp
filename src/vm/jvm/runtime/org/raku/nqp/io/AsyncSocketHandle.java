package org.raku.nqp.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ClosedChannelException;

import org.raku.nqp.runtime.Buffers;
import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.HLLConfig;
import org.raku.nqp.runtime.Ops;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.AddressInstance;
import org.raku.nqp.sixmodel.reprs.AsyncTaskInstance;
import org.raku.nqp.sixmodel.reprs.ConcBlockingQueueInstance;
import org.raku.nqp.sixmodel.reprs.IOHandleInstance;

public class AsyncSocketHandle implements IIOClosable, IIOCancelable, IIOAddressable {

    final AsynchronousSocketChannel channel;

    public AsyncSocketHandle(final ThreadContext tc) {
        try {
            this.channel = AsynchronousSocketChannel.open();
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public AsyncSocketHandle(final ThreadContext tc, final AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    public void connect(final ThreadContext tc, final AddressInstance address, final AsyncTaskInstance task) {
        final HLLConfig      hllConfig = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
        final SixModelObject IOType    = hllConfig.ioType;
        final SixModelObject Array     = hllConfig.listType;
        final SixModelObject Int       = hllConfig.intBoxType;
        final SixModelObject Str       = hllConfig.strBoxType;

        final CompletionHandler<Void, AsyncTaskInstance> handler
            = new CompletionHandler<Void, AsyncTaskInstance>() {

            @Override
            public void completed(final Void v, final AsyncTaskInstance task) {
                final ThreadContext    curTC    = tc.gc.getCurrentThreadContext();
                final SixModelObject   result   = Array.st.REPR.allocate(curTC, Array.st);
                final IOHandleInstance ioHandle = (IOHandleInstance) IOType.st.REPR.allocate(curTC, IOType.st);
                ioHandle.handle = task.handle;
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, ioHandle);
                result.push_boxed(curTC, Str);
                ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
            }

            @Override
            public void failed(final Throwable t, final AsyncTaskInstance task) {
                final ThreadContext  curTC  = tc.gc.getCurrentThreadContext();
                final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, IOType);
                result.push_boxed(curTC, Ops.box_s(t.toString(), Str, curTC));
                ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
            }
        };

        try {
            channel.connect(address.storage.getAddress(), task, handler);
        } catch (final Throwable t) {
            throw ExceptionHandling.dieInternal(tc, t);
        }
    }

    public void writeBytes(final ThreadContext tc, final AsyncTaskInstance task, final SixModelObject toWrite) {
        ByteBuffer buffer = Buffers.unstashBytes(toWrite, tc);
        writeByteBuffer(tc, task, buffer);
    }

    private void writeByteBuffer(final ThreadContext tc, final AsyncTaskInstance task, ByteBuffer buffer) {
        final HLLConfig      hllConfig = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
        final SixModelObject Array     = hllConfig.listType;
        final SixModelObject Int       = hllConfig.intBoxType;
        final SixModelObject Str       = hllConfig.strBoxType;

        final CompletionHandler<Integer, AsyncTaskInstance> handler
            = new CompletionHandler<Integer, AsyncTaskInstance>() {

            @Override
            public void completed(Integer bytesWritten, AsyncTaskInstance task) {
                final ThreadContext  curTC  = tc.gc.getCurrentThreadContext();
                final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, Ops.box_i(bytesWritten, Int, curTC));
                result.push_boxed(curTC, Str);
                ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
            }

            @Override
            public void failed(Throwable t, AsyncTaskInstance attachment) {
                final ThreadContext  curTC  = tc.gc.getCurrentThreadContext();
                final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, Int);
                result.push_boxed(curTC, Ops.box_s(t.toString(), Str, curTC));
                ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
            }
        };

        try {
            channel.write(buffer, task, handler);
        } catch (final Throwable t) {
            throw ExceptionHandling.dieInternal(tc, t);
        }
    }

    public void readBytes(final ThreadContext tc, final AsyncTaskInstance task, final SixModelObject bufType) {
        readSocket(tc, task, new Decoder() {
            public SixModelObject decode(ThreadContext tc, ByteBuffer source, Integer numRead)
                    throws Exception {
                SixModelObject res = bufType.st.REPR.allocate(tc, bufType.st);
                byte[] bytes = new byte[source.remaining()];
                source.get(bytes);
                Buffers.stashBytes(tc, res, bytes);
                return res;
            }
        });
    }

    static interface Decoder {
        public SixModelObject decode(ThreadContext tc, ByteBuffer source, Integer numRead) throws Exception;
    }

    private void readSocket(final ThreadContext tc, final AsyncTaskInstance task, final Decoder decoder) {
        final ByteBuffer     readBuffer  = ByteBuffer.allocate(32768);
        final SixModelObject BOOTAddress = tc.gc.BOOTAddress;
        final HLLConfig      hllConfig   = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
        final SixModelObject Array       = hllConfig.listType;
        final SixModelObject Int         = hllConfig.intBoxType;
        final SixModelObject Str         = hllConfig.strBoxType;

        final CompletionHandler<Integer, AsyncTaskInstance> handler
            = new CompletionHandler<Integer, AsyncTaskInstance>() {

            @Override
            public void completed(final Integer numRead, final AsyncTaskInstance task) {
                try {
                    final ThreadContext curTC = tc.gc.getCurrentThreadContext();
                    if (numRead == -1) {
                        final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                        result.push_boxed(curTC, task.schedulee);
                        result.push_boxed(curTC, Ops.box_i(task.seq, Int, curTC));
                        result.push_boxed(curTC, Str);
                        result.push_boxed(curTC, Int);
                        result.push_boxed(curTC, BOOTAddress);
                        result.push_boxed(curTC, Str);
                        ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
                    }
                    else {
                        final SixModelObject decoded;
                        readBuffer.flip();
                        decoded = decoder.decode(tc, readBuffer, numRead);
                        readBuffer.compact();

                        final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                        result.push_boxed(curTC, task.schedulee);
                        result.push_boxed(curTC, Ops.box_i(task.seq++, Int, curTC));
                        result.push_boxed(curTC, decoded);
                        result.push_boxed(curTC, Int);
                        result.push_boxed(curTC, BOOTAddress);
                        result.push_boxed(curTC, Str);
                        ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);

                        channel.read(readBuffer, task, this);
                    }
                } catch (final Exception e) {
                    failed(e, task);
                }
            }

            @Override
            public void failed(final Throwable t, final AsyncTaskInstance task) {
                if (t instanceof AsynchronousCloseException || t instanceof ClosedChannelException)
                    return;

                final ThreadContext  curTC  = tc.gc.getCurrentThreadContext();
                final SixModelObject result = Array.st.REPR.allocate(curTC, Array.st);
                result.push_boxed(curTC, task.schedulee);
                result.push_boxed(curTC, Int);
                result.push_boxed(curTC, Str);
                result.push_boxed(curTC, Int);
                result.push_boxed(curTC, BOOTAddress);
                result.push_boxed(curTC, Ops.box_s(t.toString(), Str, curTC));
                ((ConcBlockingQueueInstance) task.queue).push_boxed(curTC, result);
            }
        };

        channel.read(readBuffer, task, handler);
    }

    @Override
    public void close(final ThreadContext tc) {
        try {
            channel.close();
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void cancel(final ThreadContext tc) {
        close(tc);
    }

    @Override
    public AddressInstance getSocketAddress(final ThreadContext tc) {
        try {
            return toAddress(tc, channel.getLocalAddress());
        } catch (final Exception e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public AddressInstance getPeerAddress(final ThreadContext tc) {
        try {
            return toAddress(tc, channel.getRemoteAddress());
        } catch (final Exception e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }
}
