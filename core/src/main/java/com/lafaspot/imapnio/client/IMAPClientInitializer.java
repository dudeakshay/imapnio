/**
 *
 */
package com.lafaspot.imapnio.client;

import com.lafaspot.imapnio.command.ImapClientRespDecoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * @author kraman
 *
 */
public class IMAPClientInitializer extends ChannelInitializer<SocketChannel> {

    /** The IMAP Session. */
    private final IMAPSession session;

    /** SSL context. */
    private final SslContext sslCtx;

    /** Remote server host. */
    private final String host;

    /** Remote server port. */
    private final int port;

    /**
     * Used to initialize the client channel.
     * 
     * @param session
     *            IMAP Session
     * @param sslCtx
     *            SSL context
     * @param host
     *            remote host
     * @param port
     *            remote port
     */
    public IMAPClientInitializer(final IMAPSession session, final SslContext sslCtx, final String host, final int port) {
        this.session = session;
        this.sslCtx = sslCtx;
        this.host = host;
        this.port = port;
        // this.channelHandlers = channelHandlers;
    }

    @Override
    public void initChannel(final SocketChannel ch) {
        final ChannelPipeline pipeline = ch.pipeline();

        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
        }

        // Add the text line codec combination first
        // Many discussions say that 8000 or 10000 is a large enough max value for line length here - but Gmail easily
        // sends lines >10k characters in length. (FETCH 1:* (ALL))
        pipeline.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()));
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());

        // And then business logic.
        pipeline.addLast(new ImapClientRespDecoder());
        pipeline.addLast(new IMAPClientRespHandler(session));
        pipeline.addLast(new IMAPChannelListener(session));
    }

}
