package com.dataheaps.beanszoo.sd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Created by matteopelati on 26/10/15.
 */
public interface RemoteServiceDirectory extends ServiceDirectory {

    @RequiredArgsConstructor
    public static class RemoteService {
        @Getter final String address;
        @Getter final String id;
        @Getter final Class type;
    }

    RemoteService getRemoteService(String id, Class type);
    RemoteService getRemoteServiceByType(Class type, String name);
    Set<RemoteService> getRemoteServicesByType(Class type);



}
