package com.huawei.nb.client.callback;

import android.os.RemoteException;
import com.huawei.nb.callback.IUpdateCallback.Stub;

public class UpdateCallback extends Stub implements WaitableCallback<Integer> {
    private static final Integer INVALID_COUNT = Integer.valueOf(-1);
    private final CallbackManager callbackManager;
    private final CallbackWaiter<Integer> callbackWaiter = new CallbackWaiter(INVALID_COUNT);

    UpdateCallback(CallbackManager callbackManager) {
        this.callbackManager = callbackManager;
    }

    public void onResult(int transactionId, int count) throws RemoteException {
        this.callbackWaiter.set(transactionId, Integer.valueOf(count));
    }

    public Integer await(int transactionId, long timeout) {
        this.callbackManager.startWaiting(this);
        Integer result = (Integer) this.callbackWaiter.await(transactionId, timeout);
        this.callbackManager.stopWaiting(this);
        return result;
    }

    public void interrupt() {
        this.callbackWaiter.interrupt();
    }
}