package com.dataheaps.beanszoo.lifecycle;

/**
 * Created by admin on 8/2/17.
 */
public abstract class AbstractLifeCycle implements LifeCycle {

    Status status = Status.Idle;

    @Override
    public synchronized void start() throws Exception {

        if (status != Status.Idle)
            return;

        try {
            status = Status.Starting;
            doStart();
            status = Status.Running;
        }
        catch (Exception e) {
            status = Status.Idle;
            throw e;
        }
    }

    @Override
    public synchronized void stop() throws Exception {

        if (status != Status.Running)
            return;

        try {
            status = Status.Stopping;
            doStop();
            status = Status.Idle;
        }
        catch (Exception e) {
            status = Status.Running;
            throw e;
        }

    }

    @Override
    public synchronized Status getStatus() {
        return status;
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doStop() throws Exception;



}
