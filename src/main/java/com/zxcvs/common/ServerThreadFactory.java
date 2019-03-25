package com.zxcvs.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * 服务器线程池工厂
 *
 * @author Xiaohui Yang
 * Create at 2019/3/25 10:45
 */

@Component
public class ServerThreadFactory implements ThreadFactory {

    private int cnt;

    private String name;

    private List<String> stats;

    public ServerThreadFactory(String name) {
        cnt = 1;
        this.name = name;
        stats = new ArrayList<>();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, name + "-Thread_" + cnt);
        cnt++;
        stats.add(String.format("Create thread %d with name %s on %s.\n", thread.getId(), thread.getName(),
                new Date()));
        return thread;
    }

    public String getStats() {
        StringBuilder buffer = new StringBuilder();
        for (String stat : stats) {
            buffer.append(stat);
        }

        return buffer.toString();
    }
}
