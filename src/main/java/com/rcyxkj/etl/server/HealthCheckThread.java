package com.rcyxkj.etl.server;

import static com.rcyxkj.etl.tool.RedisUtils.redisPool;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;
import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.rcyxkj.etl.tool.LogTool;
import com.rcyxkj.etl.tool.RedisUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * 1.check etl server nodes，when one node has down migrate tasks on this node to healthy node.;
 */
public class HealthCheckThread extends TimerTask {
    public static Set<String> serverNodes = new HashSet<>();
    public static Set<String> nodesActive = new HashSet<>();
    public static Set<String> nodesMightDown = new HashSet<>();
    public static Map<String, AtomicInteger> nodeDownTimeHash = new HashMap<>();
    private RabbitMQConsumer rabbitMQConsumer;

    public HealthCheckThread(RabbitMQConsumer rabbitMQConsumer){
        this.rabbitMQConsumer = rabbitMQConsumer;
        LogTool.logInfo(1, "health check thread start.");
    }

    @Override
    public void run() {
        // 1.检查node状态
        redisPool.jedis(jedis -> {
            synchronized (serverNodes){
                serverNodes.clear();
                Set<String> ss = jedis.smembers(TSMConf.serverNodes);
                for (String s : ss) {
                    if (s.endsWith("-process")){
                        serverNodes.add(s.split("-")[0]);
                    }else {
                        serverNodes.add(s);
                    }
                }
            }
            for (String serverNode : serverNodes) {
                if (!jedis.exists(serverNode + TSMConf.heartbeatsPre)) {
                    nodesActive.remove(serverNode);
                    nodesMightDown.add(serverNode);
                } else {
                    nodesActive.add(serverNode);
                    AtomicInteger a = nodeDownTimeHash.get(serverNode);
                    if (a != null){
                        a.set(0);
                    }
                }
            }
            synchronized (rabbitMQConsumer){
                if (nodesActive.size()>0)
                    rabbitMQConsumer.notifyAll();
            }
            // 2. at first do not migrate, until over time
            if (nodesMightDown.size() > 0) {
                if (nodesActive.size() > 0) {
                    for (String downNode : nodesMightDown) {
                        AtomicInteger downTime = nodeDownTimeHash.get(downNode);
                        if (downTime == null){
                            nodeDownTimeHash.put(downNode, new AtomicInteger(1));
                            downTime = nodeDownTimeHash.get(downNode);
                        }else {
                            downTime.incrementAndGet();
                        }
//                        jedis.hincrBy(TSMConf.nodeDownTime, downNode, 10);
//                        long downTime = Long.parseLong(jedis.hget(TSMConf.nodeDownTime, downNode));
                        if (downTime.get()*10 >= TSMConf.migrateDownTime) {
                             doMigrate(downNode);
                        }
                    }
                } else {
                    LogTool.logInfo(1, "all servers down!");
                }
            } else {
                // do anything
            }
            return null;
        });
    }

    /**
     *
     * @param downNode
     */
    private void doMigrate(String downNode){
        LogTool.logInfo(1, "node " + downNode + " is down , do migrate.");
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
//                Pipeline pipeline = jedis.pipelined();
//                //                pipeline.lpush(TSMConf.nodeTasksPre + node, JSON.toJSONString(task));
//                pipeline.hdel(TSMConf.taskIdToNode, taskId, downNode);
//                pipeline.srem(TSMConf.nodeTasksSetPre + downNode, taskId);
//
//                pipeline.hset(TSMConf.taskIdToNode, taskId, newNode);
//                pipeline.sadd(TSMConf.nodeTasksSetPre + newNode, taskId);
//                pipeline.lpush(TSMConf.nodeTasksListPre + newNode, JSON.toJSONString(taskEntity) );
//                pipeline.sync();
                jedis.hdel(TSMConf.taskIdToNode, taskId, downNode);
                jedis.srem(TSMConf.nodeTasksSetPre + downNode, taskId);
                jedis.hset(TSMConf.taskIdToNode, taskId, newNode);
                jedis.sadd(TSMConf.nodeTasksSetPre + newNode, taskId);
                jedis.lpush(TSMConf.nodeTasksListPre + newNode, JSON.toJSONString(taskEntity) );
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
