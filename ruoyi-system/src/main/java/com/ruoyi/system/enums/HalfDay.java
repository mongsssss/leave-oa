package com.ruoyi.system.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 请假的 上午/下午 枚举
 */
public enum HalfDay {
    AM("AM", "上午"),
    PM("PM", "下午");

    private final String code;
    private final String desc;

    private HalfDay(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    // 根据传进来的字符串获取对应枚举，没有就报错
    public static HalfDay ofCode(String code) {
        for(HalfDay halfDay : values()) {
            if(halfDay.code.equals(code)) {
                return halfDay;
            }
        }
        throw new ServiceException("上午/下午标识不合法：" + code);
    }
}
