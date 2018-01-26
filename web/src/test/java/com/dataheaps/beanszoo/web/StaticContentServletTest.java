package com.dataheaps.beanszoo.web;

import com.dataheaps.aspectrest.RestHandler;
import com.dataheaps.aspectrest.annotations.Get;
import com.dataheaps.aspectrest.annotations.Path;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Ignore;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.*;

/**
 * Created by matteopelati on 11/27/17.
 */
public class StaticContentServletTest {

    @Test @Ignore
    public void testRestApiServer() throws Exception {


        JettyServer.RestSettings restSettings = new JettyServer.RestSettings();
        restSettings.setPath("/api");

        JettyServer.StaticContentSettings contentSettings = new JettyServer.StaticContentSettings();
        contentSettings.setPath("/");

        JettyServer.WsSettings wsSettings = new JettyServer.WsSettings();
        wsSettings.setPath("/ws");

        JettyServer server = new JettyServer();
        server.setPort(8976);
        server.setRest(restSettings);
        server.setContent(contentSettings);
        server.setWebsockets(wsSettings);
        server.setRestHandlers(ImmutableMap.of(
                "test", new RestHandler() {
                    @Override
                    public void init() {

                    }

                    @Get @Path("test")
                    public boolean test() {
                        return true;
                    }
                }
        ));
        server.start();

        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8976/api/test/test").openConnection();
        assert (conn.getResponseCode() == 200);
        String resp = IOUtils.toString(conn.getInputStream());
        assert (resp.equals("true"));

        conn = (HttpURLConnection) new URL("http://localhost:8976/static.txt").openConnection();
        assert (conn.getResponseCode() == 200);
        resp = IOUtils.toString(conn.getInputStream());
        assert (resp.equals("static"));

        server.stop();

    }
}