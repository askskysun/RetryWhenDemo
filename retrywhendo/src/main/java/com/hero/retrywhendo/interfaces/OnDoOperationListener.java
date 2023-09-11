package com.hero.retrywhendo.interfaces;

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