package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Map;

import jp.michikusa.chitose.lingr.Events;

public class HomeActivity
    extends AppCompatActivity
    implements RoomListFragment.OnRoomSelectedListener, AccountListFragment.OnAccountSelectedListener, CometService.OnCometEventListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_home);

        final AccountManager manager= AccountManager.get(this);
        Account[] accounts= manager.getAccountsByType("com.lingr");
        if(accounts.length <= 0)
        {
            manager.addAccount("com.lingr", "", null, null, this, null, null);
            this.finish();
            return;
        }
        final AppContext appContext= (AppContext)this.getApplicationContext();
        // restore state
        {
            final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
            final String name= prefs.getString("account.name", "");
            final String type= prefs.getString("account.type", "");
            if(!Strings.isNullOrEmpty(name) && !Strings.isNullOrEmpty(type))
            {
                final Account account= new Account(name, type);
                appContext.setAccount(account);
            }
        }
        if(savedInstanceState != null)
        {
            final Account account = savedInstanceState.getParcelable("account");
            if(account != null)
            {
                appContext.setAccount(account);
            }
        }
        final Account account= appContext.getAccount();

        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        pager.setAdapter(new SwipeSwitcher(this.getSupportFragmentManager()));
        if(account == null)
        {
            pager.setCurrentItem(SwipeSwitcher.POS_ACCOUNT_LIST);
        }
        else if(!Strings.isNullOrEmpty(appContext.getRoomId(account)))
        {
            pager.setCurrentItem(SwipeSwitcher.POS_ROOM);
        }
        else
        {
            pager.setCurrentItem(SwipeSwitcher.POS_ROOM_LIST);
        }

        final ActionBar bar= this.getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayUseLogoEnabled(true);
        bar.setLogo(R.drawable.icon_logo);
        if(account != null)
        {
            bar.setTitle(String.format("%s - %s", this.getString(R.string.app_name), account.name));
        }

        final Intent service= new Intent(this, CometService.class);
        this.startService(service);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        Log.i("HomeActivity", "onSaveInstanceState()");
        final AppContext appContext= (AppContext)this.getApplicationContext();
        outState.putParcelable("account", appContext.getAccount());
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        final AppContext appContext= (AppContext)this.getApplicationContext();

        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor= prefs.edit();

        final Account account= appContext.getAccount();
        if(account != null)
        {
            editor.putString("account.name", account.name);
            editor.putString("account.type", account.type);
        }

        editor.commit();

        // this.unbindService(this.serviceConnection);
        // this.serviceConnection= null;
    }

    /* @Override */
    /* public boolean onCreateOptionsMenu(Menu menu) */
    /* { */
    /*     // Inflate the menu; this adds items to the action bar if it is present. */
    /*     this.getMenuInflater().inflate(akechi.projectl.R.menu.main, menu); */
    /*     return true; */
    /* } */

    @Override
    public void onAccountSelected(Account account)
    {
        final AppContext appCtx= (AppContext)this.getApplicationContext();
        appCtx.setAccount(account);

        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        if(Strings.isNullOrEmpty(appCtx.getRoomId(account)))
        {
            pager.setCurrentItem(SwipeSwitcher.POS_ROOM_LIST);
        }
        else
        {
            pager.setCurrentItem(SwipeSwitcher.POS_ROOM);
        }

        final SwipeSwitcher adapter= (SwipeSwitcher)pager.getAdapter();
        final Fragment[] fragments= new Fragment[]{
            adapter.getFragment(SwipeSwitcher.POS_ROOM),
            adapter.getFragment(SwipeSwitcher.POS_ROOM_LIST),
            adapter.getFragment(SwipeSwitcher.POS_ACCOUNT_LIST),
        };
        for(final Fragment fragment : fragments)
        {
            if(fragment instanceof AccountListFragment.OnAccountSelectedListener)
            {
                ((AccountListFragment.OnAccountSelectedListener)fragment).onAccountSelected(account);
            }
        }

        this.getSupportActionBar().setTitle(String.format("%s - %s", this.getString(R.string.app_name), account.name));
    }

    @Override
    public void onRoomSelected(CharSequence roomId)
    {
        final AppContext appContext= (AppContext)this.getApplicationContext();
        appContext.setRoomId(appContext.getAccount(), roomId);

        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        pager.setCurrentItem(SwipeSwitcher.POS_ROOM, true);

        final SwipeSwitcher adapter= (SwipeSwitcher)pager.getAdapter();
        final Fragment fragment= adapter.getFragment(SwipeSwitcher.POS_ROOM);
        ((RoomListFragment.OnRoomSelectedListener)fragment).onRoomSelected(roomId);
    }

    @Override
    public void onCometEvent(Events events)
    {
        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        final SwipeSwitcher adapter= (SwipeSwitcher)pager.getAdapter();
        final Fragment[] fragments= new Fragment[]{
                adapter.getFragment(SwipeSwitcher.POS_ROOM_LIST),
                adapter.getFragment(SwipeSwitcher.POS_ROOM),
                adapter.getFragment(SwipeSwitcher.POS_ACCOUNT_LIST),
        };
        for(final Fragment fragment : fragments)
        {
            if(fragment instanceof CometService.OnCometEventListener)
            {
                ((CometService.OnCometEventListener)fragment).onCometEvent(events);
            }
        }
    }

    public static final class SwipeSwitcher
        extends FragmentStatePagerAdapter
    {
        public static final int POS_ROOM_LIST= 0;
        public static final int POS_ROOM= 1;
        public static final int POS_ACCOUNT_LIST= 2;

        public SwipeSwitcher(FragmentManager fm)
        {
            super(fm);
        }

        public Fragment getFragment(int position)
        {
            return this.getItem(position);
        }

        @Override
        public Fragment getItem(int position)
        {
            if(this.fragments.containsKey(position))
            {
                return this.fragments.get(position);
            }

            switch(position)
            {
                case POS_ROOM_LIST:{
                    final Fragment fragment= new RoomListFragment();
                    this.fragments.put(position, fragment);
                    return fragment;
                }
                case POS_ROOM:{
                    final Fragment fragment= new RoomFragment();
                    this.fragments.put(position, fragment);
                    return fragment;
                }
                case POS_ACCOUNT_LIST:{
                    final Fragment fragment= new AccountListFragment();
                    this.fragments.put(position, fragment);
                    return fragment;
                }
                /* case 2: { */
                /*     final Fragment fragment = new SettingsFragment(); */
                /*     this.fragments.put(position, fragment); */
                /*     return fragment; */
                /* } */
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public int getCount()
        {
            return 3;
        }

        private final Map<Integer, Fragment> fragments= Maps.newHashMap();
    }

    private static final class CometServiceConnection
        implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.i("CometServiceConnection", "onServiceConnected()");
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.i("CometServiceConnection", "onServiceDisconnected()");
        }
    }

    private ServiceConnection serviceConnection;
}

