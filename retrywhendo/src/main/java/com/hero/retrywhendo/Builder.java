package com.hero.retrywhendo;

import androidx.lifecycle.LifecycleOwner;

import com.hero.retrywhendo.interfaces.CallBack;
import com.hero.retrywhendo.interfaces.OnDoOperationListener;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 配置
 */
public class Builder<T> {

    /**
     * 操作所携带的参数
     */
    private T t;

    /**
     * 重试结束，最终的回调
     * 注意此处使用弱引用 所以不要以局部变量作为参数，否则很快被回收
     */
    private WeakReference<CallBack> finalCallBackWeakRef;

    /**
     * 操作暴露的接口
     * 注意此处使用弱引用 所以不要以局部变量作为参数，否则很快被回收
     */
    private WeakReference<OnDoOperationListener> onDoOperationListenerWeakRef;

    /**
     * 重试列表，即每次重试相隔的时间  默认3秒重试一次
     */
    private List<Integer> delayTimeList = Arrays.asList(3);

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

    public T getT() {
        return t;
    }

    public WeakReference<CallBack> getFinalCallBackWeakRef() {
        return finalCallBackWeakRef;
    }

    public WeakReference<OnDoOperationListener> getOnDoOperationListenerWeakRef() {
        return onDoOperationListenerWeakRef;
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

    public Builder setFinalCallBack(CallBack finalCallBack) {
        if (finalCallBack != null) {
            finalCallBackWeakRef = new WeakReference<>(finalCallBack);
        }
        return this;
    }

    public Builder setOnDoOperationListener(OnDoOperationListener onDoOperationListener) {
        if (onDoOperationListener != null) {
            onDoOperationListenerWeakRef = new WeakReference<>(onDoOperationListener);
        }
        return this;
    }

    public RetryWhenDoOperationHelper build() {
        return new RetryWhenDoOperationHelper(this);
    }

    @Override
    public String toString() {
        return "Builder{" +
                "t=" + JsonUtils.javabeanToJson(t) +
                ", finalCallBackWeakRef=" + finalCallBackWeakRef +
                ", onDoOperationListenerWeakRef=" + onDoOperationListenerWeakRef +
                ", delayTimeList=" + delayTimeList.toString() +
                ", subscribeOnScheduler=" + subscribeOnScheduler +
                ", observeOnScheduler=" + observeOnScheduler +
                ", owner=" + owner +
                '}';
    }
}
