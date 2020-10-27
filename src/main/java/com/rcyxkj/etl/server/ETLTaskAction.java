package com.rcyxkj.etl.server;

import static com.rcyxkj.etl.tool.RedisUtils.redisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.rcyxkj.etl.error.ScheduleException;
import com.rcyxkj.etl.tool.RedisUtils;

public interface ETLTaskAction {

    public boolean doAdd(TaskEntity task) throws ScheduleException;

    public boolean doDelete(TaskEntity task) throws ScheduleException;

    public boolean doUpdate(TaskEntity task) throws ScheduleException;
    

    /**
     * 获取最少任务存活服务节点
     * 
     * @return
     */
    default String selectNode() {
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

    default Set<String> getActiveNodesByHb() {
        return RedisUtils.getActiveNodesByHb();
    }
}
