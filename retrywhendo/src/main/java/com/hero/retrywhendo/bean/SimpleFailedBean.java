package com.hero.retrywhendo.bean;


/**
 * 实现一个简单的失败数据
 */
public class SimpleFailedBean {
    private String codeStr;
    private String msgStr;

    public String getCodeStr() {
        return codeStr;
    }

    public void setCodeStr(String codeStr) {
        this.codeStr = codeStr;
    }

    public String getMsgStr() {
        return msgStr;
    }

    public void setMsgStr(String msgStr) {
        this.msgStr = msgStr;
    }
}