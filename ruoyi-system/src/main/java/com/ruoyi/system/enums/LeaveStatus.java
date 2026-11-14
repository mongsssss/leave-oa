package com.ruoyi.system.enums;

import java.util.*;

public enum LeaveStatus {

    // TODO 以后打算把备注改成：待审批，一级审批通过，已通过，已驳回，已撤回。从而实现可以以后随便加角色权力，比如admin也可以加入一级审批和二级审批。
    // TODO 所以只能通过这几个定死的状态来写逻辑（这样的话我想把枚举名字code都改一下......）
    PENDING("PENDING","待经理审批","manager"),
    MANAGER_APPROVED("MANAGER_APPROVED","待总监审核","director"),
    APPROVED("APPROVED","已通过",null),
    REJECTED("REJECTED","已驳回",null),
    CANCELLED("CANCELLED","已撤回",null);

    private final String code;  // 状态码 -- 用于标识状态的唯一代码
    private final String desc;  // 状态描述 -- 状态的中文解释，用来给用户看的
    private final String approverRole;  // 审批人角色 -- 当前状态对应的审批人角色

    // 枚举类的构造方法，用于初始化状态码、状态描述和审批人角色
    LeaveStatus(String code, String desc, String approverRole){
        this.code = code;
        this.desc = desc;
        this.approverRole = approverRole;
    }

    public String getCode(){
        return code;
    }

    public String getDesc(){
        return desc;
    }

    public String getApproverRole(){
        return approverRole;
    }

    // 状态流转规则表（用Map实现），key为当前状态，value为可流转到的状态集合（因为是枚举类集合，所以用EnumSet，且set是不可重复的集合），
    private static final Map<LeaveStatus, Set<LeaveStatus>> TRANSITIONS = new HashMap<>();

    // 静态块，初始化状态流转规则表，用static是因为在类加载时就初始化，且只初始化一次，因为状态流转规则是固定的，是static的
    static{
        TRANSITIONS.put(PENDING, EnumSet.of(MANAGER_APPROVED, APPROVED, REJECTED, CANCELLED));
        TRANSITIONS.put(MANAGER_APPROVED, EnumSet.of(APPROVED, REJECTED, CANCELLED));
        TRANSITIONS.put(APPROVED, EnumSet.of(CANCELLED));
        TRANSITIONS.put(REJECTED, EnumSet.noneOf(LeaveStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(LeaveStatus.class));

        // 检查所有状态是否都配置了流转规则，避免后续加新的状态而忘记配置
        for(LeaveStatus s : values()){
            if(!TRANSITIONS.containsKey(s)){
                throw new IllegalStateException("状态" + s + "未配置流转规则");
            }
        }
    }

    // 用来接收前端传来的状态（String类），并把其传来的String类转为LeaveStatus枚举类
    // 用static是因为这个方法不依赖于对象的实例，其就是用来创造对象的
    public static LeaveStatus ofCode(String code){
        for(LeaveStatus s : values()){
            //"常量在前、变量在后"（known.equals(unknown)）。
            if(s.code.equals(code)){
                return s;
            }
        }
        // IllegalArgumentException 是运行时异常，表示传入的参数有误
        throw new IllegalArgumentException("未知状态码: " + code);
    }

    // 判断当前状态是否可以流转到目标状态（从当前状态是否可以进到target状态）
    public boolean canTransitTo(LeaveStatus target){
        return TRANSITIONS.get(this).contains(target);
    }
    // 判断当前状态是否是结束状态
    public boolean isFinal(){
        return TRANSITIONS.get(this).isEmpty();
    }

}
