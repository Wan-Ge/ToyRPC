package com.zxcvs.client;

import com.zxcvs.exception.RpcFutureCallbackException;
import com.zxcvs.exception.RpcFutureGetException;
import com.zxcvs.protocol.RpcRequest;
import com.zxcvs.protocol.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:05
 */

@Slf4j(topic = "ToyLogger")
public class RpcFuture implements Future<Object> {

    private Sync sync;

    private RpcRequest request;

    private RpcResponse response;

    private long startTime;

    private long responseTimeout = 5000L;

    private List<AsyncRpcCallBack> pendingCallbacks = new ArrayList<>();

    private ReentrantLock lock = new ReentrantLock();

    public RpcFuture(RpcRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (response != null) {
            return response.getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (request != null) {
                return response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RpcFutureGetException(request.getRequestId(), request.getClassName(), request.getMethodName());
        }
    }

    public void done(RpcResponse response) {
        this.response = response;
        sync.release(1);
        invokeCallbacks();
        // Threshold
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > responseTimeout) {
            log.warn("service response time is too slow. requestId:{}, responseTime:{}ms", response.getRequestId(),
                    responseTime);
        }
    }

    private void invokeCallbacks() {
        lock.lock();
        try {
            for (final AsyncRpcCallBack callback : pendingCallbacks) {
                runCallback(callback);
            }
        } catch (Exception e) {
            log.error("invokeCallbacks error:{}", ExceptionUtils.getStackTrace(e));
        } finally {
            lock.unlock();
        }
    }


    public RpcFuture addCallback(AsyncRpcCallBack callBack) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callBack);
            } else {
                pendingCallbacks.add(callBack);
            }
        } catch (Exception e) {
            log.error("addCallback error:{}", ExceptionUtils.getStackTrace(e));
        } finally {
            lock.unlock();
        }

        return this;
    }

    private void runCallback(final AsyncRpcCallBack callback) {
        final RpcResponse res = this.response;
        RpcClient.submit(() -> {
            if (!res.isError()) {
                callback.success(res.getResult());
            } else {
                callback.fail(new RpcFutureCallbackException("Response error", res.getThrowable()));
            }
        });
    }

    static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4267186114619934899L;

        /** future status */
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                return compareAndSetState(pending, done);
            } else {
                return false;
            }
        }

        public boolean isDone() {
            getState();
            return getState() == done;
        }
    }
}
