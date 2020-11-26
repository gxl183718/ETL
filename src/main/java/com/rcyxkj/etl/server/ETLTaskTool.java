package com.rcyxkj.etl.server;

import com.alibaba.fastjson.JSON;
import com.rcyxkj.etl.configs.TSMConf;
import com.rcyxkj.etl.entity.TaskEntity;
import com.rcyxkj.etl.error.ScheduleException;
import com.rcyxkj.etl.tool.LogTool;
import com.rcyxkj.etl.tool.RedisUtils;

import java.util.List;

public class ETLTaskTool {

    class TaskAction {
        public static final int ADD = 0;
        public static final int UPDATE = 1;
        public static final int DELETE = 2;
    }

    public static void handle(TaskEntity task) throws ScheduleException {
        ETLTaskAction action = ActionFactory.getTaskAction();
        switch (task.getState()) {
            case TaskAction.ADD:
                LogTool.logInfo(2, task.getM_id() + " status is add");
                action.doAdd(task);
                break;
            case TaskAction.DELETE:
                LogTool.logInfo(2, task.getM_id() + " status is delete");
                action.doDelete(task);
                break;
            case TaskAction.UPDATE:
                LogTool.logInfo(2, task.getM_id() + " status is update");
                action.doUpdate(task);
                break;
            default:
                LogTool.logInfo(TSMConf.logLevel, "Not add/delete/update action,program will do nothing.");
        }
    }

	public static void taskStore(TaskEntity taskEntity) {
        RedisUtils.taskStore(taskEntity);
	}

}
