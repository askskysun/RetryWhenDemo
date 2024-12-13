package com.hero.retrywhendo;

import android.os.Looper;
import android.util.Log;

import com.hero.retrywhendo.interfaces.FinalCallBack;
import com.hero.retrywhendo.interfaces.OperationCallBack;
import com.hero.retrywhendo.interfaces.OnDoOperationListener;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import autodispose2.AutoDispose;
import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;

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
    private int retryCount;
    private Builder<T> builder = new Builder<>();
    private Disposable disposable;

    /**
     * 是否已经停止
     */
    private boolean isStopNow;

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

    public Disposable doRetryWhenOperation() {
        retryCount = 0;
        isStopNow = false;

        Observable<OnNextBean> objectObservable = Observable.create((ObservableEmitter<OnNextBean> emitter) -> {
                    //在最后一次重试时 emitter.isDisposed() = true，无法使用 onNext 和 onError传递
                    try {
                        doOperation(emitter);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        if (builder.isDebug()) {
                            Log.e(TAG, "doRetryWhenOperation() doOperation()", exception);
                        }
                        if (!isDisposed(emitter)) {
                            emitter.onError(exception);
                            //结束
                            emitter.onComplete();
                        } else {
                            //emitter.isDisposed() = false 证明已经是最后一次
                            onFinalError(exception);
                        }
                    }
                }).retryWhen(errorObservable -> errorObservable
                        .zipWith(builder.getDelayTimeList(), (e, time) -> time)
                        //concatMap与flatMap唯一不同的是concat能保证Observer接收到Observable集合发送事件的顺序
                        .concatMap(delay -> {
                            if (builder.isDebug()) {
                                Log.i(TAG, delay + "秒后重试");
                            }
                            return Observable.timer(delay, builder.getUnit());
                        }))
                .subscribeOn(builder.getSubscribeOnScheduler())
                //子线程中处理好的数据在主线程中返回
                .observeOn(builder.getObserveOnScheduler());
        //延迟处理
        Observable<OnNextBean> booleanObservable = Observable.timer(builder.getDelay(), TimeUnit.SECONDS)
                .concatMap((Function<Long, ObservableSource<OnNextBean>>) aLong -> {
                    Log.i(TAG, String.format("延迟%d秒执行", builder.getDelay()));
                    return objectObservable;
                });

        Observer<OnNextBean> observer = getObserver();
        //使用AutoDispose 防止内存泄漏
        if (builder.getOwner() != null) {
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                booleanObservable.to(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(builder.getOwner())))
                        .subscribe(observer);
                return disposable;
            }
            //使用rxjava切换到主线程
            Observable.just(0)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(integer -> {
                        booleanObservable.to(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(builder.getOwner())))
                                .subscribe(observer);
                    });
            return disposable;
        }
        booleanObservable.subscribe(observer);
        return disposable;
    }

    private Observer<OnNextBean> getObserver() {
        Observer<OnNextBean> observer = new Observer<OnNextBean>() {
            @Override
            public void onSubscribe(@NotNull Disposable disposable) {
                RetryWhenDoOperationHelper.this.disposable = disposable;
                if (builder.isDebug()) {
                    Log.i(TAG, "Disposable..." + Thread.currentThread());
                }
            }

            @Override
            public void onNext(@NotNull OnNextBean onNextBean) {
                if (builder.isDebug()) {
                    Log.i(TAG, "onNext..." + Thread.currentThread() + " onNextBean: " + JsonUtils.javabeanToJson(onNextBean));
                }
            }

            @Override
            public void onError(@NotNull Throwable e) {
                if (builder.isDebug()) {
                    Log.e(TAG, "onError... 这里只有最后一次重试中异常才会到，其他被拦截而重试了 " + Thread.currentThread(), e);
                }
                onFinalError(e);
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
    private void doOperation(ObservableEmitter<OnNextBean> emitter) {
        Log.i(TAG, "开始执行操作: 在最后一次重试时 emitter.isDisposed() = true，无法使用 onNext 和 onError传递");
        //进行操作（同步、异步都使用回调结果处理）
        //传入操作后回调处理
        OnDoOperationListener onDoOperationListener = builder.getOnDoOperationListener();
        if (onDoOperationListener == null) {
            return;
        }

        onDoOperationListener.onDoOperation(builder.getT(), new OperationCallBack<F, S>() {
            @Override
            public void onFailed(F failedBean) {
                if (builder.isDebug()) {
                    Log.i(TAG, "onFailed failedBean:"
                            + JsonUtils.javabeanToJson(failedBean) + " Thread:" + Thread.currentThread());
                }
                onDoOperationFaile(failedBean, emitter);
            }

            /**
             * 回调成功，则直接结束重试，并回调结果
             * @param successBean
             */
            @Override
            public void onSuccess(S successBean) {
                if (builder.isDebug()) {
                    Log.i(TAG, "onSuccess successBean:"
                            + JsonUtils.javabeanToJson(successBean) + " Thread:" + Thread.currentThread());
                }

                onFinalSuccess(successBean);
                if (!isDisposed(emitter)) {
                    OnNextBean onNextBean = new OnNextBean();
                    onNextBean.setS(successBean);
                    emitter.onNext(onNextBean);
                    //结束
                    emitter.onComplete();
                }
            }
        });
    }

    private void onDoOperationFaile(F failedBean, ObservableEmitter<OnNextBean> emitter) {
        retryCount++;
        boolean isDisposed = isDisposed(emitter);
        if (retryCount > builder.getDelayTimeList().size()) {
            //重试执行完毕 emitter 已经关闭
            if (builder.isDebug()) {
                Log.i(TAG, "重试执行完毕 emitter 已经关闭");
            }
            onFinalFailed(failedBean);
            //重试列表已经都重试完了，最终回调错误信息
          /*  OnNextBean onNextBean = new OnNextBean();
            onNextBean.setF(failedBean);
            if (!isDisposed) {
                emitter.onNext(onNextBean);
                emitter.onComplete();
            }*/
            return;
        }
        if (!isDisposed) {
            if (builder.isDebug()) {
                Log.i(TAG, "onDoOperationFaile 重试次数未使用完，报个错，使之能进行重试");
            }
            //重试次数未使用完，报个错，使之能进行重试
            emitter.onError(new RuntimeException("处理失败"));
        }
    }

    private void onFinalFailed(F failedBean) {
        if (!isCanCallBack()) {
            return;
        }

        Scheduler observeOnScheduler = builder.getObserveOnScheduler();
        if (observeOnScheduler != null) {
            Observable.just(0)
                    .observeOn(observeOnScheduler)
                    .subscribe(integer -> {
                        if (isCanCallBack()) {
                            FinalCallBack finalOperationCallBack = builder.getFinalCallBack();
                            finalOperationCallBack.onFailed(failedBean);
                        }
                    });
            return;
        }
        FinalCallBack finalOperationCallBack = builder.getFinalCallBack();
        finalOperationCallBack.onFailed(failedBean);
    }


    private void onFinalSuccess(S successBean) {
        if (!isCanCallBack()) {
            return;
        }

        Scheduler observeOnScheduler = builder.getObserveOnScheduler();
        if (observeOnScheduler != null) {
            Observable.just(0)
                    .observeOn(observeOnScheduler)
                    .subscribe(integer -> {
                        if (isCanCallBack()) {
                            FinalCallBack finalOperationCallBack = builder.getFinalCallBack();
                            finalOperationCallBack.onSuccess(successBean);
                        }
                    });
            return;
        }
        FinalCallBack finalOperationCallBack = builder.getFinalCallBack();
        finalOperationCallBack.onSuccess(successBean);
    }

    private void onFinalError(Throwable e) {
        if (!isCanCallBack()) {
            return;
        }

        Scheduler observeOnScheduler = builder.getObserveOnScheduler();
        if (observeOnScheduler != null) {
            Observable.just(0)
                    .observeOn(observeOnScheduler)
                    .subscribe(integer -> {
                        if (isCanCallBack()) {
                            FinalCallBack finalOperationCallBack = builder.getFinalCallBack();
                            finalOperationCallBack.onError(e);
                        }
                    });
            return;
        }
        FinalCallBack finalOperationCallBack = builder.getFinalCallBack();
        finalOperationCallBack.onError(e);
    }

    private boolean isCanCallBack() {
        if (builder.getFinalCallBack() == null) {
            return false;
        }
        return !isStopNow;
    }

    public void stopNow() {
        if (disposable != null) {
            disposable.dispose();
        }

        isStopNow = true;
    }

    /**
     * 判断是否已经取消
     * io.reactivex.rxjava3.exceptions.UndeliverableException: The exception could not be delivered to the consumer because it has already canceled/disposed the flow or the exception has nowhere to go to begin with. Further reading: https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling | java.lang.RuntimeException: 处理失败
     *
     * @return
     */
    private boolean isDisposed(ObservableEmitter emitter) {
        if (disposable == null || emitter == null) {
            return true;
        }

        if (builder.isDebug()) {
            Log.i(TAG, "isDisposed:" + disposable.isDisposed()
                    + " emitter.isDisposed:" + emitter.isDisposed()
                    + " Thread:" + Thread.currentThread());
        }

        if (disposable.isDisposed() || emitter.isDisposed()) {
            return true;
        }

        return false;
    }

    private class OnNextBean {
        private S s;
        private F f;

        public S getS() {
            return s;
        }

        public void setS(S s) {
            this.s = s;
        }

        public F getF() {
            return f;
        }

        public void setF(F f) {
            this.f = f;
        }

        public boolean isSuccess() {
            return s != null;
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
