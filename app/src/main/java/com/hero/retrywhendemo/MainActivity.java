package com.hero.retrywhendemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hero.retrywhendo.JsonUtils;
import com.hero.retrywhendo.RetryWhenDoOperationHelper;
import com.hero.retrywhendo.bean.SimpleFailedBean;
import com.hero.retrywhendo.interfaces.CallBack;
import com.hero.retrywhendo.interfaces.OnDoOperationListener;

import java.util.Arrays;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static int count;

    /**
     * 操作的回调 其中失败成功的数据结构是个泛型，可以自己定义
     */
    private CallBack<SimpleFailedBean, String> callBack = new CallBack<SimpleFailedBean, String>() {
        @Override
        public void onFailed(SimpleFailedBean failedBean) {
            Log.i(TAG, "final onFailed " + Thread.currentThread() + "  simpleFailedBean:" + JsonUtils.javabeanToJson(failedBean));
        }

        @Override
        public void onSuccess(String successBean) {
            Log.i(TAG, "final onSuccess: " + Thread.currentThread() + "  " + successBean);
        }
    };

    /**
     * 实现操作暴露的接口  即需要重试的操作
     */
    private OnDoOperationListener onDoOperationListener = new OnDoOperationListener<String, SimpleFailedBean, String>() {
        /**
         * 进行操作
         *
         * @param str 操作所传入的参数
         * @param callBack 每次操作的回调  注意与上面最终的回调区分 ；其中回调的失败、成功的数据类型可以自定义 F, S
         */
        @Override
        public void onDoOperation(String str, CallBack<SimpleFailedBean, String> callBack) {
            count++;
            Log.i(TAG, "doAsyncOperation: " + Thread.currentThread() + " 处理参数为： " + str + " count:" + count);
                     /*   if (count > 5) {
                            callBack.onSuccess("成功");
                            return;
                        }*/
            //此处模拟一个异步操作
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SimpleFailedBean simpleFailedBean = new SimpleFailedBean();
                    simpleFailedBean.setCodeStr("111");
                    simpleFailedBean.setMsgStr("传入参数为：" + str + "，但是处理失败了！");
                    callBack.onFailed(simpleFailedBean);
                }
            }).start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRetryWhenOperation();
            }
        });
    }

    public void doRetryWhenOperation() {
        count = 0;
        Disposable disposable = RetryWhenDoOperationHelper.getInstance()
                //重试列表，即每次重试相隔的时间  默认3秒重试一次
                .setDelayTimeList(Arrays.asList(3, 2, 3, 1, 2, 4))
                // 执行线程 默认io线程
                .setSubscribeOnScheduler(Schedulers.io())
                //回调线程  默认主线程
                .setObserveOnScheduler(AndroidSchedulers.mainThread())
                //使用AutoDispose 防止内存泄漏
                .setOwner(this)
                //操作所传入的参数
                .setParam("这是请求参数")
                //重试结束，最终的回调
                //注意此处使用弱引用 所以不要以局部变量作为参数，否则很快被回收
                .setFinalCallBack(callBack)
                //操作暴露的接口  注意此处使用弱引用 所以不要以局部变量作为参数，否则很快被回收
                .setOnDoOperationListener(onDoOperationListener)
                //构建对象
                .build()
                //执行操作
                .doRetryWhenOperation();
    }
}