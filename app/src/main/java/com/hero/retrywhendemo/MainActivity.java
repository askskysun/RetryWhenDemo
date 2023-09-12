package com.hero.retrywhendemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FirstActivity.class));
            }
        });
    }
}