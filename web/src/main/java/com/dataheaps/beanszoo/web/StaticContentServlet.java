package com.dataheaps.beanszoo.web;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Created by matteo on 10/3/16.
 */
public class StaticContentServlet extends HttpServlet {


    String localBasePath;
    String defaultPage;

    ClassLoader cl = getClass().getClassLoader();

    public StaticContentServlet(String localBasePath, String defaultPage) {
        this.localBasePath = localBasePath;
        this.defaultPage = defaultPage;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getRequestURI();

        if (path.isEmpty() && !req.getRequestURI().endsWith("/")) {
            resp.sendRedirect(req.getRequestURI() + "/");
            return;
        }

        String target = (path.isEmpty() || path.equals("/")) ? defaultPage : path;

        if (localBasePath != null && (!localBasePath.trim().isEmpty())) {

            File f = Paths.get(localBasePath, target).toFile();
            if (!f.exists()) {
                f = Paths.get(localBasePath, defaultPage).toFile();
            }
            FileInputStream i = new FileInputStream(f);
            IOUtils.copy(i, resp.getOutputStream());
            i.close();

        }
        else {

            if (target.startsWith("/"))
                target = target.substring(1);

            InputStream i = cl.getResourceAsStream(target);
            if (i == null) {
                i = cl.getResourceAsStream(defaultPage);
            }
            IOUtils.copy(i, resp.getOutputStream());
            i.close();
        }

    }
}

