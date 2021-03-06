/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.sftp;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import com.xvzhu.connections.mockserver.SftpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 0:39
 */
public class SftpImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(SftpImplTest.class);
    private static final int BYTE_DEFAULT_SIZE = 4096;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private SftpServer sftpServer;
    private ISftpConnection sftpConnection;

    @Before
    public void sftpImplTest() throws InterruptedException, ConnectionException {
        LOG.error("Begin to start server.");
        sftpServer = new SftpServer();
        String uuid = sftpServer.getUuid();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        sftpServer.setupSftpServer(uuid, countDownLatch);
        countDownLatch.await();
        int port = sftpServer.getPort(uuid);
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        sftpConnection = new SftpImpl();
        sftpConnection.connect(connectionBean, 10000);
    }

    @After
    public void shutdownSftp() throws ConnectionException{
        LOG.error("Begin to shutdown server.");
        sftpServer.shutdown();
        sftpConnection.disconnect();
    }

    @Test
    public void should_create_connection_when_with_correct_information() throws ConnectionException{
        assertNotNull(sftpConnection.getChannelSftp());
        assertNotNull(sftpConnection.currentDirectory());
    }

    @Test
    public void should_return_current_directory_when_query_pwd() throws ConnectionException{
        File file = new File("");
        LOG.error("current file : {}", file.getAbsolutePath());
        LOG.error("Sftp path is : {}", sftpConnection.currentDirectory());
        String localFilePath = file.getAbsolutePath().replace("\\", "").replace("/", "");
        String sftpFilePath = sftpConnection.currentDirectory().replace("\\", "").replace("/", "");
        assertThat(localFilePath, is(sftpFilePath));
    }

    @Test
    public void should_return_true_when_check_current_directory_is_directory() throws ConnectionException{
        LOG.error("Sftp path is : {}", sftpConnection.currentDirectory());
        assertThat(sftpConnection.isDirectory(sftpConnection.currentDirectory()), is(true));
    }

    @Test
    public void should_return_false_when_check_not_exists_directory() throws ConnectionException{
        String path = sftpConnection.currentDirectory() + "/test";
        sftpConnection.mkdirs(path);
        assertThat(sftpConnection.isDirectory(path), is(true));
        sftpConnection.deleteDirectory(path);
        assertThat(sftpConnection.isDirectory(path), is(false));
    }

    @Test
    public void should_mkdir_when_dir_not_exists() throws ConnectionException{
        String path = sftpConnection.currentDirectory() + "/test";
        sftpConnection.mkdirs(path);
        assertThat(sftpConnection.isDirectory(path), is(true));
        sftpConnection.deleteDirectory(path);
        assertThat(sftpConnection.isDirectory(path), is(false));
    }

    @Test
    public void should_make_nested_dir_when_dir_not_exists() throws ConnectionException{
        String parentPath = sftpConnection.currentDirectory() + "/com";
        String deepPath = parentPath + "/test";

        sftpConnection.mkdirs(deepPath);
        assertThat(sftpConnection.isDirectory(deepPath), is(true));
        sftpConnection.deleteDirectory(deepPath);
        assertThat(sftpConnection.isDirectory(deepPath), is(false));
        sftpConnection.deleteDirectory(parentPath);
        assertThat(sftpConnection.isDirectory(parentPath), is(false));
    }

    @Test
    public void should_successfully_when_upload_file() throws ConnectionException, IOException {
        byte[] input = "Go go go, fire in the hole".getBytes();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(input));
        sftpConnection.upload(sftpConnection.currentDirectory(), "test.txt", inputStream);
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(true));

        byte[] download = inputStreamToByteArray(sftpConnection.download(sftpConnection.currentDirectory(), "test.txt"));
        assertThat(new String(input).equals(new String(download)), is(true));

        sftpConnection.deleteFile(sftpConnection.currentDirectory(), "test.txt");
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(false));
    }

    @Test
    public void should_successfully_when_download_file() throws ConnectionException, IOException {
        byte[] input = "Go go go, fire in the hole".getBytes();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(input));
        sftpConnection.upload(sftpConnection.currentDirectory(), "test.txt", inputStream);
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(true));

        byte[] download = inputStreamToByteArray(sftpConnection.download(sftpConnection.currentDirectory(), "test.txt"));
        assertThat(new String(input).equals(new String(download)), is(true));

        sftpConnection.deleteFile(sftpConnection.currentDirectory(), "test.txt");
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(false));
    }

    @Test
    public void should_throw_connection_exception_when_download_non_exists_dir() throws ConnectionException, IOException {
        expectedException.expect(ConnectionException.class);
        sftpConnection.download(sftpConnection.currentDirectory() + "/test/test", "test.txt");
    }

    @Test
    public void should_throw_connection_exception_when_download_non_exists_file() throws ConnectionException, IOException {
        expectedException.expect(ConnectionException.class);
        sftpConnection.download(sftpConnection.currentDirectory(), "test.txt");
    }

    @Test
    public void should_return_true_when_file_exists() throws ConnectionException, IOException {
        byte[] input = "Go go go, fire in the hole".getBytes();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(input));
        sftpConnection.upload(sftpConnection.currentDirectory(), "test.txt", inputStream);
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(true));

        byte[] download = inputStreamToByteArray(sftpConnection.download(sftpConnection.currentDirectory(), "test.txt"));
        assertThat(new String(input).equals(new String(download)), is(true));

        sftpConnection.deleteFile(sftpConnection.currentDirectory(), "test.txt");
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(false));
    }

    @Test
    public void should_successfully_when_delete_exists_file() throws ConnectionException, IOException {
        byte[] input = "Go go go, fire in the hole".getBytes();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(input));
        sftpConnection.upload(sftpConnection.currentDirectory(), "test.txt", inputStream);
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(true));

        byte[] download = inputStreamToByteArray(sftpConnection.download(sftpConnection.currentDirectory(), "test.txt"));
        assertThat(new String(input).equals(new String(download)), is(true));

        sftpConnection.deleteFile(sftpConnection.currentDirectory(), "test.txt");
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test.txt"), is(false));
    }

    @Test
    public void should_return_directly_when_delete_non_exists_file() throws ConnectionException, IOException {
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test/test.txt"), is(false));
        sftpConnection.deleteFile(sftpConnection.currentDirectory() + "/test", "test.txt");
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test/test.txt"), is(false));
    }

    @Test
    public void should_successfully_when_delete_exists_directory() throws ConnectionException, IOException {
        String path = sftpConnection.currentDirectory() + "/test";
        sftpConnection.mkdirs(path);
        assertThat(sftpConnection.isDirectory(path), is(true));

        sftpConnection.deleteDirectory(path);
        assertThat(sftpConnection.isDirectory(path), is(false));
    }

    @Test
    public void should_return_directly_when_delete_non_exists_dir() throws ConnectionException, IOException {
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test"), is(false));
        sftpConnection.deleteDirectory(sftpConnection.currentDirectory() + "/test");
        assertThat(sftpConnection.isExist(sftpConnection.currentDirectory() + "/test"), is(false));
    }

    @Test
    public void should_rename_successfully_when_input_correct_name_to_change() throws ConnectionException{
        String currentPath = sftpConnection.currentDirectory();
        String path = currentPath + "/test";
        sftpConnection.mkdirs(path);
        assertThat(sftpConnection.isDirectory(path), is(true));
        String pathNew = currentPath + "/com";
        sftpConnection.rename(path, pathNew);
        assertThat(sftpConnection.isDirectory(pathNew), is(true));

        sftpConnection.deleteDirectory(pathNew);
        assertThat(sftpConnection.isDirectory(pathNew), is(false));
    }

    @Test
    public void should_failed_to_rename_when_input_wrong_name_to_change() throws ConnectionException{
        expectedException.expect(ConnectionException.class);
        expectedException.expectMessage("Failed to rename the file!");
        String currentPath = sftpConnection.currentDirectory();
        String path = currentPath + "/test";
        String pathNew = currentPath + "/com";
        sftpConnection.rename(path, pathNew);
    }

    @Test
    public void should_return_files_when_list_file() throws ConnectionException{
        byte[] input = "Go go go, fire in the hole".getBytes();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(input));
        String path = sftpConnection.currentDirectory() + "/test";

        sftpConnection.upload(path, "test.txt", inputStream);
        assertThat(sftpConnection.isFile(sftpConnection.currentDirectory() + "/test.txt"), is(true));

        List<String> files = sftpConnection.list(sftpConnection.currentDirectory());
        // conaint . and .. path.
        assertThat(files.size(), is(3));

        sftpConnection.deleteFile(path, "test.txt");
        assertThat(sftpConnection.isExist(path + "/test.txt"), is(false));

        sftpConnection.deleteDirectory(path);
        assertThat(sftpConnection.isExist(path), is(false));
    }

    @Test
    public void should_return_empty_collection_when_list_file_not_exists() throws ConnectionException {
        assertThat(sftpConnection.isFile(sftpConnection.currentDirectory() + "/test.txt"), is(false));
        List<String> files = sftpConnection.list(sftpConnection.currentDirectory()+"/test");
        assertThat(files.size(), is(0));
    }

    @Test
    public void should_return_closed_status_when_connection_was_closed() throws ConnectionException {
        assertTrue(sftpConnection.isValid());
        assertFalse(sftpConnection.isClosed());
        sftpConnection.disconnect();
        assertFalse(sftpConnection.isValid());
        assertTrue(sftpConnection.isClosed());
    }

    /**
     * convert input stream to byte array.
     *
     * @param in input
     * @return byte[]
     * @throws IOException the io exception.
     */
    private byte[] inputStreamToByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BYTE_DEFAULT_SIZE];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}