package com.rcyxkj.etl.server;

import redis.clients.jedis.Pipeline;

import com.alibaba.fastjson.JSON;
import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.rcyxkj.etl.error.ScheduleException;
import com.rcyxkj.etl.tool.LogTool;

import static com.rcyxkj.etl.tool.RedisUtils.redisPool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * schedule thread,implement through redis
 */
public class AsyncETLTaskAction implements ETLTaskAction {

    @Override
    public boolean doAdd(TaskEntity task) throws ScheduleException {
        String taskId = task.getTask_id();
        String mId = task.getM_id();
        String node = selectNode();
        if (node == null) {
            throw new ScheduleException("select node error, node=null.");
        }
        redisPool.jedis(jedis -> {
            // Assign to selected node
//            Pipeline pipeline = jedis.pipelined();
//            pipeline.lpush(TSMConf.nodeTasksListPre + node, JSON.toJSONString(task));
//            pipeline.sadd(TSMConf.nodeTasksSetPre + node, taskId);
//            pipeline.hset(TSMConf.taskIdToNode, taskId, node);
//            pipeline.rpush(TSMConf.taskHandle + taskId, LogTool.getTime() + "action '" +
//                    ETLTaskTool.TaskAction.ADD + "'," + task.toString());//任务操作审计
//            pipeline.sync();
            jedis.lpush(TSMConf.nodeTasksListPre + node, JSON.toJSONString(task));
            jedis.sadd(TSMConf.nodeTasksSetPre + node, taskId);
            jedis.hset(TSMConf.taskIdToNode, taskId, node);
            jedis.rpush(TSMConf.taskHandle + taskId, LogTool.getTime() + "action '" +
                    ETLTaskTool.TaskAction.ADD + "'," + task.toString());//任务操作审计
            return null;
        });
        LogTool.logInfo(1, "task(" + task.getM_id() + ") -> " + node);
        async(mId);
        return false;
    }


    @Override
    public boolean doUpdate(TaskEntity task) throws ScheduleException {
        String taskId = task.getTask_id();
        String mId = task.getM_id();
        redisPool.jedis(jedis -> {
            try {
                String node = jedis.hget(TSMConf.taskIdToNode, taskId);
                if (node == null) {
                    LogTool.logInfo(1, "taskId=" + taskId + ", all server nodes have no this task, "
                            + "action 'update' do not pull to server.");
                } else {
//                    Pipeline pipeline = jedis.pipelined();
//                    pipeline.lpush(TSMConf.nodeTasksListPre + node, JSON.toJSONString(task));//推送到node的操作
//                    pipeline.rpush(TSMConf.taskHandle + taskId, LogTool.getTime() + "action '" +
//                            ETLTaskTool.TaskAction.ADD + "'," + task.toString());//任务操作审计
//                    pipeline.sync();
//                    pipeline.close();
                    jedis.lpush(TSMConf.nodeTasksListPre + node, JSON.toJSONString(task));//推送到node的操作
                    jedis.rpush(TSMConf.taskHandle + taskId, LogTool.getTime() + "action '" +
                    ETLTaskTool.TaskAction.ADD + "'," + task.toString());//任务操作审计
                }
                LogTool.logInfo(1, "task(" + task.getM_id() + ") -> " + node);
            } catch (Exception e) {
                LogTool.logInfo(1, e.getMessage());
            }
            return null;
        });
        async(mId);

        return false;
    }

    @Override
    public boolean doDelete(TaskEntity task) throws ScheduleException {
        String taskId = task.getTask_id();
        String mId = task.getM_id();
        redisPool.jedis(jedis -> {
            try {
                String node = jedis.hget(TSMConf.taskIdToNode, taskId);
                if (node == null) {
                    LogTool.logInfo(1, "taskId=" + taskId + ", all server nodes have no this task, "
                            + "action 'delete' do not pull to server.");
                } else {
//                    Pipeline pipeline = jedis.pipelined();
//                    pipeline.lpush(TSMConf.nodeTasksListPre + node, JSON.toJSONString(task));
//                    pipeline.hdel(TSMConf.taskIdToNode, taskId, node);
//                    pipeline.srem(TSMConf.nodeTasksSetPre + node, taskId);
//                    pipeline.sync();
//                    pipeline.close();
                    jedis.lpush(TSMConf.nodeTasksListPre + node, JSON.toJSONString(task));
                    jedis.hdel(TSMConf.taskIdToNode, taskId, node);
                    jedis.srem(TSMConf.nodeTasksSetPre + node, taskId);
                }
                LogTool.logInfo(1, "task(" + task.getM_id() + ") -> " + node);
            } catch (Exception e) {
                LogTool.logInfo(1, e.getMessage());
            }
            return null;
        });
        async(mId);

        return false;
    }
    private void async(String msgId) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            redisPool.jedis(jedis -> {
                String state = null;
                while (true) {
                     state = jedis.hget(TSMConf.actionState, msgId);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (state != null) {
                        if ("1".equals(state)) {
                            LogTool.logInfo(1, msgId + " execution success(1)");
                        } else {
                            LogTool.logInfo(1, msgId + " execution failed(" + state + ")");
                        }
                        break;
                    }
                }
                return null;
            });
            future.complete(msgId);
        }, msgId + "-async");
        thread.start();

        try {
            future.get(TSMConf.aysncTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
