package com.hero.retrywhendo;

import android.util.Log;

import com.hero.retrywhendo.interfaces.CallBack;
import com.hero.retrywhendo.interfaces.OnDoOperationListener;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import autodispose2.AutoDispose;
import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * <pre>
 * 重试操作的工具类
 * @param <T> 操作所携带的参数，多参数则封装成bean
 * @param <F> 操作失败回调的参数，多参数则封装成bean
 * @param <S> 操作成功回调的参数，多参数则封装成bean
 * </pre>
 */
public class RetryWhenDoOperationHelper<T, F, S> {

    private final String TAG = "RetryWhenHelper";

    /**
     * 重试的次数
     */
    private AtomicInteger count = new AtomicInteger(0);
    private Builder<T> builder = new Builder<>();
    private Disposable disposable;

    /**
     * 是否已经停止
     */
    private AtomicBoolean isStopNow = new AtomicBoolean(false);

    public static Builder getInstance() {
        return new Builder();
    }

    RetryWhenDoOperationHelper(Builder<T> builder) {
        if (builder != null) {
            this.builder = builder;
        }
        if (builder.isDebug()) {
            Log.i(TAG, "配置builder：" + this.builder.toString());
        }
    }

    public RetryWhenDoOperationHelper doRetryWhenOperation() {
        count.set(0);
        isStopNow.set(false);
        Observable<Boolean> objectObservable = Observable.create((ObservableEmitter<Boolean> emitter) -> {
            doOperation(emitter);
        }).retryWhen((Observable<Throwable> errorObservable) -> errorObservable
                .zipWith(builder.getDelayTimeList(), (Throwable e, Integer time) -> time)
                .flatMap((Integer delay) -> {
                    if (builder.isDebug()) {
                        Log.i(TAG, delay + "秒后重试");
                    }
                    return Observable.timer(delay, TimeUnit.SECONDS);
                }))
                .subscribeOn(builder.getSubscribeOnScheduler())
                //子线程中处理好的数据在主线程中返回
                .observeOn(builder.getObserveOnScheduler());
        Observer<Boolean> observer = getObserver();
        //使用AutoDispose 防止内存泄漏
        if (builder.getOwner() != null) {
            objectObservable.to(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(builder.getOwner())))
                    .subscribe(observer);
            return this;
        }
        objectObservable.subscribe(observer);
        return this;
    }

    private Observer<Boolean> getObserver() {
        Observer<Boolean> observer = new Observer<Boolean>() {
            @Override
            public void onSubscribe(@NotNull Disposable disposable) {
                RetryWhenDoOperationHelper.this.disposable = disposable;
                if (builder.isDebug()) {
                    Log.i(TAG, "Disposable..." + Thread.currentThread());
                }
            }

            @Override
            public void onNext(@NotNull Boolean isSuccess) {
                if (builder.isDebug()) {
                    Log.i(TAG, "onNext..." + Thread.currentThread() + " isSuccess: " + isSuccess);
                }
            }

            @Override
            public void onError(@NotNull Throwable e) {
                if (builder.isDebug()) {
                    Log.e(TAG, "onError..." + Thread.currentThread(), e);
                }

                if (isCanCallBack()) {
                    CallBack finalCallBack = builder.getFinalCallBack();
                    finalCallBack.onFailed(e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                if (builder.isDebug()) {
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
    private void doOperation(ObservableEmitter<Boolean> emitter) {
        Log.i(TAG, "开始执行操作: ");
        //进行操作（同步、异步都使用回调结果处理）
        //传入操作后回调处理
        OnDoOperationListener onDoOperationListener = builder.getOnDoOperationListener();
        if (onDoOperationListener == null) {
            return;
        }

        onDoOperationListener.onDoOperation(builder.getT(), new CallBack<F, S>() {
            @Override
            public void onFailed(F failedBean) {
                if (emitter == null) {
                    return;
                }

                if (builder.isDebug()) {
                    //在最后一次时 emitter.isDisposed() = true，无法使用 onNext传递
                    Log.i(TAG, "emitter.isDisposed：" + emitter.isDisposed() + " disposable:" + (disposable == null ? "null" : disposable.isDisposed()));
                }
                count.set(count.get() + 1);
                if (count.get() > builder.getDelayTimeList().size()) {
                    //重试列表已经都重试完了，最终回调错误信息
                    if (isCanCallBack()) {
                        CallBack finalCallBack = builder.getFinalCallBack();
                        finalCallBack.onFailed(failedBean);
                    }
                    emitter.onNext(false);
                    emitter.onComplete();
                    return;
                }

                //重试次数未使用完，报个错，使之能进行重试
                emitter.onError(new RuntimeException("处理失败"));
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
                if (isCanCallBack()) {
                    CallBack finalCallBack = builder.getFinalCallBack();
                    finalCallBack.onSuccess(successBean);
                }
                emitter.onNext(true);
                //结束
                emitter.onComplete();
            }
        });
    }

    private boolean isCanCallBack() {
        if (builder.getFinalCallBack() == null) {
            return false;
        }
        return !isStopNow.get();
    }

    public void stopNow() {
        if (disposable != null) {
            disposable.dispose();
        }

        isStopNow.set(true);
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
