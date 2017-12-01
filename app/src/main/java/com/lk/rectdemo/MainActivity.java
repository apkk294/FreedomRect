package com.lk.rectdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.lk.freedomrect.FreedomRectView;

public class MainActivity extends AppCompatActivity {

    private Button          mBtnGetLocation;
    private FreedomRectView mRectView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();
    }

    private void initViews() {
        mBtnGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnGetLocation.setText(mRectView.getPoints().toString());
            }
        });
    }

    private void findViews() {
        mBtnGetLocation = findViewById(R.id.btn);
        mRectView = findViewById(R.id.rectView);
    }
}
