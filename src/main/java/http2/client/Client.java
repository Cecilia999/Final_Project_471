package http2.client;

import http2.Util;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class Client {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8888;
    private static SslContext sslCtx;
    private static Channel[] channel = new Channel[5];

    public static void main(String[] args) throws CertificateException, SSLException {
        sslCtx = Util.createSSLContext(false);

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ClientInit initializer = new ClientInit(sslCtx, Integer.MAX_VALUE, HOST, PORT);

        try {
            for (int i = 0; i < 5; i++) {
                Bootstrap b = new Bootstrap();
                b.group(workerGroup);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                b.remoteAddress(HOST, PORT);
                b.handler(initializer);

                channel[i] = b.connect()
                        .syncUninterruptibly()
                        .channel();

                System.out.println("Connected to " + HOST + ':' + PORT);

                SettingsHandler http2SettingsHandler = initializer.getSettingsHandler();
                http2SettingsHandler.awaitSettings(60, TimeUnit.SECONDS);

                System.out.println(" divides the request into binary frames ...");

                FullHttpRequest request = Util.createGetRequest(HOST, PORT);

                ClientFrameHandler responseHandler = initializer.getResponseHandler();

                responseHandler.put(3, channel[i].write(request), channel[i].newPromise());
                responseHandler.put(5, channel[i].write(request), channel[i].newPromise());
                channel[i].flush();
                responseHandler.awaitResponses(60, TimeUnit.SECONDS);
            }
            System.out.println("Finished");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            for (Channel c:
                    channel) {
                c.close()
                        .syncUninterruptibly();
            }
        }
    }
}
