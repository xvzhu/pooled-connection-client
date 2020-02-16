package com.xvzhu.connections.operation;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionConst;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.sftp.SftpImpl;
import mockit.Capturing;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 23:34
 */
public class OperationFactoryTest {
    private OperationFactory operationFactory = new OperationFactory(ConnectionManagerConfig.builder().maxConnectionSize(3).build());
    private ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder().build();
    private static final int SECOND_UNIT = 1000;

    @Capturing
    private SftpImpl sftpConnection;

    @Mocked
    private Session session;

    @Mocked
    private ChannelSftp channelSftp;

    @Test
    public void should_return_consume_function_when_get_release_consumer() {
        assertThat(operationFactory.getReleaseConsumer() instanceof Consumer, is(true));
    }

    @Test
    public void should_release_current_basic_connections_when_basic_connection_reuse_time_out() {
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionBean connectionBean = new ConnectionBean("192.168.1.1", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(true)
                        .borrowTime(timeNow - (connectionManagerConfig.getReuseTimeoutSecond() * SECOND_UNIT) - 100000)
                        .build();
        managerBeanMap.put(Thread.currentThread(), managerBean);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean, managerBeanMap);

        BasicSftpClientConnectionManager.getConnections().entrySet().stream().forEach(operationFactory.getReleaseConsumer());
        assertThat(managerBean.isConnectionBorrowed(), is(false));
        long releaseInterval = (managerBean.getReleaseTime() - timeNow) / 1000;
        assertThat(releaseInterval >= 0 && releaseInterval < 3, is(true));
    }

    @Test
    public void should_not_release_current_basic_connections_when_basic_connection_reuse_time_not_out() {
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionBean connectionBean = new ConnectionBean("192.168.1.1", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(true)
                        .borrowTime(timeNow)
                        .build();
        managerBeanMap.put(Thread.currentThread(), managerBean);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean, managerBeanMap);

        BasicSftpClientConnectionManager.getConnections().entrySet().stream().forEach(operationFactory.getReleaseConsumer());
        assertThat(managerBean.isConnectionBorrowed(), is(true));
    }

    @Test
    public void should_close_current_basic_connections_when_basic_connection_close_time_out() throws JSchException {
        new Expectations() {
            {
                sftpConnection.getChannelSftp();
                result = channelSftp;
                channelSftp.disconnect();
                channelSftp.getSession();
                result = session;
                session.disconnect();
            }
        };

        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionBean connectionBean = new ConnectionBean("192.168.1.1", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(false)
                        .releaseTime(timeNow - connectionManagerConfig.getCloseTimeoutSecond() * SECOND_UNIT - 10000)
                        .sftpConnection(sftpConnection)
                        .build();
        managerBeanMap.put(Thread.currentThread(), managerBean);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean, managerBeanMap);

        BasicSftpClientConnectionManager.getConnections().entrySet().stream().forEach(operationFactory.getReleaseConsumer());
        assertThat(managerBeanMap.get(Thread.currentThread()) == null, is(true));
    }

    @Test
    public void should_not_close_current_basic_connections_when_basic_connection_close_time_not_out() throws JSchException {
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionBean connectionBean = new ConnectionBean("192.168.1.1", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(false)
                        .releaseTime(timeNow)
                        .sftpConnection(sftpConnection)
                        .build();
        managerBeanMap.put(Thread.currentThread(), managerBean);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean, managerBeanMap);

        BasicSftpClientConnectionManager.getConnections().entrySet().stream().forEach(operationFactory.getReleaseConsumer());
        assertThat(managerBeanMap.get(Thread.currentThread()) != null, is(true));
    }

    @Test
    public void should_release_all_basic_connections_when_basic_connection_reuse_time_out() {
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionBean connectionBean = new ConnectionBean("192.168.1.1", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(true)
                        .borrowTime(timeNow - (connectionManagerConfig.getReuseTimeoutSecond() * SECOND_UNIT) - 100000)
                        .build();
        managerBeanMap.put(Thread.currentThread(), managerBean);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean, managerBeanMap);

        ConnectionBean connectionBean1 = new ConnectionBean("192.168.1.2", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap1 = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean1 =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(true)
                        .borrowTime(timeNow - (connectionManagerConfig.getReuseTimeoutSecond() * SECOND_UNIT) - 100000)
                        .build();
        managerBeanMap1.put(Thread.currentThread(), managerBean1);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean1, managerBeanMap1);

        ConnectionBean connectionBean2 = new ConnectionBean("192.168.1.3", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap2 = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean2 =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(true)
                        .borrowTime(timeNow)
                        .build();
        managerBeanMap2.put(Thread.currentThread(), managerBean2);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean2, managerBeanMap2);

        Thread.currentThread().setName(ConnectionConst.SCHEDULE_THREAD_NAME);

        BasicSftpClientConnectionManager.getConnections().entrySet().stream().forEach(operationFactory.getReleaseConsumer());
        assertThat(managerBean.isConnectionBorrowed(), is(false));
        assertThat(managerBean1.isConnectionBorrowed(), is(false));
        assertThat(managerBean2.isConnectionBorrowed(), is(true));
        long releaseInterval = (managerBean1.getReleaseTime() - timeNow) / 1000;
        assertThat(releaseInterval >= 0 && releaseInterval < 3, is(true));
    }

    @Test
    public void should_close_all_basic_connections_when_basic_connection_close_time_out() throws JSchException{
        new Expectations() {
            {
                sftpConnection.getChannelSftp();
                result = channelSftp;
                channelSftp.disconnect();
                channelSftp.getSession();
                result = session;
                session.disconnect();
            }
        };
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionBean connectionBean = new ConnectionBean("192.168.1.1", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(false)
                        .sftpConnection(sftpConnection)
                        .releaseTime(timeNow - (connectionManagerConfig.getCloseTimeoutSecond() * SECOND_UNIT) - 100000)
                        .build();
        managerBeanMap.put(Thread.currentThread(), managerBean);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean, managerBeanMap);

        ConnectionBean connectionBean1 = new ConnectionBean("192.168.1.2", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap1 = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean1 =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(false)
                        .sftpConnection(sftpConnection)
                        .releaseTime(timeNow - (connectionManagerConfig.getCloseTimeoutSecond() * SECOND_UNIT) - 100000)
                        .build();
        managerBeanMap1.put(Thread.currentThread(), managerBean1);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean1, managerBeanMap1);

        ConnectionBean connectionBean2 = new ConnectionBean("192.168.1.3", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap2 = new HashMap<>();
        BasicSftpClientConnectionManager.ManagerBean managerBean2 =
                BasicSftpClientConnectionManager.ManagerBean.builder()
                        .isConnectionBorrowed(false)
                        .releaseTime(timeNow)
                        .build();
        managerBeanMap2.put(Thread.currentThread(), managerBean2);
        BasicSftpClientConnectionManager.getConnections().put(connectionBean2, managerBeanMap2);

        Thread.currentThread().setName(ConnectionConst.SCHEDULE_THREAD_NAME);

        BasicSftpClientConnectionManager.getConnections().entrySet().stream().forEach(operationFactory.getReleaseConsumer());
        assertThat(managerBeanMap.get(Thread.currentThread()) == null, is(true));
        assertThat(managerBeanMap1.get(Thread.currentThread()) == null, is(true));
        assertThat(managerBeanMap2.get(Thread.currentThread()) != null, is(true));
    }

    @Test
    public void should_close_all_basic_connections_when_basic_connections_exceed_max_limit() throws JSchException{
        new Expectations() {
            {
                sftpConnection.getChannelSftp();
                result = channelSftp;
                channelSftp.disconnect();
                channelSftp.getSession();
                result = session;
                session.disconnect();
            }
        };
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionBean connectionBean = new ConnectionBean("192.168.1.1", 22, "huawei", "huawei");
        Map<Thread, BasicSftpClientConnectionManager.ManagerBean> managerBeanMap = new HashMap<>();

        managerBeanMap.put(Thread.currentThread(), BasicSftpClientConnectionManager.ManagerBean.builder()
                .isConnectionBorrowed(false)
                .sftpConnection(sftpConnection)
                .releaseTime(timeNow)
                .build());
        managerBeanMap.put(new Thread("1"), BasicSftpClientConnectionManager.ManagerBean.builder()
                .isConnectionBorrowed(false)
                .sftpConnection(sftpConnection)
                .releaseTime(timeNow)
                .build());
        managerBeanMap.put(new Thread("2"), BasicSftpClientConnectionManager.ManagerBean.builder()
                .isConnectionBorrowed(true)
                .borrowTime(timeNow)
                .build());
        managerBeanMap.put(new Thread("3"), BasicSftpClientConnectionManager.ManagerBean.builder()
                .isConnectionBorrowed(true)
                .borrowTime(timeNow)
                .build());
        BasicSftpClientConnectionManager.getConnections().put(connectionBean, managerBeanMap);

        Thread.currentThread().setName(ConnectionConst.SCHEDULE_THREAD_NAME);

        BasicSftpClientConnectionManager.getConnections().entrySet().stream().forEach(operationFactory.getReleaseConsumer());
        assertThat(managerBeanMap.size(), is(2));
        assertThat(managerBeanMap.get(Thread.currentThread()) == null, is(true));
    }
}
