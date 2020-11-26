package com.rcyxkj.etl.server;

import com.alibaba.fastjson.JSON;
import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.rcyxkj.etl.error.ScheduleException;
import com.rcyxkj.etl.tool.LogTool;
import com.rcyxkj.etl.tool.RedisUtils;
import com.zzq.dolls.mq.rabbit.RabbitConsumer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RabbitMQConsumer implements Runnable {
    public static AtomicBoolean isWaiting = new AtomicBoolean(false);
    public static volatile TaskEntity msg;

    @Override
    public void run() {

        LogTool.logInfo(1, "consumer is starting.");

        try {
            RabbitConsumer rabbitConsumer = RabbitConsumer.builder()
                    .host(TSMConf.rabbitMqHostName)
                    .port(TSMConf.rabbitMqPort)
                    .user(TSMConf.rabbitMqUsername)
                    .password(TSMConf.rabbitMqPassword)
                    .topic(TSMConf.rabbitMqQueueName)
                    .build();

            rabbitConsumer.message(body -> {
                String task = new String(body);
                TaskEntity taskEntity = JSON.parseObject(task, TaskEntity.class);
                LogTool.logInfo(2, "recv and store task : " + task);
                while(true){
                    try {
                        // 1.task schedule
                        ETLTaskTool.handle(taskEntity);
                        return true;
                    } catch (ScheduleException e) {
                        LogTool.logInfo(2, "task: task_id = " + taskEntity.getTask_id() + ", err: " + e.getMessage());
                        msg = taskEntity;
                        synchronized (this){
                            try {
                                isWaiting.set(true);
                                LogTool.logInfo(1, "no etl server size=" + HealthCheckThread.nodesActive.size() + ", wait before anny node active.");
                                this.wait();
                                isWaiting.set(false);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }

                    }
                }
            });
            rabbitConsumer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean cacheDataHandle(){
        LogTool.logInfo(1, "at first load cache msg.");
        List<String> es = RedisUtils.taskInRedis();
        if (es.size() < 1)
            LogTool.logInfo(1, "cache has no msg to handle " + es.size());
        else
            LogTool.logInfo(1, "cache has " + es.size() + " msgs to handle.");
        for (String task : es) {
            TaskEntity taskEntity = JSON.parseObject(task, TaskEntity.class);
            while (true){
                try {
                    ETLTaskTool.handle(taskEntity);
                    RedisUtils.taskInRedisDelete(taskEntity.getM_id());
                    break;
                } catch (ScheduleException e) {
                    e.printStackTrace();
                    synchronized (this){
                        try {
                            LogTool.logInfo(1, "when load cache msg , there is no active etl server" +
                                    " size " + HealthCheckThread.nodesActive.size()+",wait for a active etl server.");
                            this.wait();
                            break;
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

}
