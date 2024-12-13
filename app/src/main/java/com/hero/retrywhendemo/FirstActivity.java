package com.hero.retrywhendemo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.hero.retrywhendo.JsonUtils;
import com.hero.retrywhendo.RetryWhenDoOperationHelper;
import com.hero.retrywhendo.bean.SimpleFailedBean;
import com.hero.retrywhendo.interfaces.FinalCallBack;
import com.hero.retrywhendo.interfaces.OperationCallBack;
import com.hero.retrywhendo.interfaces.OnDoOperationListener;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * <pre>
 *
 * </pre>
 */
public class FirstActivity extends AppCompatActivity {
    private static final String TAG = "FirstActivity";
    private static volatile int count;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRetryWhenOperation();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: disposable:" + (disposable == null ? "null" : disposable.isDisposed()));
    }

    public void doRetryWhenOperation() {
        count = 0;
        Log.i(TAG, "doRetryWhenOperation: disposable:" + (disposable == null ? "null" : disposable.isDisposed()));
        //判断是否正在进行
        if (disposable != null && !disposable.isDisposed()) {
            return;
        }

        disposable = RetryWhenDoOperationHelper.getInstance()
                //是否调试打印日志
                .setIsDebug(true)
                //延迟执行时间，默认为0，不延迟
                .setDelay(2)
                //此处参数意义为：第一次失败，3秒后重试；第二次失败，2秒后重试，第三次失败1秒后重试
                //重试列表，即每次重试相隔的时间  默认3秒重试一次
//                .setDelayTimeList(Arrays.asList(3, 2, 1, 2, 4, 1))
                .setDelayTimeList(Arrays.asList(1, 1))
                //单位
                .setUnit(TimeUnit.SECONDS)
                // 执行线程 默认io线程
                .setSubscribeOnScheduler(Schedulers.io())
                //回调线程  默认主线程
                .setObserveOnScheduler(AndroidSchedulers.mainThread())
                //使用AutoDispose 防止内存泄漏
                .setOwner(this)
                //操作所传入的参数
                .setParam("这是请求参数")
                //实现操作暴露的接口  即需要重试的操作
                //操作暴露的接口  注意此处使用弱引用 所以不要以局部变量作为参数，否则很快被回收
                .setOnDoOperationListener(new OnDoOperationListener<String, SimpleFailedBean, String>() {

                    /**
                     * 进行操作
                     *
                     * @param str 操作所传入的参数
                     * @param operationCallBack 每次操作的回调  注意与上面最终的回调区分 ；其中回调的失败、成功的数据类型可以自定义 F, S
                     */
                    @Override
                    public void onDoOperation(String str, OperationCallBack<SimpleFailedBean, String> operationCallBack) {
                        //此处进行操作，可异步，也可同步；最终CallBack回调结果即可
                        //此处模拟一个异步操作
                        count++;
                        Log.i(TAG, "doAsyncOperation: " + Thread.currentThread() + " 处理参数为： " + str + " count:" + count);


                        //测试过程出现异常
                       /* String strr = null;
                        boolean equals = strr.equals("");*/

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                /*if (count > 1) {
                                    operationCallBack.onSuccess("成功");
                                    return;
                                }
                                SimpleFailedBean simpleFailedBean = new SimpleFailedBean();
                                simpleFailedBean.setCodeStr("111");
                                simpleFailedBean.setMsgStr("传入参数为：" + str + "，但是处理失败了！");
                                operationCallBack.onFailed(simpleFailedBean);*/
                                operationCallBack.onSuccess("");
                            }
                        }).start();
                    }
                })
                // 操作的回调 其中失败成功的数据结构是个泛型，可以自己定义
                //重试结束，最终的回调
                .setFinalCallBack(new FinalCallBack<SimpleFailedBean, String>() {
                    @Override
                    public void onFailed(SimpleFailedBean failedBean) {
                        Log.i(TAG, "final onFailed " + Thread.currentThread() + "  simpleFailedBean:" + JsonUtils.javabeanToJson(failedBean));
                    }

                    @Override
                    public void onSuccess(String successBean) {
                        Log.i(TAG, "final onSuccess: " + Thread.currentThread() + "  " + successBean);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.i(TAG, "final onError: " + Thread.currentThread() + "  " + e.toString());
                    }
                })
                //构建对象
                .build()
                //执行操作
                .doRetryWhenOperation();
//停止操作  停止执行，停止回调
//        retryWhenDoOperationHelper.stopNow();
    }
}