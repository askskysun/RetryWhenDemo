package com.hero.retrywhendo.interfaces;

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