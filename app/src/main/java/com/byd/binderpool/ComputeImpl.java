package com.byd.binderpool;

import android.os.RemoteException;


/**
 * 计算实现
 * <p/>
 * Created by wangchenlong on 16/6/17.
 */
final class ComputeImpl extends ICompute.Stub {
    @Override
    public int add(int a, int b) throws RemoteException {
        return a + b;
    }
}
