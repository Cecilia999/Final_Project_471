package http2;

import http2.client.ClientFrameHandler;
import http2.client.SettingsHandler;
import http2.server.ServerFrameHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

import static io.netty.handler.logging.LogLevel.INFO;

public class Util {
    public static SslContext createSSLContext(boolean isServer) throws SSLException, CertificateException {

        SslContext sslCtx;

        SelfSignedCertificate ssc = new SelfSignedCertificate();

        if (isServer) {
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(SslProvider.JDK)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(Protocol.ALPN,
                    SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1))
                .build();
        } else {
            sslCtx = SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(Protocol.ALPN,
                    SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2))
                .build();
        }
        return sslCtx;

    }

    public static ApplicationProtocolNegotiationHandler getServerAPNHandler() {
        ApplicationProtocolNegotiationHandler serverAPNHandler = new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_2) {

            @Override
            protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
                if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                    ctx.pipeline()
                        .addLast(Http2FrameCodecBuilder.forServer()
                            .build(), new ServerFrameHandler());
                    return;
                }
                throw new IllegalStateException("Protocol: " + protocol + " not supported");
            }
        };
        return serverAPNHandler;

    }

    public static ApplicationProtocolNegotiationHandler getClientAPNHandler(int maxContentLength, final SettingsHandler settingsHandler, final ClientFrameHandler responseHandler) {
        final Http2Connection connection = new DefaultHttp2Connection(false);

        final HttpToHttp2ConnectionHandler connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
            .frameListener(new DelegatingDecompressorFrameListener(connection, new InboundHttp2ToHttpAdapterBuilder(connection).maxContentLength(maxContentLength)
            .propagateSettings(true)
            .build()))
            .connection(connection)
            .build();

        ApplicationProtocolNegotiationHandler clientAPNHandler = new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_2) {
            @Override
            protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                    ChannelPipeline p = ctx.pipeline();
                    p.addLast(connectionHandler);
                    p.addLast(settingsHandler, responseHandler);
                    return;
                }
                ctx.close();
                throw new IllegalStateException("Protocol: " + protocol + " not supported");
            }
        };

        return clientAPNHandler;

    }

    public static FullHttpRequest createGetRequest(String host, int port) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.valueOf("HTTP/2.0"), HttpMethod.GET, "/", Unpooled.EMPTY_BUFFER);
        request.headers()
            .add(HttpHeaderNames.HOST, new String(host + ":" + port));
        request.headers()
            .add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTPS);
        request.headers()
            .add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        request.headers()
            .add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
        return request;
    }
}
