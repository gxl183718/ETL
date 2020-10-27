package com.rcyxkj.etl.entity;

import lombok.Data;

@Data
public class TaskEntity {
    private String m_id = "";//消息id
    private String task_id = "";
    private String app_id = "";
    private String tenant_id = "";
    private String task_name = "";
    private int state;  //0:新增；1:修改；2:删除
}
