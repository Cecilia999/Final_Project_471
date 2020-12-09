package http2.client;

import http2.Util;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

public class ClientInit extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final int maxContentLength;
    private SettingsHandler settingsHandler;
    private ClientFrameHandler responseHandler;
    private String host;
    private int port;

    public ClientInit(SslContext sslCtx, int maxContentLength, String host, int port) {
        this.sslCtx = sslCtx;
        this.maxContentLength = maxContentLength;
        this.host = host;
        this.port = port;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {

        settingsHandler = new SettingsHandler(ch.newPromise());
        responseHandler = new ClientFrameHandler();
        
        if (sslCtx != null) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
            pipeline.addLast(Util.getClientAPNHandler(maxContentLength, settingsHandler, responseHandler));
        }
    }

    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }
    
    public ClientFrameHandler getResponseHandler() {
        return responseHandler;
    }
}
