package http2.server;

import http2.Util;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
public final class Server {

    private static final int PORT = 8888;

    public static void main(String[] args) throws Exception {
        final SslContext sslCtx = Util.createSSLContext(true);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
//            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        if (sslCtx != null) {
                            ch.pipeline()
                                .addLast(sslCtx.newHandler(ch.alloc()), Util.getServerAPNHandler());
                        }
                    }

                });

            Channel ch = b.bind(PORT)
                .sync()
                .channel();

            System.out.println("HTTP/2 Server is listening on https://127.0.0.1:" + PORT + '/');

            ch.closeFuture()
                .sync();
        } finally {
            group.shutdownGracefully();
        }
    }

}
