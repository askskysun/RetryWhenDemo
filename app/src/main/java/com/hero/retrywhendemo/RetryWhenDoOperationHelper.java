package com.hero.retrywhendemo;

import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import autodispose2.AutoDispose;
import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * <pre>
 * 重试操作的工具类
 * @param <T> 操作所携带的参数，多参数则封装成bean
 * @param <F> 操作失败回调的参数，多参数则封装成bean
 * @param <S> 操作成功回调的参数，多参数则封装成bean
 * </pre>
 * Author by sun, Email 1910713921@qq.com, Date on 2023/7/21.
 */
public class RetryWhenDoOperationHelper<T, F, S> {

    private final String TAG = "RetryWhenHelper";

    /**
     * 是否调试
     */
    private boolean ISDEBUG = true;

    /**
     * 重试的次数
     */
    private AtomicInteger count = new AtomicInteger(0);
    private Builder<T> builder = new Builder<>();

    public static Builder getInstance() {
        return new Builder();
    }

    private RetryWhenDoOperationHelper(Builder<T> builder) {
        if (builder != null) {
            this.builder = builder;
        }
        if (ISDEBUG) {
            Log.i(TAG, "配置builder：" + this.builder.toString());
        }
    }

    public void doRetryWhenOperation() {
        count.set(0);
        Observable<OnNextResultBean> objectObservable = Observable.create((ObservableEmitter<OnNextResultBean> emitter) -> {
            doOperation(emitter);
        }).retryWhen((Observable<Throwable> errorObservable) -> errorObservable
                .zipWith(builder.delayTimeList, (Throwable e, Integer time) -> time)
                .flatMap((Integer delay) -> {
                    if (ISDEBUG) {
                        Log.i(TAG, delay + "秒后重试");
                    }
                    return Observable.timer(delay, TimeUnit.SECONDS);
                }))
                .subscribeOn(builder.subscribeOnScheduler)
                //子线程中处理好的数据在主线程中返回
                .observeOn(builder.observeOnScheduler);
        Observer<OnNextResultBean> observer = getObserver();
        //使用AutoDispose 防止内存泄漏
        if (builder.owner != null) {
            objectObservable.to(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(builder.owner)))
                    .subscribe(observer);
            return;
        }
        objectObservable.subscribe(observer);
    }

    private Observer<OnNextResultBean> getObserver() {
        Observer<OnNextResultBean> observer = new Observer<OnNextResultBean>() {
            @Override
            public void onSubscribe(@NotNull Disposable d) {
                if (ISDEBUG) {
                    Log.i(TAG, "Disposable..." + Thread.currentThread());
                }
            }

            @Override
            public void onNext(@NotNull OnNextResultBean onNextResultBean) {
                if (ISDEBUG) {
                    Log.i(TAG, "onNext..." + Thread.currentThread() + " onNextResultBean: " + javabeanToJson(onNextResultBean));
                }
                if (builder.finalCallBackWeakRef == null) {
                    return;
                }
                CallBack finalCallBack = builder.finalCallBackWeakRef.get();
                //注意要与对应的类型一致，否则异常中断
                Object r = onNextResultBean.getR();
                if (onNextResultBean.isSuccessed()) {
                    if (finalCallBack != null) {
                        finalCallBack.onSuccess(r);
                    }
                    return;
                }
                if (finalCallBack != null) {
                    finalCallBack.onFailed(r);
                }
            }

            @Override
            public void onError(@NotNull Throwable e) {
                if (ISDEBUG) {
                    Log.e(TAG, "onError..." + Thread.currentThread(), e);
                }
            }

            @Override
            public void onComplete() {
                if (ISDEBUG) {
                    Log.i(TAG, "onComplete..." + Thread.currentThread());
                }
            }
        };
        return observer;
    }

    /**
     * 执行操作
     *
     * @param emitter
     */
    private void doOperation(ObservableEmitter<OnNextResultBean> emitter) {
        Log.i(TAG, "开始执行操作: ");
        //进行操作（同步、异步都使用回调结果处理）
        //传入操作后回调处理
        if (builder.onDoOperationListenerWeakRef == null) {
            return;
        }
        OnDoOperationListener onDoOperationListener = builder.onDoOperationListenerWeakRef.get();
        if (onDoOperationListener == null) {
            return;
        }

        onDoOperationListener.onDoOperation(builder.t, new CallBack<F, S>() {
            @Override
            public void onFailed(F failedBean) {
                if (emitter == null) {
                    return;
                }

                count.set(count.get() + 1);
                if (count.get() > builder.delayTimeList.size()) {
                    //重试列表已经都重试完了，最终回调错误信息
                    OnNextResultBean<F> onNextResultBean = new OnNextResultBean<>(false, failedBean);
                    emitter.onNext(onNextResultBean);
                    emitter.onComplete();
                    return;
                }

                //重试次数未使用完，报个错，使之能进行重试
                emitter.onError(new RuntimeException("处理失败"));
                emitter.onComplete();
            }

            /**
             * 回调成功，则直接结束重试，并回调结果
             * @param successBean
             */
            @Override
            public void onSuccess(S successBean) {
                if (emitter == null) {
                    return;
                }
                //回调最终结果
                OnNextResultBean<S> onNextResultBean = new OnNextResultBean<>(true, successBean);
                emitter.onNext(onNextResultBean);
                //结束
                emitter.onComplete();
            }
        });
    }

    /**
     * 配置
     */
    public static class Builder<T> {

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
                    "t=" + javabeanToJson(t) +
                    ", finalCallBackWeakRef=" + finalCallBackWeakRef +
                    ", onDoOperationListenerWeakRef=" + onDoOperationListenerWeakRef +
                    ", delayTimeList=" + delayTimeList.toString() +
                    ", subscribeOnScheduler=" + subscribeOnScheduler +
                    ", observeOnScheduler=" + observeOnScheduler +
                    ", owner=" + owner +
                    '}';
        }
    }

    /**
     * 操作暴露的接口
     *
     * @param <T> 传入参数
     * @param <F> 失败回调数据结构
     * @param <S> 成功回调数据结构
     */
    public interface OnDoOperationListener<T, F, S> {

        /**
         * 进行操作
         *
         * @param t        操作所传入的参数
         * @param callBack 每次操作的回调  注意与上面最终的回调区分 ；其中回调的失败、成功的数据类型可以自定义 F, S
         */
        void onDoOperation(T t, CallBack<F, S> callBack);
    }

    /**
     * 操作的回调
     *
     * @param <F> 失败的回调数据
     * @param <S> 成功的回调数据
     */
    public interface CallBack<F, S> {
        void onFailed(F failedBean);

        void onSuccess(S successBean);
    }

    /**
     * 实现一个简单的失败数据
     */
    public static class SimpleFailedBean {
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

    /**
     * onNext 回调时传递的结果
     */
    public static class OnNextResultBean<R> {

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

    public static String javabeanToJson(Object obj) {
        try {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String json = gson.toJson(obj);
            return json != null ? json : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
/**
 * Disposable...Thread[main,5,main]
 * 开始请求接口:
 * doAsyncOperation: Thread[RxCachedThreadScheduler-3,5,main]  参数 count:1
 * 3秒后重试
 * 开始请求接口:
 * doAsyncOperation: Thread[RxComputationThreadPool-7,5,main]  参数 count:2
 * 2秒后重试
 * 开始请求接口:
 * doAsyncOperation: Thread[RxComputationThreadPool-1,5,main]  参数 count:3
 * 3秒后重试
 * 开始请求接口:
 * doAsyncOperation: Thread[RxComputationThreadPool-2,5,main]  参数 count:4
 * 1秒后重试
 * 开始请求接口:
 * doAsyncOperation: Thread[RxComputationThreadPool-3,5,main]  参数 count:5
 * 2秒后重试
 * 开始请求接口:
 * doAsyncOperation: Thread[RxComputationThreadPool-4,5,main]  参数 count:6
 * 4秒后重试
 * 开始请求接口:
 * doAsyncOperation: Thread[RxComputationThreadPool-8,5,main]  参数 count:7
 * onNext...Thread[main,5,main] onNextResultBean: {"isSuccessed":false,"r":{"codeStr":"111","msgStr":"错了"}}
 * final onFailed Thread[main,5,main]  simpleFailedBean:{"codeStr":"111","msgStr":"错了"}
 * onComplete...Thread[main,5,main]
 **/
