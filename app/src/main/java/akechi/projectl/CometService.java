package akechi.projectl;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

import akechi.projectl.async.LingrTaskLoader;
import jp.michikusa.chitose.lingr.Events;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrException;

public class CometService
    extends Service
{
    public static interface OnCometEventListener
    {
        void onCometEvent(Events events);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return new CometBinder();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i("CometService", "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("CometService", "onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.i("CometService", "onDestroy()");
    }

    private final class CometBinder
        extends Binder
    {
        public CometService getService()
        {
            return CometService.this;
        }
    }
}
