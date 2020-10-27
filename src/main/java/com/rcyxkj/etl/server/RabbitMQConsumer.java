package com.rcyxkj.etl.server;

import com.alibaba.fastjson.JSON;
import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.rcyxkj.etl.error.ScheduleException;
import com.rcyxkj.etl.tool.LogTool;
import com.zzq.dolls.mq.rabbit.RabbitConsumer;

public class RabbitMQConsumer implements Runnable {

    @Override
    public void run() {

        LogTool.logInfo(1, "consumer is starting.");

        try {
            RabbitConsumer rabbitConsumer = RabbitConsumer.builder().host(TSMConf.rabbitMqHostName)
                    .port(TSMConf.rabbitMqPort).user(TSMConf.rabbitMqUsername).password(TSMConf.rabbitMqPassword)
                    .topic(TSMConf.rabbitMqQueueName).build();

            rabbitConsumer.message(body -> {
                String task = new String(body);
                TaskEntity taskEntity = JSON.parseObject(task, TaskEntity.class);
                ETLTaskTool.taskStore(taskEntity);
                LogTool.logInfo(2, "recv and store task : " + task);
                try {
                    // TODO:任务执行
                    // 1.task schedule
                    ETLTaskTool.handle(taskEntity);
                    return true;
                } catch (ScheduleException e) {
                    // TODO 调度异常？重新调度，还是重新消费？
                    LogTool.logInfo(2, "task: task_id = " + taskEntity.getTask_id() + ", err: " + e.getMessage());
                    return false;
                } catch (Exception e) {
                    // TODO 未知异常，跳过？
                    LogTool.logInfo(2, "task: task_id = " + taskEntity.getTask_id() + ", err: " + e.getMessage());
                    return true;
                }
            });

            rabbitConsumer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
