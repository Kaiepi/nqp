package org.raku.nqp.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.Optional;

import org.raku.nqp.runtime.Buffers;
import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.HLLConfig;
import org.raku.nqp.runtime.Ops;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.AsyncTaskInstance;
import org.raku.nqp.sixmodel.reprs.ConcBlockingQueueInstance;
import org.raku.nqp.sixmodel.reprs.IOHandleInstance;

public class AsyncSocketHandle implements IIOClosable, IIOCancelable, IIOAddressable {
    private AsynchronousSocketChannel channel;

    public AsyncSocketHandle(ThreadContext tc) {
        try {
            this.channel = AsynchronousSocketChannel.open();
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public AsyncSocketHandle(ThreadContext tc, AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    public void connect(final ThreadContext tc, final SocketAddress address, final AsyncTaskInstance task) {
        final CompletionHandler<Void, AsyncTaskInstance> handler = new CompletionHandler<>() {
            final HLLConfig      hllConfig = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
            final SixModelObject Array     = hllConfig.listType;
            final SixModelObject Str       = hllConfig.strBoxType;
            final SixModelObject IOType    = hllConfig.ioType;

            @Override
            public void completed(final Void v, final AsyncTaskInstance task) {
                final ThreadContext    curTC      = tc.gc.getCurrentThreadContext();
                final IOHandleInstance connection = (IOHandleInstance)IOType.st.REPR.allocate(curTC, IOType.st);
                connection.handle = task.handle;
                emit(curTC, task, Str, connection);
            }

            @Override
            public void failed(Throwable t, AsyncTaskInstance task) {
                final ThreadContext curTC = tc.gc.getCurrentThreadContext();
                emit(curTC, task, Ops.box_s(t.toString(), Str, curTC), IOType);
            }

            private void emit(
                final ThreadContext     tc,
                final AsyncTaskInstance task,
                final SixModelObject    error,
                final SixModelObject    connection
            ) {
                final SixModelObject result = Array.st.REPR.allocate(tc, Array.st);
                result.push_boxed(tc, task.schedulee);
                result.push_boxed(tc, error);
                result.push_boxed(tc, connection);
                ((ConcBlockingQueueInstance)task.queue).push_boxed(tc, result);
            }
        };

        try {
            channel.connect(address, task, handler);
        } catch (Throwable e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public SocketAddress getLocalAddress(final ThreadContext tc) {
        try {
            return Optional.ofNullable(channel.getLocalAddress())
                           .orElseThrow(() -> ExceptionHandling.dieInternal(tc,
                               "No local address for this socket exists. " +
                               "It may not be connected yet, or IPv6 may be misconfigured."));
        } catch (ClosedChannelException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public SocketAddress getRemoteAddress(final ThreadContext tc) {
        try {
            return Optional.ofNullable(channel.getRemoteAddress())
                           .orElseThrow(() -> ExceptionHandling.dieInternal(tc,
                               "No remote address for this socket exists. " +
                               "It may not be connected yet, or IPv6 may be misconfigured."));
        } catch (ClosedChannelException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public void writeBytes(ThreadContext tc, AsyncTaskInstance task, SixModelObject toWrite) {
        final ByteBuffer buffer = Buffers.unstashBytes(toWrite, tc);
        writeByteBuffer(tc, task, buffer);
    }

    private void writeByteBuffer(final ThreadContext tc, final AsyncTaskInstance task, final ByteBuffer buffer) {
        try {
            final HLLConfig      hllConfig = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
            final SixModelObject Array     = hllConfig.listType;
            final SixModelObject Int       = hllConfig.intBoxType;
            final SixModelObject Str       = hllConfig.strBoxType;

            final CompletionHandler<Integer, AsyncTaskInstance> handler = new CompletionHandler<>() {
                @Override
                public void completed(final Integer bytesWritten, final AsyncTaskInstance task) {
                    final ThreadContext curTC = tc.gc.getCurrentThreadContext();
                    emit(curTC, task, Str, Ops.box_i(bytesWritten, Int, curTC));
                }

                @Override
                public void failed(Throwable t, AsyncTaskInstance attachment) {
                    final ThreadContext curTC = tc.gc.getCurrentThreadContext();
                    emit(curTC, task, Ops.box_s(t.toString(), Str, curTC), Int);
                }

                private void emit(
                    final ThreadContext     tc,
                    final AsyncTaskInstance task,
                    final SixModelObject    error,
                    final SixModelObject    bytesWritten
                ) {
                    final SixModelObject result = Array.st.REPR.allocate(tc, Array.st);
                    result.push_boxed(tc, task.schedulee);
                    result.push_boxed(tc, error);
                    result.push_boxed(tc, bytesWritten);
                    ((ConcBlockingQueueInstance)task.queue).push_boxed(tc, result);
                }
            };

            channel.write(buffer, task, handler);
        } catch (Throwable e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public void readBytes(final ThreadContext tc, final AsyncTaskInstance task, final SixModelObject bufType) {
        readSocket(tc, task, new Decoder() {
            public SixModelObject decode(
                final ThreadContext tc,
                final ByteBuffer    source,
                final Integer       numRead
            ) throws Exception {
                final SixModelObject res   = bufType.st.REPR.allocate(tc, bufType.st);
                final byte[]         bytes = new byte[source.remaining()];
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
        final ByteBuffer                                    readBuffer = ByteBuffer.allocate(32768);
        final CompletionHandler<Integer, AsyncTaskInstance> handler    = new CompletionHandler<>() {
            final HLLConfig      hllConfig   = tc.curFrame.codeRef.staticInfo.compUnit.hllConfig;
            final SixModelObject Array       = hllConfig.listType;
            final SixModelObject Int         = hllConfig.intBoxType;
            final SixModelObject Str         = hllConfig.strBoxType;
            final SixModelObject BOOTAddress = tc.gc.BOOTAddress;

            @Override
            public void completed(final Integer numRead, final AsyncTaskInstance task) {
                final ThreadContext curTC = tc.gc.getCurrentThreadContext();
                try {
                    if (numRead == -1) {
                        emit(curTC, task, Str, Str, Ops.box_i(task.seq, Int, curTC));
                    } else {
                        final SixModelObject decoded = decoder.decode(tc, readBuffer.flip(), numRead);
                        emit(curTC, task, Str, decoded, Ops.box_i(task.seq++, Int, curTC));
                        readBuffer.compact();
                        channel.read(readBuffer, task, this);
                    }
                } catch (Throwable t) {
                    failed(t, task);
                }
            }

            @Override
            public void failed(final Throwable t, final AsyncTaskInstance task) {
                if (!(t instanceof AsynchronousCloseException) && !(t instanceof ClosedChannelException)) {
                    final ThreadContext curTC = tc.gc.getCurrentThreadContext();
                    emit(curTC, task, Ops.box_s(t.toString(), Str, curTC), Str, Int);
                }
            }

            private void emit(
                final ThreadContext     tc,
                final AsyncTaskInstance task,
                final SixModelObject    error,
                final SixModelObject    str,
                final SixModelObject    seq
            ) {
                final SixModelObject result = Array.st.REPR.allocate(tc, Array.st);
                result.push_boxed(tc, task.schedulee);
                result.push_boxed(tc, error);
                result.push_boxed(tc, str);
                result.push_boxed(tc, seq);
                result.push_boxed(tc, BOOTAddress);
                ((ConcBlockingQueueInstance)task.queue).push_boxed(tc, result);
            }
        };

        try {
            channel.read(readBuffer, task, handler);
        } catch (Throwable t) {
            handler.failed(t, task);
        }
    }

    @Override
    public void close(ThreadContext tc) {
        try {
            channel.close();
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void cancel(ThreadContext tc) {
        close(tc);
    }
}
