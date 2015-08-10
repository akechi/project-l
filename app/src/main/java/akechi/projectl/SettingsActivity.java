package akechi.projectl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.google.common.collect.Lists;

import java.util.List;

public class SettingsActivity
    extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_settings);

        final ActionBar bar= this.getSupportActionBar();
        bar.setHomeButtonEnabled(true);

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getApplicationContext());
        {
            final IntentFilter ifilter= new IntentFilter(Event.PreferenceChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    SettingsActivity.this.finish();
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getApplicationContext());
        for(final BroadcastReceiver receiver : this.receivers)
        {
            lbMan.unregisterReceiver(receiver);
        }
        this.receivers.clear();
    }

    private final List<BroadcastReceiver> receivers= Lists.newLinkedList();
}
