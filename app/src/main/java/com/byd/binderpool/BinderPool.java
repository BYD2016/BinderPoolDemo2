package com.byd.binderpool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.CountDownLatch;

/**
 * 连接池实现
 * <p/>
 * Created by wangchenlong on 16/6/17.
 */
public final class BinderPool {

    private static final String TAG = BinderPool.class.getSimpleName();

    public static final int BINDER_CODE_COMPUTE = 0;
    public static final int BINDER_CODE_SECURITY_CENTER = 1;


    private IBinderPool mBinderPool;

    // 编译器每次都需要从主存中读取
    private static volatile BinderPool sInstance;

    private Context mAppContext;

    private CountDownLatch mBinderPoolCountDownLatch;

    private BinderPool(Context context) {
        mAppContext = context.getApplicationContext();
        connectBinderPoolService();
    }

    // 单例
    public static BinderPool getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BinderPool.class) {
                if (sInstance == null) {
                    sInstance = new BinderPool(context);
                }
            }
        }
        return sInstance;
    }

    private synchronized void connectBinderPoolService() {
        mBinderPoolCountDownLatch = new CountDownLatch(1);
        Intent serviceIntent = new Intent(mAppContext, BinderPoolService.class);
        mAppContext.bindService(serviceIntent, mBinderPoolConnection, Context.BIND_AUTO_CREATE);
        try {
            mBinderPoolCountDownLatch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "连接到BinderPoolService。", e);
        }
    }

    // 失效重联机制, 当Binder死亡时, 重新连接
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override public void binderDied() {
            Log.e(TAG, "Binder失效");
            mBinderPool.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mBinderPool = null;

            // 重新连接
            connectBinderPoolService();
        }
    };

    // Binder的服务连接
    private ServiceConnection mBinderPoolConnection = new ServiceConnection() {

        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            mBinderPool = IBinderPool.Stub.asInterface(service);
            try {
                mBinderPool.asBinder().linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "call IBinderPool#linkToDeath fail", e);
            }
            mBinderPoolCountDownLatch.countDown();
        }

        @Override public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * 查询Binder
     *
     * @param binderCode binder代码
     * @return Binder
     */
    public IBinder queryBinder(int binderCode) {
        IBinder binder = null;
        try {
            if (mBinderPool != null) {
                binder = mBinderPool.queryBinder(binderCode);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "call IBinderPool#queryBinder fail", e);
        }

        return binder;
    }

    /**
     * Binder池实现
     */
    static final class BinderPoolImpl extends IBinderPool.Stub {
        BinderPoolImpl() {
            super();
        }

        @Override
        public IBinder queryBinder(int binderCode) throws RemoteException {
            IBinder binder = null;
            switch (binderCode) {
                case BINDER_CODE_COMPUTE:
                    binder = new ComputeImpl();
                    break;

                case BINDER_CODE_SECURITY_CENTER:
                    binder = new SecurityCenterImpl();
                    break;

                default:
                    break;
            }
            return binder;
        }
    }

}
