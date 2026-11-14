package com.ruoyi.system.enums;

public enum ApprovalAction {
    APPROVE("APPROVE","通过"),
    REJECT("REJECT","驳回"),
    CANCEL("CANCEL","撤回");

    private final String code;
    private final String desc;

    ApprovalAction(String code,String desc){
        this.code = code;
        this.desc = desc;
    }

    public String getCode(){
        return this.code;
    }

    public String getDesc(){
        return this.desc;
    }

}
