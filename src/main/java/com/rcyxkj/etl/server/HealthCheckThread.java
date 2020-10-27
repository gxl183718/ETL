package com.rcyxkj.etl.server;

import static com.rcyxkj.etl.tool.RedisUtils.redisPool;

import java.util.*;

import com.alibaba.fastjson.JSON;
import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.rcyxkj.etl.tool.LogTool;
import com.rcyxkj.etl.tool.RedisUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * 1.检查交换任务节点服务健康状况，节点down掉后迁移节点上的任务至健康节点
 */
public class HealthCheckThread extends TimerTask {
    public static Set<String> serverNodes = new HashSet<>();
    public static Set<String> nodesActive = new HashSet<>();
    public static Set<String> nodesMightDown = new HashSet<>();

    @Override
    public void run() {
        // 1.检查node状态
        redisPool.jedis(jedis -> {
            synchronized (serverNodes){
                serverNodes.clear();
                serverNodes.addAll(jedis.smembers(TSMConf.serverNodes));
            }
            for (String serverNode : serverNodes) {
                if (!jedis.exists(serverNode + TSMConf.heartbeatsPre)) {
                    nodesActive.remove(serverNode);
                    nodesMightDown.add(serverNode);
                } else {
                    nodesActive.add(serverNode);
                }
            }
            // 2.判断might down节点挂掉的时间，down掉一定时间没有恢复迁移这个节点的任务
            if (nodesMightDown.size() > 0) {
                if (nodesActive.size() > 0) {
                    for (String downNode : nodesMightDown) {
                        jedis.hincrBy(TSMConf.nodeDownTime, downNode, 10);
                        long downTime = Long.parseLong(jedis.hget(TSMConf.nodeDownTime, downNode));
                        if (downTime >= TSMConf.migrateDownTime) {
                             doMigrate(downNode);
                        }
                    }
                } else {
                    LogTool.logInfo(1, "all servers down!");
                }
            } else {
                // 不需要迁移
            }
            return null;
        });
    }

    /**
     *
     * @param downNode
     */
    private void doMigrate(String downNode){
        Set<String> tasksId = redisPool.jedis(jedis -> {
           return jedis.smembers(TSMConf.nodeTasksSetPre + downNode);
        });
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setState(0);
        for (String taskId : tasksId) {
            taskEntity.setTask_id(taskId);
            String newNode = selectNode();
            redisPool.jedis(jedis -> {
                // 任务分配到节点
                Pipeline pipeline = jedis.pipelined();
                //                pipeline.lpush(TSMConf.nodeTasksPre + node, JSON.toJSONString(task));

                pipeline.hdel(TSMConf.taskIdToNode, taskId, downNode);
                pipeline.srem(TSMConf.nodeTasksSetPre + downNode, taskId);

                pipeline.hset(TSMConf.taskIdToNode, taskId, newNode);
                pipeline.sadd(TSMConf.nodeTasksSetPre + newNode, taskId);
                pipeline.lpush(TSMConf.nodeTasksListPre + newNode, JSON.toJSONString(taskEntity) );
                return null;
            });
        }
    }
    static String selectNode() {
        Set<String> node = getActiveNodesByHb();
        Map<String, Long> map = new HashMap<>();
        long min_node_id = redisPool.jedis(jedis -> {
            long min = Long.MAX_VALUE;
            for (String s : node) {
                long len = jedis.scard(TSMConf.nodeTasksSetPre + s);
                map.put(s, len);
                min = len < min ? len : min;
            }
            return min;
        });
        String selectedNode = null;
        for (Map.Entry<String, Long> stringLongEntry : map.entrySet()) {
            if (stringLongEntry.getValue() == min_node_id)
                selectedNode = stringLongEntry.getKey();
        }
        return selectedNode;
    }

    static Set<String> getActiveNodesByHb() {
        return RedisUtils.getActiveNodesByHb();
    }
}
