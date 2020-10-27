package com.rcyxkj.etl.tool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class LoadScript {

    private static final String SCRIPT_DIR = "/scripts";

    public static String selectLeaader() {
        try {
            return readScriptToString("select_leader.lua");
        } catch (Exception e) {
            LogTool.logInfo(2, e.getMessage());
        }
        return "";
    }

    private static String readScriptToString(String file) throws IOException {
        InputStream stream = LoadScript.class.getResourceAsStream(SCRIPT_DIR + File.separator + file);
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }
    
}
