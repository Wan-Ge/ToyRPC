/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zxcvs.client;

import com.zxcvs.exception.RequestTimeoutException;
import com.zxcvs.protocol.RpcRequest;
import com.zxcvs.protocol.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RpcFuture for async RPC call
 *
 * @author Xiaohui Yang
 * Create at 2018/8/28 16:22
 */

@Slf4j
public class RpcFuture implements Future<Object> {

    private Sync sync;

    private RpcRequest rpcRequest;
    private RpcResponse rpcResponse;
    private Long startTime;
    private static final Long RESPONSE_TIME_OUT = 5000L;

    private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    public RpcFuture(RpcRequest request) {
        this.sync = new Sync();
        this.rpcRequest = request;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * TODO: finish it
     *
     * @param mayInterruptIfRunning true of false
     * @return whether cancel success
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO: finish it
     *
     * @return cancelled for true
     */
    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() {
        sync.acquire(-1);
        return rpcResponse != null ? rpcResponse.getResult() : null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (rpcResponse != null) {
                return rpcResponse.getResult();
            } else {
                return null;
            }
        } else {
            log.warn("Client call timeout!");
            throw new RequestTimeoutException(rpcRequest.getRequestId(), rpcRequest.getClassName(), rpcRequest.getMethodName());
        }
    }

    private void done(RpcResponse response) {
        this.rpcResponse = response;
        sync.release(1);

    }

    private void invokeCallbacks() {
        /*lock.lock();
        try {
            pendingCallbacks.forEach(callback -> );
        }*/
    }

    private void runCallback(final AsyncRPCCallback callback) {
        final RpcResponse res = this.rpcResponse;

    }


    /**
     * Base of synchronization control for this lock. Uses AQS state to
     * represent the status of this lock.
     */
    static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1413726105887788330L;

        /**
         * future status - done & pending
         */
        private final int done = 1;

        private final int pending = 0;

        /**
         * try acquire, judge current status
         *
         * @param acquire done
         * @return success if current status is pending
         */
        @Override
        protected boolean tryAcquire(int acquire) {
            return getState() == acquire;
        }

        /**
         * set status to done by CAS
         *
         * @param acquire done
         * @return result of release
         */
        @Override
        protected boolean tryReleaseShared(int acquire) {
            if (getState() == pending) {
                return compareAndSetState(pending, done);
            } else {
                return false;
            }
        }

        boolean isDone() {
            return getState() == done;
        }
    }
}
