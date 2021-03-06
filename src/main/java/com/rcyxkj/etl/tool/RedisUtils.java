package com.rcyxkj.etl.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.zzq.dolls.redis.RedisPool;
import com.zzq.dolls.redis.mini.MiniPool;
import com.zzq.dolls.redis.module.RedisModule;

public class RedisUtils {

    public final static MiniPool redisPool;
    static {
//        redisPool = RedisPool.builder().urls(TSMConf.redisUrls)
//                .masterName(TSMConf.redisMaster)
//                .password(TSMConf.redisPass)
//                .redisMode(TSMConf.redisMode)
//                .db(TSMConf.redisDb).build();
        redisPool = RedisPool.builder()
                .urls(TSMConf.redisUrls)
                .redisMode(TSMConf.redisMode)
                .password(TSMConf.redisPass)
                .masterName(TSMConf.redisMaster)
                .db(TSMConf.redisDb)
                .build().mini();
    }

    /**
     * 所有task持久化存储列表
     * 
     * @param task
     */
    public static void taskStore(TaskEntity task) {
        String mId = task.getM_id();
        redisPool.jedis(jedis -> jedis.hset(TSMConf.allTask, mId, JSON.toJSONString(task)));
    }
    public static List<String> taskInRedis() {
        return redisPool.jedis(jedis -> jedis.hvals(TSMConf.allTask));
    }
    public static void taskInRedisDelete(String m_id) {
        redisPool.jedis(jedis -> jedis.hdel(TSMConf.allTask, m_id));
    }

    /**
     * 删除任务 数据
     * 
     * @param taskId
     */
    public static void taskDelete(String taskId) {
        redisPool.jedis(jedis -> jedis.hdel(TSMConf.allTask, taskId));
    }

    public static long nodeTasksNum(String node){
        return redisPool.jedis(jedis -> jedis.scard(TSMConf.nodeTasksSetPre + node)
        );
    }

    public static Map<String, String> nodeTaskStatus(String node){
        Map<String, String> taskStatus = new HashMap<>();
        Set<String> taskIds = redisPool.jedis(jedis -> jedis.smembers(TSMConf.nodeTasksSetPre + node));
        for (String taskId : taskIds) {
            taskStatus.put(taskId,"running");
        }
        return taskStatus;
    }

    public static List<String> taskActions(String taskId){
        return redisPool.jedis(jedis -> jedis.lrange(TSMConf.taskHandle + taskId, 0, -1));
    }
    public static Long taskActionNums(String taskId){
        return redisPool.jedis(jedis -> jedis.llen(TSMConf.taskHandle + taskId));
    }

    public static Set<String> getActiveNodesByHb() {
        // should add LivenessProbe
        return redisPool.jedis(jedis -> jedis.keys("*" + TSMConf.heartbeatsPre)).stream()
                .map(key -> key.split("-")[0])
                .collect(Collectors.toSet());
    }
}
