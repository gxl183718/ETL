package com.rcyxkj.etl.server;

import com.rcyxkj.etl.configs.TSMConf;

public class ActionFactory {

    public static ETLTaskAction getTaskAction() {

        if (TSMConf.isAsync) {
            return new AsyncETLTaskAction();
        } else {
            return new SyncETLTaskAction();
        }
        
    }
    
}
