/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

/**
 * The definition of const.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 16:21
 */
public class ConnectionConst {

    /**
     * The constant SCHEDULE_THREAD_NAME.
     */
    public static final String SCHEDULE_THREAD_NAME = "Connection-Monitor-Thread";

    /**
     * The time out of connect.
     */
    public static final int DEFAULT_CONNECT_TIME_OUT_MS = 5000;

    /**
     * The constant POOLED_DEFAULT_THREAD_NAME.
     */
    public static final String POOLED_DEFAULT_THREAD_NAME = "Pooled-default-thread";

    private ConnectionConst() {

    }
}
