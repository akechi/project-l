package akechi.projectl;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.DateTime;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import akechi.projectl.async.LingrTaskLoader;
import jp.michikusa.chitose.lingr.Events;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrException;
import jp.michikusa.chitose.lingr.Room;

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

        this.loader= this.newSubscribeLoader();

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getApplicationContext());
        {
            final IntentFilter ifilter= new IntentFilter(Event.AccountChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final CometService that= CometService.this;
                    final Loader<Void> oldLoader= that.loader;

                    oldLoader.abandon();

                    that.loader= that.newSubscribeLoader();
                    that.loader.forceLoad();
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
        {
            final IntentFilter ifilter= new IntentFilter(Event.PreferenceChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final CometService that= CometService.this;
                    final Loader<Void> oldLoader= that.loader;

                    oldLoader.abandon();

                    that.loader= that.newSubscribeLoader();
                    that.loader.forceLoad();
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
        {
            final IntentFilter ifilter= new IntentFilter(CometService.class.getCanonicalName());
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final AppContext appContext= (AppContext)CometService.this.getApplicationContext();
                    final Iterable<Pattern> patterns= this.buildPatterns(appContext);
                    final Events events= (Events)intent.getSerializableExtra("events");
                    for(final Events.Event event : events.getEvents())
                    {
                        if(event.getMessage() == null)
                        {
                            continue;
                        }

                        final Room.Message message= event.getMessage();
                        // exclude message by myself
                        if(appContext.getAccount().name.equals(message.getSpeakerId()))
                        {
                            continue;
                        }
                        // exclude message by bot
                        if("bot".equals(message.getType()))
                        {
                            continue;
                        }
                        final boolean found= Iterables.any(patterns, new Predicate<Pattern>(){
                            @Override
                            public boolean apply(Pattern input)
                            {
                                return input.matcher(message.getText()).find();
                            }
                        });
                        if(found)
                        {
                            final Intent onClickIntent= new Intent(appContext, HomeActivity.class);
                            onClickIntent.setAction(Event.OnNotificationTapped.ACTION);
                            onClickIntent.putExtra(Event.OnNotificationTapped.KEY_ACCOUNT_NAME, appContext.getAccount().name);
                            onClickIntent.putExtra(Event.OnNotificationTapped.KEY_ROOM_ID, message.getRoom());
                            onClickIntent.putExtra(Event.OnNotificationTapped.KEY_MESSAGE_ID, message.getId());

                            final PendingIntent pendingIntent= PendingIntent.getActivity(appContext, 0, onClickIntent, PendingIntent.FLAG_ONE_SHOT);

                            final Notification notif= new NotificationCompat.Builder(CometService.this)
                                .setSmallIcon(R.drawable.icon_notif_star)
                                .setContentTitle("Found the highlighted message")
                                .setContentText("Room: " + message.getRoom())
                                .setSubText(message.getText())
                                .setContentInfo("From: " + message.getNickname())
                                .setTicker(DateFormat.getDateTimeInstance().format(new Date(new DateTime(message.getTimestamp()).getValue())))
                                .setAutoCancel(true)
                                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                .setVibrate(new long[]{1000, 1000})
                                .setContentIntent(pendingIntent)
                                .build()
                            ;
                            NotificationManagerCompat.from(appContext)
                                .notify(0, notif)
                            ;
                        }
                    }
                }

                private Iterable<Pattern> buildPatterns(AppContext ctx)
                {
                    final Iterable<String> patterns= Splitter.on(System.getProperty("line.separator")).split(ctx.getHighlightPattern());
                    if(Iterables.isEmpty(patterns))
                    {
                        return null;
                    }
                    final Iterable<Pattern> transformed= Iterables.transform(patterns, new Function<String, Pattern>(){
                        @Override
                        public Pattern apply(String input)
                        {
                            if(Strings.isNullOrEmpty(input))
                            {
                                return null;
                            }
                            try
                            {
                                return Pattern.compile(input);
                            }
                            catch(PatternSyntaxException e)
                            {
                                return null;
                            }
                        }
                    });
                    return ImmutableList.copyOf(Iterables.filter(transformed, Predicates.notNull()));
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
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

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getApplicationContext());
        for(final BroadcastReceiver receiver : this.receivers)
        {
            lbMan.unregisterReceiver(receiver);
        }
        this.receivers.clear();
    }

    private void scheduleNext()
    {
        final PendingIntent intent= PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        final AlarmManager alarmMan= (AlarmManager)this.getSystemService(ALARM_SERVICE);
        // re-invoke myself every 500ms
        alarmMan.set(AlarmManager.RTC, System.currentTimeMillis() + 500, intent);
    }

    private SubscribeLoader newSubscribeLoader()
    {
        return new SubscribeLoader(this){
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

    private ObserveLoader newObserveLoader()
    {
        return new ObserveLoader(this){
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
                }
            }

            @Override
            protected void onLoadingFailed(Throwable e)
            {
                if(e instanceof LingrException)
                {
                    Log.e("CometService", "Oops, restarting service...", e);
                    CometService.this.loader.abandon();
                    CometService.this.loader= CometService.this.newSubscribeLoader();
                    CometService.this.scheduleNext();
                }
                else
                {
                    super.onLoadingFailed(e);
                }
            }
        };
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
            final AppContext appContext= this.getApplicationContext();
            final Account account= appContext.getAccount();
            final Iterable<String> ids= appContext.getRoomIds(account);
            final Iterable<String> roomIds;
            if(Iterables.isEmpty(ids))
            {
                roomIds= lingr.getRooms(authToken);
            }
            else
            {
                roomIds= ids;
            }
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
            CometService.this.loader= CometService.this.newObserveLoader();
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
            CometService.this.counter= events.getCounter();

            final Intent intent= new Intent(CometService.class.getCanonicalName());
            intent.putExtra("events", (Serializable) events);
            if(CometService.this.loader == this)
            {
                final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(CometService.this.getApplicationContext());
                lbMan.sendBroadcast(intent);
            }
            return null;
        }

        @Override
        protected void showMessage(CharSequence message)
        {
            // suppress message
            Log.i("CometService", "" + message);
        }
    }

    private long counter;

    private Loader<Void> loader;

    private final List<BroadcastReceiver> receivers= Lists.newLinkedList();
}
