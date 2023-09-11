package com.hero.retrywhendo.bean;

/**
 * onNext 回调时传递的结果
 */
public class OnNextResultBean<R> {

    /**
     * 成功的
     */
    private boolean isSuccessed;
    private R r;

    public OnNextResultBean(boolean isSuccessed, R r) {
        this.isSuccessed = isSuccessed;
        this.r = r;
    }

    public R getR() {
        return r;
    }

    public boolean isSuccessed() {
        return isSuccessed;
    }
}