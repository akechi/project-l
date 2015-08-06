package akechi.projectl;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

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
        throw new AssertionError("Cannot bind this service");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        this.notifMan= (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
        final Notification notif= new Notification.Builder(this)
            .setContentTitle("ProjectL ...started")
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class), 0))
            // for backword-compatibility
            .getNotification()
        ;
        this.notifMan.notify(0, notif);

        this.loader= new SubscribeLoader(this){
            @Override
            protected Void onLoadInBackground()
            {
                try
                {
                    return super.onLoadInBackground();
                }
                finally
                {
                    CometService.this.scheduleNext();
                    return null;
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        Log.i("CometService", "forceLoad()");
        this.loader.forceLoad();

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    private void scheduleNext()
    {
        final PendingIntent intent= PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        final AlarmManager alarmMan= (AlarmManager)this.getSystemService(ALARM_SERVICE);
        // re-invoke myself every 500ms
        alarmMan.set(AlarmManager.RTC, System.currentTimeMillis() + 500, intent);
    }

    private class SubscribeLoader
        extends LingrTaskLoader<Void>
    {
        public SubscribeLoader(Context context)
        {
            super(context);
        }

        @Override
        public Void loadInBackground(CharSequence authToken, LingrClient lingr)
                throws IOException, LingrException
        {
            final Iterable<String> roomIds= lingr.getRooms(authToken);
            lingr.unsubscribe(authToken, roomIds);
            final long counter= lingr.subscribe(authToken, true, roomIds);
            // sometimes, lingr returns 0 value for counter
            // and observe with counter = 0, cause errors
            Log.i("CometService", "counter = " + counter);
            if(counter <= 0)
            {
                // retry
                return null;
            }
            CometService.this.counter= counter;
            CometService.this.loader= new ObserveLoader(this.getContext()){
                @Override
                protected Void onLoadInBackground()
                {
                    try
                    {
                        return super.onLoadInBackground();
                    }
                    finally
                    {
                        CometService.this.scheduleNext();
                        return null;
                    }
                }
            };
            return null;
        }

        @Override
        protected void showMessage(CharSequence message)
        {
            // suppress message
            Log.i("CometService", "" + message);
        }
    }

    private class ObserveLoader
        extends LingrTaskLoader<Void>
    {
        public ObserveLoader(Context context)
        {
            super(context);
        }

        @Override
        public Void loadInBackground(CharSequence authToken, LingrClient lingr)
            throws IOException, LingrException
        {
            Log.i("CometService", "counter = " + CometService.this.counter);
            final Events events= lingr.observe(authToken, CometService.this.counter, 30, TimeUnit.MINUTES);
            // sometimes, lingr returns 0 value for counter
            // and observe with counter = 0, cause errors
            final long counter= events.getCounter();
            Log.i("CometService", "counter = " + counter);
            if(counter <= 0)
            {
                // retry
                return null;
            }
            final Intent intent= new Intent(CometService.class.getCanonicalName());
            intent.putExtra("events", (Serializable) events);
            CometService.this.sendBroadcast(intent);

            CometService.this.counter= events.getCounter();
            return null;
        }

        @Override
        protected void showMessage(CharSequence message)
        {
            // suppress message
            Log.i("CometService", "" + message);
        }
    }

    private NotificationManager notifMan;

    private long counter;

    private Loader<Void> loader;
}
