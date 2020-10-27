package com.rcyxkj.etl.tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.rcyxkj.etl.configs.TSMConf;

public class LogTool {
    public static String getTime(){
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()) + " ----> ";
    }

    public static void logInfo(int level, String info){
        if (level <= TSMConf.logLevel){
            System.out.println(getTime() + info);
        }
    }
}
