package com.dataheaps.beanszoo.web;

import com.dataheaps.aspectrest.serializers.Serializer;
import com.dataheaps.beanszoo.utils.Multimap;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.ExtensionFactory;
import org.eclipse.jetty.websocket.servlet.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by matteo on 10/2/16.
 */

public class WebSocketNotifier extends WebSocketServlet {

    enum Command {
        sub, unsub
    }

    @Data
    static class ControlMessage {
        Command command;
        String[] ids;
    }


    Serializer serializer;
    WebsocketHandler handler;

    public WebSocketNotifier(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void configure(WebSocketServletFactory f) {

        ExtensionFactory extFact = f.getExtensionFactory();
        extFact.unregister("deflate-frame");
        extFact.unregister("permessage-deflate");
        extFact.unregister("x-webkit-deflate-frame");

        handler = new WebsocketHandler(serializer);
        f.setCreator((req, resp) -> handler);
    }

    public void sendMessage(String subid, Object message) {
        handler.sendMessage(subid, message);
    }

    @WebSocket
    public static class WebsocketHandler {

        ExecutorService sender = Executors.newSingleThreadExecutor();
        Multimap<String, Session> subs = new Multimap<>();
        Multimap<Session, String> subsReverse = new Multimap<>();
        ReadWriteLock lock = new ReentrantReadWriteLock();

        Serializer serializer;

        public WebsocketHandler(Serializer serializer) {
            this.serializer = serializer;
        }

        public void sendMessage(String id, Object o) {
            sender.submit(() -> {
                lock.readLock().lock();
                byte[] data = serializer.serialize(o);
                try {
                    if (id != null) {
                       Set<Session> sessions = subs.get(id);
                       for (Session s : sessions)
                           s.getRemote().sendString(new String(data));
                    }
                }
                catch (Exception e) {

                }
                finally {
                    lock.readLock().unlock();
                }
            });
        }

        @OnWebSocketMessage
        public void messageReceived(Session session, Reader reader) throws Exception {
            lock.writeLock().lock();
            try {
                byte[] data = IOUtils.toByteArray(reader);
                ControlMessage message = (ControlMessage)
                        serializer.deserialize(new ByteArrayInputStream(data), ControlMessage.class);
                for (String id: message.getIds()) {
                    if (message.getCommand().equals(Command.sub)) {
                        subs.put(id, session);
                        subsReverse.put(session, id);
                    }
                    else if (message.getCommand().equals(Command.unsub)) {
                        subs.remove(id, session);
                        subsReverse.remove(session, id);
                    }
                }
            }
            catch (Exception e) {

            }
            finally {
                lock.writeLock().unlock();
            }
        }

        @OnWebSocketConnect
        public void connected(Session s) throws IOException {
            System.out.println("connected");
        }

        @OnWebSocketClose
        public void close(Session s, int closeCode, String closeReason) {
            lock.writeLock().lock();
            try {
                Set<String> ids = subsReverse.get(s);
                for (String id: ids)
                    subs.remove(id, s);
                subsReverse.removeAll(s);
            }
            catch (Exception e) {

            }
            finally {
                lock.writeLock().unlock();
            }
        }

    }
}
