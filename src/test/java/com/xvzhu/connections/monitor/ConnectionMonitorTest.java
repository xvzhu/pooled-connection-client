package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ManagerBean;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import lombok.NonNull;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * Connection Monitor Test.
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 0:40
 */
public class ConnectionMonitorTest {
    @Test
    public void should_notify_all_observers_when_notified_from_biz() {
        IConnectionManager connectionManager = BasicSftpClientConnectionManager.builder().build();
        ConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();
        connectionMonitor.attach(new BizObserver());

        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        ManagerBean managerBean = ManagerBean.builder().build();
        managerMap.put(Thread.currentThread(), managerBean);
        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        connectionMonitor.notifyObservers(connectionManager, connectionBean, connections);

        ConnectionBean connectionBean1 = ConnectionBeanBuilder.builder().host("101.10.10.10").build().getConnectionBean();
        assertNotNull(connections.get(connectionBean1));
    }

    static class BizObserver implements IObserver{

        @Override
        public void visit(@NonNull IConnectionManager connectionManager, @NonNull ConnectionBean connectionBean, @NonNull Map<ConnectionBean, Map<Thread, ManagerBean>> connections) {
            ConnectionBean connectionBean1 = ConnectionBeanBuilder.builder().host("101.10.10.10").build().getConnectionBean();
            Map<Thread, ManagerBean> managerMap = new HashMap<>();
            ManagerBean managerBean = ManagerBean.builder().build();
            managerMap.put(Thread.currentThread(), managerBean);
            connections.put(connectionBean1, managerMap);
        }

        @Override
        public void run() {

        }
    }
}