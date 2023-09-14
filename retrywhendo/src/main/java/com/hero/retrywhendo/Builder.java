package com.hero.retrywhendo;

import androidx.lifecycle.LifecycleOwner;

import com.hero.retrywhendo.interfaces.CallBack;
import com.hero.retrywhendo.interfaces.OnDoOperationListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 配置
 */
public class Builder<T> {

    /**
     * 是否调试
     */
    private boolean isDebug;

    /**
     * 操作所携带的参数
     */
    private T t;

    /**
     * 重试结束，最终的回调
     */
    private CallBack finalCallBack;

    /**
     * 操作暴露的接口
     */
    private OnDoOperationListener onDoOperationListener;

    /**
     * 重试列表，即每次重试相隔的时间  默认3秒重试一次
     */
    private List<Integer> delayTimeList = Arrays.asList(3);

    /**
     * 单位
     */
    private TimeUnit unit = TimeUnit.SECONDS;

    /**
     * 执行线程 默认io线程
     */
    private Scheduler subscribeOnScheduler = Schedulers.io();

    /**
     * 回调线程  默认主线程
     */
    private Scheduler observeOnScheduler = AndroidSchedulers.mainThread();

    /**
     * 使用AutoDispose 防止内存泄漏
     */
    private LifecycleOwner owner;

    public TimeUnit getUnit() {
        return unit;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public T getT() {
        return t;
    }

    public CallBack getFinalCallBack() {
        return finalCallBack;
    }

    public OnDoOperationListener getOnDoOperationListener() {
        return onDoOperationListener;
    }

    public List<Integer> getDelayTimeList() {
        return delayTimeList;
    }

    public Scheduler getSubscribeOnScheduler() {
        return subscribeOnScheduler;
    }

    public Scheduler getObserveOnScheduler() {
        return observeOnScheduler;
    }

    public LifecycleOwner getOwner() {
        return owner;
    }

    public Builder setIsDebug(boolean isDebug) {
        this.isDebug = isDebug;
        return this;
    }

    public Builder setSubscribeOnScheduler(Scheduler subscribeOnScheduler) {
        if (subscribeOnScheduler != null) {
            this.subscribeOnScheduler = subscribeOnScheduler;
        }
        return this;
    }

    public Builder setObserveOnScheduler(Scheduler observeOnScheduler) {
        if (observeOnScheduler != null) {
            this.observeOnScheduler = observeOnScheduler;
        }
        return this;
    }

    public Builder setOwner(LifecycleOwner owner) {
        this.owner = owner;
        return this;
    }

    /**
     * 传入参数
     *
     * @param t
     * @return
     */
    public Builder setParam(T t) {
        this.t = t;
        return this;
    }

    public Builder setDelayTimeList(List<Integer> delayTimeList) {
        if (delayTimeList != null && !delayTimeList.isEmpty()) {
            this.delayTimeList = delayTimeList;
        }
        return this;
    }

    public Builder setUnit(TimeUnit unit) {
        this.unit = unit;
        return this;
    }

    public Builder setFinalCallBack(CallBack finalCallBack) {
        this.finalCallBack = finalCallBack;
        return this;
    }

    public Builder setOnDoOperationListener(OnDoOperationListener onDoOperationListener) {
        this.onDoOperationListener = onDoOperationListener;
        return this;
    }

    public RetryWhenDoOperationHelper build() {
        return new RetryWhenDoOperationHelper(this);
    }

    @Override
    public String toString() {
        return "Builder{" +
                "t=" + JsonUtils.javabeanToJson(t) +
                ", finalCallBack=" + finalCallBack +
                ", onDoOperationListener=" + onDoOperationListener +
                ", delayTimeList=" + delayTimeList.toString() +
                ", subscribeOnScheduler=" + subscribeOnScheduler +
                ", observeOnScheduler=" + observeOnScheduler +
                ", owner=" + owner +
                '}';
    }
}
