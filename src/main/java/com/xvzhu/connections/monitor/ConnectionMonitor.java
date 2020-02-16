package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * <p>Connection Monitor.</p>
 * Contains two type to inspect the manager.<br>
 * One is triggered by get, release or close connection. Inspect the connection.<br>
 * Another is scheduled by special thread. The field of intervalTimeSecond is the schedule time.<Br>
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public class ConnectionMonitor implements IConnectionMonitor {
    private static final int DEFAULT_OBSERVERS = 2;
    private static final long DEFAULT_INTERVAL_TIME_SECOND = 60L;
    private static class ConnectionMonitorHolder {
        private static final ConnectionMonitor INSTANCE = new ConnectionMonitor();
    }

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread scheduledThread = new Thread();
            scheduledThread.setName("Connection-Monitor-Thread");
            return scheduledThread;
        }
    });

    /**
     * The Observers.
     */
    private List<IObserver> observers = new ArrayList<>(DEFAULT_OBSERVERS);

    private long intervalTimeSecond = DEFAULT_INTERVAL_TIME_SECOND;

    private ConnectionMonitor() {
        IObserver inspectObserver = new InspectObserver();
        observers.add(new LogObserver());
        observers.add(inspectObserver);
        executor.schedule(inspectObserver, intervalTimeSecond, TimeUnit.SECONDS);
    }

    /**
     * <p>Get schedule interval time (second).</p>
     *
     * @return the interval time second
     */
    public long getIntervalTimeSecond() {
        return intervalTimeSecond;
    }

    /**
     * <p>Set schedule interval time (second).</p>
     *
     * @param intervalTimeSecond the interval time second
     */
    public void setIntervalTimeSecond(long intervalTimeSecond) {
        this.intervalTimeSecond = intervalTimeSecond;
    }

     /**
     * Gets instance.
     *
     * @return the instance
     */
    public static ConnectionMonitor getInstance() {
        return ConnectionMonitorHolder.INSTANCE;
    }

    /**
     * Attach.
     *
     * @param observer the observer
     */
    public void attach(IObserver observer) {
        observers.add(observer);
    }


    /**
     * Notify observers.
     *
     * @param connectionManager the connection manager
     * @param connectionBean    the connection bean
     */
    public void notifyObservers(IConnectionManager connectionManager, ConnectionBean connectionBean) {
        for (IObserver observer : observers) {
            observer.visit(connectionManager, connectionBean);
        }
    }
}