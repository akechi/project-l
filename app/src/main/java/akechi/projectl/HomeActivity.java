package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import jp.michikusa.chitose.lingr.Events;

public class HomeActivity
    extends AppCompatActivity
    implements CometService.OnCometEventListener, ViewPager.OnPageChangeListener
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
        pager.addOnPageChangeListener(this);
        pager.setAdapter(new SwipeSwitcher(this.getSupportFragmentManager()));
        if(account != null && !Strings.isNullOrEmpty(appContext.getRoomId(account)))
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

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getApplicationContext());
        {
            final IntentFilter ifilter= new IntentFilter(CometService.class.getCanonicalName());
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final Events events= (Events)intent.getSerializableExtra("events");
                    HomeActivity.this.onCometEvent(events);
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
        {
            final IntentFilter ifilter= new IntentFilter(Event.RoomChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final String roomId= intent.getStringExtra(Event.RoomChange.KEY_ROOM_ID);
                    HomeActivity.this.onRoomSelected(roomId);
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }

        final Intent service= new Intent(this, CometService.class);
        this.startService(service);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {
    }

    @Override
    public void onPageSelected(int position)
    {
        final TextView whenceView= (TextView)this.findViewById(R.id.whenceView);
        final AppContext appContext= (AppContext)this.getApplicationContext();
        switch(position)
        {
            case SwipeSwitcher.POS_ROOM:{
                final Account account= appContext.getAccount();
                final String roomId= appContext.getRoomId(account);
                if(Iterables.size(appContext.getAccounts()) <= 1)
                {
                    whenceView.setText("You're in " + roomId);
                }
                else
                {
                    whenceView.setText(String.format("You're %s, in %s", account.name, roomId));
                }
                break;
            }
            case SwipeSwitcher.POS_ROOM_LIST:{
                whenceView.setText("Choose a room");
                break;
            }
            default:
                throw new AssertionError("Unknown page position: " + position);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            // swipe to room list when back button press in the room
            case KeyEvent.KEYCODE_BACK:{
                final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
                if(pager.getCurrentItem() == SwipeSwitcher.POS_ROOM)
                {
                    pager.setCurrentItem(SwipeSwitcher.POS_ROOM_LIST, true);
                    return true;
                }
                // fallthrough
            }
            default:
                return super.onKeyDown(keyCode, event);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        final AppContext appContext= (AppContext)this.getApplicationContext();
        final Iterable<Account> accounts= appContext.getAccounts();
        // only 1 account, make invisible
        if(Iterables.size(accounts) <= 1)
        {
            return false;
        }

        for(final Account account : accounts)
        {
            menu.add(account.name);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        final AppContext appContext= (AppContext)this.getApplicationContext();
        final String accountName= item.getTitle().toString();
        final Optional<Account> account= Iterables.tryFind(appContext.getAccounts(), new Predicate<Account>(){
            @Override
            public boolean apply(Account input)
            {
                return input.name.equals(accountName);
            }
        });
        if(!account.isPresent())
        {
            Toast.makeText(this, "Did you remove an account? Try again", Toast.LENGTH_SHORT).show();
            return true;
        }

        appContext.setAccount(account.get());

        // choose current page
        {
            final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
            if(Strings.isNullOrEmpty(appContext.getRoomId(account.get())))
            {
                pager.setCurrentItem(SwipeSwitcher.POS_ROOM_LIST);
            }
            else
            {
                pager.setCurrentItem(SwipeSwitcher.POS_ROOM);
            }
            this.onPageSelected(pager.getCurrentItem());
        }
        // trigger event
        {
            final Intent intent= new Intent(Event.AccountChange.ACTION);
            intent.putExtra(Event.AccountChange.KEY_ACCOUNT, account.get());
            final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getApplicationContext());
            lbMan.sendBroadcast(intent);
        }

        return true;
    }

    private void onRoomSelected(CharSequence roomId)
    {
        final AppContext appContext= (AppContext)this.getApplicationContext();
        appContext.setRoomId(appContext.getAccount(), roomId);

        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        pager.setCurrentItem(SwipeSwitcher.POS_ROOM, true);
    }

    @Override
    public void onCometEvent(Events events)
    {
        Log.i("HomeActivity", "events = " + events);
        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        final SwipeSwitcher adapter= (SwipeSwitcher)pager.getAdapter();
        final Fragment[] fragments= new Fragment[]{
                adapter.getFragment(SwipeSwitcher.POS_ROOM_LIST),
                adapter.getFragment(SwipeSwitcher.POS_ROOM),
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
            return 2;
        }

        private final Map<Integer, Fragment> fragments= Maps.newHashMap();
    }

    private List<BroadcastReceiver> receivers= Lists.newLinkedList();
}

