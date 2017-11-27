package com.dataheaps.beanszoo.web;

import com.dataheaps.aspectrest.AspectRestServlet;
import com.dataheaps.aspectrest.RestHandler;
import com.dataheaps.aspectrest.modules.auth.AuthModule;
import com.dataheaps.aspectrest.serializers.GensonSerializer;
import com.dataheaps.aspectrest.serializers.Serializer;
import com.dataheaps.beanszoo.lifecycle.AbstractLifeCycle;
import com.dataheaps.beanszoo.sd.Services;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by admin on 19/2/17.
 */
public class JettyServer extends AbstractLifeCycle {

    @Data @NoArgsConstructor
    public static class CorsSettings {
        @Getter @Setter List<String> allowedHeaders = new ArrayList<>();
        @Getter @Setter List<String> allowedVerbs = ImmutableList.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        @Getter @Setter String allowedOrigin = "*";
        @Getter @Setter int maxAge = 3600;
        @Getter @Setter boolean allowCredentials = true;
    }

    @Data @NoArgsConstructor
    public static class SslSettings {
        @Getter @Setter int port = 8443;
        @Getter @Setter String keyStorePath = null;
        @Getter @Setter String keyStorePassword = null;
        @Getter @Setter String keyManagerPassword = null;
    }

    @Data @NoArgsConstructor
    public static class WsSettings {
        String path = "/ws";
        String[] proxies = new String[0];
    }

    @Data @NoArgsConstructor
    public static class StaticContentSettings {
        @Getter @Setter String localBasePath = null;
        @Getter @Setter String defaultPage = "index.html";
        @Getter @Setter String path = "/";
    }

    @Data @NoArgsConstructor
    public static class RestSettings {
        @Getter @Setter String path = "/api";
    }

    static class MethodMessageInfo {
        String command;
        Map<String, Integer> fields = new HashMap<>();
        int subscriptionIndex = -1;
    }


    static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

    @Getter @Setter int port = 8080;

    @Getter @Setter SslSettings ssl = null;
    @Getter @Setter CorsSettings cors = null;
    @Getter @Setter WsSettings websockets = null;
    @Getter @Setter StaticContentSettings content = null;
    @Getter @Setter RestSettings rest = null;
    @Getter @Setter Serializer serializer = new GensonSerializer();

    @Getter @Setter Map<String, RestHandler> restHandlers = new HashMap<>();
    @Getter @Setter Map<String, AuthModule> restAuthenticators = new HashMap<>();
    @Getter @Setter Map<String, String> headers = new HashMap<>();

    protected Server server;
    protected Services services;


    Server createServer() throws Exception {

        Server server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        if (ssl != null) {

            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(ssl.keyStorePath);
            sslContextFactory.setKeyStorePassword(ssl.keyStorePassword);
            sslContextFactory.setKeyManagerPassword(ssl.keyManagerPassword);
            ServerConnector sslConnector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, "HTTP/1.1"),
                    new HttpConnectionFactory(https)
            );
            sslConnector.setPort(ssl.port);
            server.addConnector(sslConnector);
        }

        HandlerCollection handlers = new HandlerCollection();

        if (cors != null) {
            handlers.addHandler(new CorsHandler(
                    cors.allowedVerbs, cors.allowedOrigin, cors.allowedHeaders,
                    cors.maxAge, cors.allowCredentials
            ));
        }

        if (rest != null) {

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath(rest.getPath());
            handlers.addHandler(context);

            AspectRestServlet restServlet = new AspectRestServlet(false, serializer, services);
            restServlet.setModules(restHandlers);
            restServlet.setAuthenticators(restAuthenticators);
            restServlet.setHeaders(headers);
            context.addServlet(new ServletHolder(restServlet), "/");
        }

        if (websockets != null) {

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath(websockets.getPath());
            handlers.addHandler(context);

            WebSocketNotifier notifierServlet = new WebSocketNotifier(serializer);
            context.addServlet(new ServletHolder(notifierServlet), "/");
            registerWebProxy(websockets.proxies, notifierServlet);
        }

        if (content != null) {

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath(content.getPath());
            handlers.addHandler(context);

            StaticContentServlet staticServlet = new StaticContentServlet(
                    content.localBasePath == null ? null : StringUtils.join(content.localBasePath.split("/"), File.separator),
                    content.defaultPage
            );
            context.addServlet(new ServletHolder(staticServlet), "/");
        }

        server.setHandler(handlers);
        return server;

    }

    void registerWebProxy(String[] proxies, WebSocketNotifier notifier) throws Exception {

        Class[] klasses = new Class[proxies.length];
        for (int ctr=0;ctr<proxies.length;ctr++)
            klasses[ctr] = Class.forName(proxies[ctr]);

        Object proxy = Proxy.newProxyInstance(
                this.getClass().getClassLoader(), klasses,
                new InvocationHandler() {

                    Map<Method, MethodMessageInfo> cache = new ConcurrentHashMap<Method, MethodMessageInfo>();
                    int hash = new Random().nextInt();

                    MethodMessageInfo getMethodInfo(Method m) {

                        MethodMessageInfo info = cache.get(m);
                        if (info != null)
                            return info;

                        WsCommand cmd = m.getAnnotation(WsCommand.class);
                        if (cmd == null) return null;

                        info = new MethodMessageInfo();
                        info.command = cmd.value();

                        Parameter[] params = m.getParameters();
                        for (int ctr=0;ctr<params.length;ctr++) {
                            if (params[ctr].getAnnotation(WsSubscription.class) != null)
                                info.subscriptionIndex = ctr;
                            else {
                                WsParam p = params[ctr].getAnnotation(WsParam.class);
                                info.fields.put(p.value(), ctr);
                            }
                        }
                        cache.put(m, info);
                        return info;
                    }

                    @Override
                    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if (method.getName().equals("hashCode"))
                            return hash;
                        else if (method.getName().equals("equals"))
                            return hash == args[0].hashCode();

                        MethodMessageInfo methodInfo = getMethodInfo(method);
                        if (methodInfo == null)
                            return null;

                        String subId = methodInfo.subscriptionIndex < 0 ? null : (String) args[methodInfo.subscriptionIndex];
                        Map<String, Object> params = new HashMap<>();
                        params.put("command", methodInfo.command);
                        for (Map.Entry<String,Integer> e: methodInfo.fields.entrySet())
                            params.put(e.getKey(), args[e.getValue()]);
                        notifier.sendMessage(subId, params);

                        return null;
                    }
                }
        );

        services.getServiceDirectory().putService(proxy);
    }


    protected synchronized void doStart() throws Exception {
        server = createServer();
        server.start();
    }

    protected synchronized void doStop() throws Exception {
        server.stop();
    }

    public void init(Services services) throws Exception {
        this.services = services;
    }
}

