package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by matteopelati on 28/10/15.
 */


public class WsRpcServer implements RpcServer {

    @RequiredArgsConstructor
    static class EventServlet extends WebSocketServlet {

        final Map<String,RPCRequestCodec> codecs;
        final RpcRequestHandler requestHandler;

        @Override
        public void configure(WebSocketServletFactory factory) {
            factory.setCreator(new RpcWsCreator(codecs, requestHandler));
        }
    }

    @RequiredArgsConstructor
    static class RpcWsCreator implements WebSocketCreator {

        final Map<String,RPCRequestCodec> codecs;
        final RpcRequestHandler requestHandler;

        @Override
        public Object createWebSocket(ServletUpgradeRequest ureq, ServletUpgradeResponse uresp) {
            return new WsRpcServerHandler(codecs, requestHandler);
        }
    }


    final WsRpcServerAddress bindings;
    final Map<String,RPCRequestCodec> codecs = new HashMap<>();
    final RpcRequestHandler requestHandler;
    Server server;

    public WsRpcServer(WsRpcServerAddress bindings, List<RPCRequestCodec> codecs, RpcRequestHandler requestHandler) {
        this.bindings = bindings;
        this.requestHandler = requestHandler;
        for (RPCRequestCodec codec : codecs)
            this.codecs.put(codec.getContentType(), codec);

        server = new Server(bindings.getPort());
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServletHolder holderEvents = new ServletHolder(new EventServlet(this.codecs, requestHandler));
        context.addServlet(holderEvents, "/events/*");
    }


    @Override
    public void start() throws Exception {
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }
}
