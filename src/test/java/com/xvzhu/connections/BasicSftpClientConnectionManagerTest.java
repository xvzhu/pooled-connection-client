package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import com.xvzhu.connections.mockserver.SftpServer;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import com.xvzhu.connections.sftp.SftpImplTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 17:02
 */
public class BasicSftpClientConnectionManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SftpImplTest.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private SftpServer sftpServer;
    private ISftpConnection sftpConnection;
    private int port;

    @Before
    public void sftpImplTest() throws InterruptedException, ConnectionException {
        LOG.error("Begin to start server.");
        sftpServer = new SftpServer();
        String uuid = sftpServer.getUuid();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        sftpServer.setupSftpServer(uuid, countDownLatch);
        countDownLatch.await();
         port = sftpServer.getPort(uuid);
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        sftpConnection = SftpConnectionFactory.builder().connectionBean(connectionBean).build().create();
    }

    @After
    public void shutdownSftp() {
        LOG.error("Begin to shutdown server.");
        sftpServer.shutdown();
        sftpConnection.getChannelSftp().disconnect();
    }

    @Test
    public void should_successfully_create_connection_when_create_new_basic_manager() throws ConnectionException {
        IConnectionManager manager = BasicSftpClientConnectionManager.builder()
                .setMaxConnectionSize(8)
                .setAutoInspect(false)
                .setBorrowTimeoutMS(36000)
                .setIdleTimeoutSecond(300000)
                .setSchedulePeriodTimeMS(6000L)
                .setConnectionTimeoutMs(60000)
                .build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        try {
            ISftpConnection sftpConnection = (ISftpConnection) manager.borrowConnection(connectionBean);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
        }
    }

    @Test
    public void should_failed_to_borrow_connection_when_connections_size_over_limit() throws ConnectionException, InterruptedException {
        expectedException.expect(ConnectionException.class);
        expectedException.expectMessage("Failed to borrow connection because of to much connections.");
        IConnectionManager manager = BasicSftpClientConnectionManager.builder()
                .setMaxConnectionSize(1)
                .setAutoInspect(false)
                .build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            new Thread(() -> {
                try {
                    manager.borrowConnection(connectionBean);
                } catch (ConnectionException e) {
                    LOG.error("Failed to borrow the first connection.");
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            countDownLatch.await();
            manager.borrowConnection(connectionBean);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
            BasicSftpClientConnectionManager.builder()
                    .setMaxConnectionSize(8)
                    .build();
        }
    }

    @Test
    public void should_not_borrow_when_the_connection_was_borrowed() throws ConnectionException {
        IConnectionManager manager = BasicSftpClientConnectionManager.builder()
                .setAutoInspect(false)
                .build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        try {
            ISftpConnection sftpConnection = (ISftpConnection) manager.borrowConnection(connectionBean);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);

            expectedException.expect(ConnectionException.class);
            expectedException.expectMessage(String.format(Locale.ENGLISH,
                    "The host : %s, thread :%s's connection has been borrowed!",
                    connectionBean.getHost(), Thread.currentThread().getName()));
            manager.borrowConnection(connectionBean);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
        }
    }

    @Test
    public void should_borrow_when_the_connection_was_idle() throws ConnectionException {
        IConnectionManager manager = BasicSftpClientConnectionManager.builder()
                .setAutoInspect(false)
                .build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        try {
            ISftpConnection sftpConnection = (ISftpConnection) manager.borrowConnection(connectionBean);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);

            manager.releaseConnection(connectionBean);
            ISftpConnection sftpConnection1 = (ISftpConnection) manager.borrowConnection(connectionBean);
            assertSame(sftpConnection, sftpConnection1);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
        }
    }

    @Test
    public void should_borrow_when_the_connection_was_shutdown() throws ConnectionException {
        IConnectionManager manager = BasicSftpClientConnectionManager.builder()
                .setAutoInspect(false)
                .build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        try {
            ISftpConnection sftpConnection = (ISftpConnection) manager.borrowConnection(connectionBean);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);

            manager.closeConnection(connectionBean);
            ISftpConnection sftpConnection1 = (ISftpConnection) manager.borrowConnection(connectionBean);
            assertTrue(sftpConnection1.currentDirectory().length() > 0);
            assertNotSame(sftpConnection, sftpConnection1);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
        }
    }

    @Test
    public void should_auto_release_connections_when_connections_borrow_timed_out() throws ConnectionException, InterruptedException {
        IConnectionManager manager = BasicSftpClientConnectionManager.builder()
                .setBorrowTimeoutMS(1)
                .setIdleTimeoutSecond(100000)
                .setSchedulePeriodTimeMS(10)
                .setAutoInspect(true)
                .build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        try {
            ISftpConnection sftpConnection = (ISftpConnection) manager.borrowConnection(connectionBean);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);

            Thread.sleep(10);
            ISftpConnection sftpConnection1 = (ISftpConnection) manager.borrowConnection(connectionBean);
            assertTrue(sftpConnection1.currentDirectory().length() > 0);
            assertSame(sftpConnection, sftpConnection1);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
            BasicSftpClientConnectionManager.builder()
                    .setBorrowTimeoutMS(3600000)
                    .setIdleTimeoutSecond(100000)
                    .setSchedulePeriodTimeMS(60000)
                    .setAutoInspect(false)
                    .build();
        }
    }

    @Test
    public void should_auto_shutdown_connections_when_connections_idle_timed_out() throws ConnectionException, InterruptedException {
        IConnectionManager manager = BasicSftpClientConnectionManager.builder()
                .setBorrowTimeoutMS(1)
                .setIdleTimeoutSecond(1)
                .setSchedulePeriodTimeMS(10)
                .setAutoInspect(true)
                .build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        try {
            ISftpConnection sftpConnection = (ISftpConnection) manager.borrowConnection(connectionBean);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);

            Thread.sleep(100);
            ISftpConnection sftpConnection1 = (ISftpConnection) manager.borrowConnection(connectionBean);
            assertTrue(sftpConnection1.currentDirectory().length() > 0);
            assertNotSame(sftpConnection, sftpConnection1);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
            BasicSftpClientConnectionManager.builder()
                    .setBorrowTimeoutMS(3600000)
                    .setIdleTimeoutSecond(100000)
                    .setSchedulePeriodTimeMS(60000)
                    .setAutoInspect(false)
                    .build();
        }
    }
}