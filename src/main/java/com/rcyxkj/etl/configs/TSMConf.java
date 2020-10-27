package com.rcyxkj.etl.configs;

import com.zzq.dolls.config.From;

import java.util.List;

@From(name = "configs/schedule.yml", alternateNames = "schedule.yml")
public class TSMConf {

    //1.redis配置
    @From(alternateNames = "redisUrl")
    public static List<String> redisUrls;
    @From(must = false)
    public static String redisMaster;
    @From(must = false)
    public static int redisMode = 0;
    @From(must = false)
    public static int redisDb = 0;
    @From(alternateNames = "redisPass")
    public static String redisPass;
    //2.rabbitMq配置
    @From(name = "rabbitMqUsername")
    public static String rabbitMqUsername;
    @From(name = "rabbitMqPassword")
    public static String rabbitMqPassword;
    @From(name = "rabbitMqHostName")
    public static String rabbitMqHostName;
    @From(name = "rabbitMqPort")
    public static int rabbitMqPort;
    @From(name = "rabbitMqQueueName")
    public static String rabbitMqQueueName;
    //3.redis数据字段配置
    /**
     * 保存所有任务的队列
     */
    @From(must = false)
    public static String allTask = "task-all";
    /**
     * 本机IP段
     * 根据outsideIp获取本机ip,如‘10.136.’则查找包含‘10.135.’的ip为本机ip
     */
    @From(name = "outsideIp")
    public static String outsideIp;
    /**
     * expire实现服务leader选举，
     * string格式
     * key->$serverLeader，value->$serverName
     * 周期性设置过期时间为$leaderExpire
     */
    @From(must = false)
    public static String serverLeader = "ser.leader";
    /**
     * leader选举周期，即每过多久时间重新设置一次leader的过期时间
     */
    @From(name = "leaderPeriodMS", must = false)
    public static int leaderPeriod = 10;
    /**
     * leader过期时间，默认serverLeader过期时间设置为(leaderperiod+5)s
     */
    @From(name = "leaderExpireMS", must = false)
    public static String leaderExpire = "15";

    /**
     * 节点名，默认本机ip由$outsideIp设定，没有对应的ip则用hostname作为nodeName，此选项可以手动指定nodeName
     */
    @From(name = "nodeName", must = false)
    public static String nodeName;
    /**
     * leader 的节点名称
     */
    @From(must = false)
    public static String leaderName;
    @From( must = false)
    public static boolean isLeader = false;
    @From(must = false)
    public static int httpPort = 20099;
    /**
     * 节点任务队列
     * 交换服务获取任务
     */
    @From(must = false)
    public static String nodeTasksListPre = "taskin-";
//    /**
//     * 任务执行结果
//     * hash
//     * field: taskid
//     * value: 0/1
//     */
//    @From(must = false)
//    public static String resultTask = "res-task";
//
//    /**
//     * 调度超时时间
//     */
//    @From(must = false)
//    public static int resultTimeOut = 600;
//
//    /**
//     * 异步调度，获取返回结果间隔
//     */
//    @From(must = false)
//    public static int periOfCallResult = 1000;
//
//    /**
//     * 异常，待调度的任务
//     */
//    @From(must = false)
//    public static String redistributionQueue = "err-tasks";
    /**
     * 所有交换任务节点
     */
    @From(must = false)
    public static String serverNodes = "nodelist";
    /**
     * 存活节点的key值
     * heartbeats:nodes:
     */
    @From(must = false)
    public static String heartbeatsPre = "-process";

    /**
     * 节点挂掉的时长
     * hash
     * field: node
     * value: time
     */
    @From(must = false)
    public static String nodeDownTime = "down-time-ns";

    /**
     * 任务状态
     * filed: msg_id
     * value: 0/1
     */
    @From(must = false)
    public static String actionState = "actionstate";

    /**
     * 节点的任务
     */
    @From(must = false)
    public static String nodeTasksSetPre = "taskList-";

    /**
     * 任务到节点的映射
     * hash
     * key: taskIdToNode;
     * filed: taskId;
     * value: node
     */
    @From(must = false)
    public static String taskIdToNode = "tid2node";

    /**
     * 日志输出等级
     */
    @From(must = false)
    public static int logLevel = 1;

    /**
     * 任务下发采用方式，同步web，异步redis
     */
    @From(must = false)
    public static boolean isAsync = true;

    /**
     * 异步任务超时时间
     */
    @From(name = "aysncTimeoutMS" , must = false)
    public static long aysncTimeout = 30 * 1000L;
    @From(must = false)
    public static int migrateDownTime = 30;
    /**
     * 任务操作审计
     */
    @From(name = "taskHandle")
    public static String taskHandle = "task.handle.";

}
