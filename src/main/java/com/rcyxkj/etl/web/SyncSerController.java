package com.rcyxkj.etl.web;

import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.server.HealthCheckThread;
import com.rcyxkj.etl.tool.LogTool;
import com.rcyxkj.etl.tool.RedisUtils;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class SyncSerController {

    public static void getNodeAll(Context context) {
        StringBuilder sb = new StringBuilder();
        for (String node : HealthCheckThread.nodesActive) {
            sb.append("<tr>");
            sb.append("<td>" + node + "</td>");
            sb.append("<td><font color=\"black\">running</font> </td>");
            sb.append("<td><a href=\"http://localhost:" + TSMConf.httpPort + "/api/node/" +
                    node + "\">节点任务状态(" + RedisUtils.nodeTasksNum(node)  +")</a></td>");
            sb.append("</tr>");
        }
        for (String node : HealthCheckThread.nodesMightDown) {
            sb.append("<tr>");
            sb.append("<td>" + node + "</td>");
            sb.append("<td><font color=\"red\">running</font> </td>");
            sb.append("<td><a href=\"http://localhost:" + TSMConf.httpPort + "/api/node/" +
                    node + "\">节点任务状态(" + RedisUtils.nodeTasksNum(node)  +")</a></td>");
            sb.append("</tr>");
        }
        LogTool.logInfo(2, sb.toString());
        String page =
                "<HTML>" +
                "  <HEAD>" +
                "    <TITLE>调度监控界面</TITLE>" +
                "  </HEAD>" +
                "  <BODY>" +
                "    <H1> 节点状态监控</H1>" +
                "      <table border=\"1\" cellspacing=\"0\" cellpadding=\"10\">" +
                "      <tbody>" +
                "        <tr>" +
                "          <th>节点</td>" +
                "          <th>状态</td>" +
                "          <th>详情</td>" +
                "        </tr>" +
                        sb.toString() +
                "      </tbody>" +
                "      </table>" +
                "  </BODY>" +
                "</HTML>";
        context.html(page);
    }

    public static void nodeTasksStatus(Context context) {
        StringBuilder sb = new StringBuilder();
        String node = context.pathParam("nodeName");
        Map<String, String> map = RedisUtils.nodeTaskStatus(node);
        for (Map.Entry<String, String> entry : map.entrySet()){
            sb.append("<tr>");
            sb.append("<td>" + entry.getKey() + "</td>");
            String color = "black";
            if (!"running".equals(entry.getValue())){
                color = "red";
            }
            sb.append("<td><font color=\""+color+"\">"+entry.getValue()+"</font> </td>");
            sb.append("<td><a href=\"http://localhost:" + TSMConf.httpPort + "/api/getTaskActions/" +
                    entry.getKey() + "\">操作详情(" + RedisUtils.taskActionNums(entry.getKey())  +")</a></td>");
            sb.append("</tr>");
        }
        LogTool.logInfo(2, sb.toString());
        String page =
                "<HTML>" +
                        "  <HEAD>" +
                        "    <TITLE>调度监控界面</TITLE>" +
                        "  </HEAD>" +
                        "  <BODY>" +
                        "    <H1>" + node + "节点任务列表</H1>" +
                        "      <table border=\"1\" cellspacing=\"0\" cellpadding=\"10\">" +
                        "      <tbody>" +
                        "        <tr>" +
                        "          <th>任务id</td>" +
                        "          <th>任务状态</td>" +
                        "          <th>操作详情</td>" +
                        "        </tr>" +
                        sb.toString() +
                        "      </tbody>" +
                        "      </table>" +
                        "  </BODY>" +
                        "</HTML>";
        context.html(page);
    }
    public static void getTaskActions(Context context){
        String taskId = context.pathParam("taskId");
        List<String> list = RedisUtils.taskActions(taskId);
        StringBuilder sb = new StringBuilder();
        for (String action : list){
            sb.append("<tr>");
            sb.append("<td>" + taskId + "</td>");
            sb.append("<td>" + action + "</td>");
            sb.append("</tr>");
        }
        LogTool.logInfo(1, sb.toString());
        String page =
                "<HTML>" +
                        "  <HEAD>" +
                        "    <TITLE>调度监控界面</TITLE>" +
                        "  </HEAD>" +
                        "  <BODY>" +
                        "    <H1>任务操作列表</H1>" +
                        "      <table border=\"1\" cellspacing=\"0\" cellpadding=\"10\">" +
                        "      <tbody>" +
                        "        <tr>" +
                        "          <th>任务id</td>" +
                        "          <th>操作详情</td>" +
                        "        </tr>" +
                        sb.toString() +
                        "      </tbody>" +
                        "      </table>" +
                        "  </BODY>" +
                        "</HTML>";
        context.html(page);
    }
    public static void info(Context context) {

    }
    
}
