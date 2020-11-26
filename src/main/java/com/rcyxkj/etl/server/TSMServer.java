package com.rcyxkj.etl.server;

import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.tool.LogTool;
import com.rcyxkj.etl.tool.RedisUtils;
import com.rcyxkj.etl.web.HttpServer;
import com.zzq.dolls.config.LoadConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Timer;

public class TSMServer {
    public static String outsideIp = null;
    public static boolean isSetOutsideIP = false;

    public static void main(String[] args) {
        try {
            // 1.加载配置
            LoadConfig.load(TSMConf.class);
            System.out.println(LoadConfig.toString(TSMConf.class)); 
            // 2.设置nodeName
            InetAddress[] a = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress ia : a) {
                if (ia.getHostAddress().contains(TSMConf.outsideIp)) {
                    LogTool.logInfo(1, "[1] Got node IP " + ia.getHostAddress() + " by hint " + TSMConf.outsideIp);
                    outsideIp = ia.getHostAddress();
                    isSetOutsideIP = true;
                }else {
                }
            }
            if (!isSetOutsideIP)
                LogTool.logInfo(1, "[1] Have not get node ip by hint " + TSMConf.outsideIp);
            if (TSMConf.nodeName == null) {
                if (isSetOutsideIP) {
                    TSMConf.nodeName = outsideIp;
                } else {
                    TSMConf.nodeName = InetAddress.getLocalHost().getHostName() + System.nanoTime();
                }
                LogTool.logInfo(1, "[2] Not set node name in config file, get name '"+ TSMConf.nodeName +"' automatically");
            }
            // 3.主备模式，leader 选举线程，所有调度任务只在master节点执行
            Timer timer = new Timer();
            ServerLeader serverLeader = new ServerLeader();
            timer.schedule(serverLeader, 10, TSMConf.leaderPeriod * 1000L);
            // 4.http服务，调度数据监控界面
            HttpServer httpServer = new HttpServer();
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LogTool.logInfo(1, "process exiting");
                if (RabbitMQConsumer.isWaiting.get()){
                    LogTool.logInfo(1, "there are msgs were consumed but no active etl servers, load for next handling.");
                    RedisUtils.taskStore(RabbitMQConsumer.msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

    }
}
