/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.operation;

import java.util.Optional;

/**
 * The enum Protocol define.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-22 23:49
 */
public enum ProtocolDefine {
    /**
     * Sftp protocol define.
     */
    SFTP("com.xvzhu.connections.apis.protocol.ISftpConnection",
            "com.xvzhu.connections.sftp.SftpImpl",
            "com.xvzhu.connections.sftp.SftpConnectionFactory"),
    /**
     * Shell protocol define.
     */
    SHELL("com.xvzhu.connections.apis.protocol.IShellConnection",
            "com.xvzhu.connections.shell.ShellImpl",
            "com.xvzhu.connections.shell.ShellConnectionFactory");
    private String connectionType;
    private String connectionImpl;
    private String connectionFactory;

    ProtocolDefine(String connectionType, String connectionImpl, String connectionFactory) {
        this.connectionType = connectionType;
        this.connectionImpl = connectionImpl;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Gets connection type.
     *
     * @return the connection type
     */
    public String getConnectionType() {
        return connectionType;
    }

    /**
     * Gets connection.
     *
     * @return the connection
     */
    public String getConnectionImpl() {
        return connectionImpl;
    }

    /**
     * Gets connection factory.
     *
     * @return the connection factory
     */
    public String getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Parse type optional.
     *
     * @param connectionType the connection type
     * @return the optional
     */
    public static Optional<ProtocolDefine> parseType(String connectionType) {
        switch (connectionType) {
            case "com.xvzhu.connections.apis.protocol.ISftpConnection":
                return Optional.of(SFTP);
            case "com.xvzhu.connections.apis.protocol.IShellConnection":
                return Optional.of(SHELL);
            default:
                return Optional.empty();
        }
    }
}
