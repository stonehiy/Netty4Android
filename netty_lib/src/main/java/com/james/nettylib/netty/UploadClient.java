package com.james.nettylib.netty;

/**
 * Create by james on 2018/6/28
 */

import com.james.nettylib.constant.NetworkConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.util.List;
import java.util.Map;


public class UploadClient {
    private StringBuffer resultBuffer = new StringBuffer();
    private EventLoopGroup group = null;
    private HttpDataFactory factory = null;
    private Object waitObject = new Object();
    private ChannelFuture future = null;


    public interface FileUploadListener {
        public void onSuccess(String url);

        public void onFailed();
    }

    private String host;
    private int port;
    private FileUploadListener listener;

    public UploadClient() {

        this.host = getUploadHost();
        this.port = getUploadPort();

    }

    private String getUploadHost(){
        if(NetworkConfig.DEV){
            return NetworkConfig.UPLOAD_HOST_TEST;
        }else{
            return NetworkConfig.UPLOAD_HOST;
        }
    }

    private int getUploadPort(){
        if(NetworkConfig.DEV){
            return NetworkConfig.UPLOAD_PORT_TEST;
        }else{
            return NetworkConfig.UPLOAD_PORT;
        }
    }

    public void doUpload(final String path, final Map<String, String> params, final FileUploadListener listener) {
        this.listener = listener;

        this.group = new NioEventLoopGroup();
        this.factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

        Bootstrap b = new Bootstrap();
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.SO_SNDBUF, 1048576 * 200);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.group(group).channel(NioSocketChannel.class);
        b.handler(new UpLoadClientIntializer());
        b.connect(host, port).addListener(new ChannelFutureListener() {


            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture != null && channelFuture.isSuccess()) {
                    future = channelFuture;
                    uploadFile(path, params, listener);
                } else {
                    listener.onFailed();
                }
            }
        });
    }

    private void uploadFile(String path, Map<String, String> params, final FileUploadListener listener) {
        if (path == null) {
            System.out.println("path is null");
            listener.onFailed();
            return;
        }
        File file = new File(path);
        if (!file.canRead()) {
            System.out.println(file.getName() + "file is not allow read");
            listener.onFailed();
            return;
        }
        if (file.isHidden() || !file.isFile()) {
            System.out.println(file.getName() + "file is hidden");
            listener.onFailed();
            return;
        }

        try {
            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "");
            HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(factory, request, false);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                bodyRequestEncoder.addBodyAttribute(entry.getKey(), entry.getValue());
            }
            bodyRequestEncoder.addBodyFileUpload("myfile", file, "application/x-zip-compressed", false);
            List<InterfaceHttpData> bodylist = bodyRequestEncoder.getBodyListAttributes();
            if (bodylist == null) {
                System.out.println("bodylist is null");
                listener.onFailed();
                return;
            }

            HttpRequest request2 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, file.getName());
            HttpPostRequestEncoder bodyRequestEncoder2 = new HttpPostRequestEncoder(factory, request2, true);

            bodyRequestEncoder2.setBodyHttpDatas(bodylist);
            bodyRequestEncoder2.finalizeRequest();

            Channel channel = this.future.channel();
            if (channel.isActive() && channel.isWritable()) {
                channel.writeAndFlush(request2);

                if (bodyRequestEncoder2.isChunked()) {
                    channel.writeAndFlush(bodyRequestEncoder2).awaitUninterruptibly();
                }

                bodyRequestEncoder2.cleanFiles();
            }
            channel.closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void shutdownClient() {
        group.shutdownGracefully();
        factory.cleanAllHttpDatas();
    }


    public boolean isCompleted() {
        while (waitObject != null) {
        }
        if (resultBuffer.length() > 0) {
            if ("200".equals(resultBuffer.toString())) {
                resultBuffer.setLength(0);
                return true;
            }
        }
        return false;
    }


    private class UpLoadClientIntializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            pipeline.addLast("decoder", new HttpResponseDecoder());
            pipeline.addLast("encoder", new HttpRequestEncoder());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            pipeline.addLast("dispatcher", new UpLoadClientHandler());
        }
    }


    private class UpLoadClientHandler extends SimpleChannelInboundHandler<HttpObject> {
        private boolean readingChunks = false;
        private int succCode = 200;


        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;

                succCode = response.getStatus().code();

                if (succCode == 200 && HttpHeaders.isTransferEncodingChunked(response)) {
                    readingChunks = true;
                }
            }

            if (msg instanceof HttpContent) {
                HttpContent chunk = (HttpContent) msg;
                System.out.println("HttpContent" + succCode + ">>" + chunk.content().toString(CharsetUtil.UTF_8));
                String result = chunk.content().toString(CharsetUtil.UTF_8);
                if (result != null && !result.equals("")) {
                    String[] filenames = result.split("\\|");
                    if (filenames[1] != null && !filenames[1].equals("")) {
                        listener.onSuccess(filenames[1]);
                    } else {
                        listener.onFailed();
                    }
                } else {
                    listener.onFailed();
                }
                if (chunk instanceof LastHttpContent) {
                    readingChunks = false;
                }
            }

            if (!readingChunks) {
                resultBuffer.append(succCode);
                ctx.channel().close();
            }
            shutdownClient();
        }


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            waitObject = null;
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            resultBuffer.setLength(0);
            resultBuffer.append(500);
            System.out.println("exceptionCaught=" + cause.getMessage());
            cause.printStackTrace();
            ctx.channel().close();
            shutdownClient();
        }
    }
}
