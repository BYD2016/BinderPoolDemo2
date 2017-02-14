package com.byd.binderpool.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.byd.binderpool.BinderPool;
import com.byd.binderpool.ICompute;
import com.byd.binderpool.ISecurityCenter;
import com.byd.binderpool.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MSG_CODE_ENCRYPT_DECRYPT_MSG = 0;
    private static final int MSG_CODE_ADD = 1;

    private ISecurityCenter mISecurityCenter;
    private ICompute mICompute;

    private TextView mTvEncryptMsg; // 加密数据的显示
    private TextView mTvAddMsg; // 累计数据的显示

    // 周用后台服务方法
    private ExecutorService mRpcExecutorService;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CODE_ENCRYPT_DECRYPT_MSG:
                    mTvEncryptMsg.setText((String) msg.obj);
                    break;

                case MSG_CODE_ADD:
                    mTvAddMsg.setText((String) msg.obj);
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTvEncryptMsg = (TextView) findViewById(R.id.main_tv_encrypt_msg);
        mTvAddMsg = (TextView) findViewById(R.id.main_tv_add_msg);

        mRpcExecutorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * 加密解密的点击回调
     *
     * @param view 界面
     */
    public void encryptMsg(View view) {
        mRpcExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                doEncrypt();
            }
        });
    }

    /**
     * 调用加密服务
     */
    private void doEncrypt() {
        final BinderPool binderPool = BinderPool.getInstance(this);

        synchronized (this) {
            if (mISecurityCenter == null) {
                IBinder securityBinder = binderPool.queryBinder(BinderPool.BINDER_CODE_SECURITY_CENTER);
                mISecurityCenter = ISecurityCenter.Stub.asInterface(securityBinder);

                try {
                    securityBinder.linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            mISecurityCenter = null;

                            // 重连
                            IBinder securityBinder = binderPool.queryBinder(
                                    BinderPool.BINDER_CODE_SECURITY_CENTER);
                            mISecurityCenter = ISecurityCenter.Stub.asInterface(securityBinder);

                        }
                    }, 0);

                } catch (RemoteException e) {
                    Log.e(TAG, "reconnect to BINDER_CODE_SECURITY_CENTER", e);
                }
            }
        }

        String msg = "Hello, I am Spike!";
        try {
            String encryptMsg = mISecurityCenter.encrypt(msg);
            Log.e(TAG, "加密信息: " + encryptMsg);

            String decryptMsg = mISecurityCenter.decrypt(encryptMsg);
            Log.e(TAG, "解密信息: " + decryptMsg);

            mHandler.obtainMessage(MSG_CODE_ENCRYPT_DECRYPT_MSG, encryptMsg + "\n" + decryptMsg)
                    .sendToTarget();

        } catch (RemoteException e) {
            Log.e(TAG, "call in ISecurityCenter fail.", e);
        }
    }

    /**
     * 加法的点击回调
     *
     * @param view 视图
     */
    void addNumbers(View view) {
        mRpcExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                doAddition();
            }
        });
    }

    /**
     * 调用加法服务
     */
    private void doAddition() {
        synchronized (this) {
            if (mICompute == null) {
                BinderPool binderPool = BinderPool.getInstance(getApplicationContext());
                IBinder computeBinder = binderPool.queryBinder(BinderPool.BINDER_CODE_COMPUTE);
                mICompute = ICompute.Stub.asInterface(computeBinder);
            }
        }

        try {
            int result = mICompute.add(12, 12);
            Log.e(TAG, "12 + 12 = " + result);
            mHandler.obtainMessage(MSG_CODE_ADD, result + "")
                    .sendToTarget();
        } catch (RemoteException e) {
            Log.e(TAG, "call in ICompute fail.", e);
        }
    }
}
