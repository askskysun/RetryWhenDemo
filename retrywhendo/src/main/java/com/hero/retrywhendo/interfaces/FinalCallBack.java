package com.hero.retrywhendo.interfaces;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * 重试过后最终的回调
 *
 * @param <F> 失败的回调数据
 * @param <S> 成功的回调数据
 */
public interface FinalCallBack<F, S> {
    void onFailed(F failedBean);

    void onSuccess(S successBean);

    void onError(@NonNull Throwable e);
}