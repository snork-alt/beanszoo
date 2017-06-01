package com.dataheaps.beanszoo.web;

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by admin on 21/2/17.
 */

@AllArgsConstructor @NoArgsConstructor
public class CorsHandler extends AbstractHandler {


    static final int HTTP_OK = 200;
    static final String OptionsHttpVerb = "OPTIONS";

    static final String AccessControlAllowOrigin = "Access-Control-Allow-Origin";
    static final String AccessControlAllowMethods = "Access-Control-Allow-Methods";
    static final String AccessControlAllowHeaders = "Access-Control-Allow-Headers";
    static final String AccessControlExposeHeaders = "Access-Control-Expose-Headers";
    static final String AccessControlMaxAge = "Access-Control-Max-Age";
    static final String AccessControlAllowCredentials = "Access-Control-Allow-Credentials";


    @Getter @Setter List<String> allowedVerbs;
    @Getter @Setter String allowedOrigin;
    @Getter @Setter List<String> allowedHeaders;
    @Getter @Setter int maxAge;
    @Getter @Setter boolean allowCredentials;

    @Override
    public void handle(String s, Request request, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        if (!request.getMethod().toUpperCase().equals(OptionsHttpVerb)) {
            request.setHandled(false);
            return;
        }

        resp.setStatus(200);
        resp.addHeader(AccessControlAllowOrigin, allowedOrigin);
        resp.addHeader(AccessControlAllowMethods, Joiner.on(", ").join(allowedVerbs));
        resp.addHeader(AccessControlAllowHeaders, Joiner.on(", ").join(allowedHeaders));
        resp.addHeader(AccessControlExposeHeaders, Joiner.on(", ").join(allowedHeaders));
        resp.addHeader(AccessControlMaxAge, Integer.toString(maxAge));
        resp.addHeader(AccessControlAllowCredentials, Boolean.toString(allowCredentials));
        request.setHandled(true);

    }
}
