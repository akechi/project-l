package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.text.DateFormat;
import java.util.List;
import java.util.Map;

import jp.michikusa.chitose.lingr.Events;

public class HomeActivity
    extends AppCompatActivity
    implements CometService.OnCometEventListener
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
        if(account != null && !Strings.isNullOrEmpty(appContext.getRoomId(account)))
        {
            pager.setCurrentItem(SwipeSwitcher.POS_ROOM);
        }
        else
        {
            pager.setCurrentItem(SwipeSwitcher.POS_ROOM_LIST);
        }

        // Setup ActionBar
        appContext.getActionBarMode().applyActionBar(appContext, this.getSupportActionBar());

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
        {
            final IntentFilter ifilter= new IntentFilter(Event.PreferenceChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final AppContext appContext= (AppContext)HomeActivity.this.getApplicationContext();
                    appContext.getActionBarMode().applyActionBar(appContext, HomeActivity.this.getSupportActionBar());
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }

        final Intent service= new Intent(this, CometService.class);
        this.startService(service);

        // handle explicit intent
        if(Event.OnNotificationTapped.ACTION.equals(this.getIntent().getAction()))
        {
            this.onNotificationTapped(this.getIntent());
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

        final AppContext appContext= (AppContext)this.getApplicationContext();
        if(!appContext.isBackgroundServiceEnabled())
        {
            Log.i("HomeActivity", "stopService");
            final Intent intent= new Intent(this, CometService.class);
            this.stopService(intent);
        }

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
        menu.add(Menu.NONE, MENU_ITEM_RELOAD, Menu.NONE, "Reload");
        menu.add(Menu.NONE, MENU_ITEM_PREFERENCE, Menu.NONE, "Settings");
        menu.add(Menu.NONE, MENU_ITEM_APP_INFO, Menu.NONE, "App Info");

        // Switch account
        final AppContext appContext= (AppContext)this.getApplicationContext();
        final Iterable<Account> accounts= appContext.getAccounts();
        if(Iterables.size(accounts) > 1)
        {
            final SubMenu subMenu= menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, "Switch account");
            for(final Account account : accounts)
            {
                subMenu.add(Menu.NONE, MENU_ITEM_ACCOUNT, Menu.NONE, account.name);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case MENU_ITEM_RELOAD:{
                final Intent intent= new Intent(Event.Reload.ACTION);
                final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getApplicationContext());
                lbMan.sendBroadcast(intent);
                return true;
            }
            case MENU_ITEM_PREFERENCE:{
                final Intent intent= new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            }
            case MENU_ITEM_APP_INFO:{
                final DialogFragment dialog= new AppInfoFragment();
                dialog.show(this.getSupportFragmentManager(), "dialog");
                return true;
            }
            case MENU_ITEM_ACCOUNT:{
                final AppContext appContext= (AppContext)this.getApplicationContext();
                final Optional<Account> account= Iterables.tryFind(appContext.getAccounts(), new AccountNameEquals(item.getTitle().toString()));
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
                }
                // apply ActionBar
                {
                    appContext.getActionBarMode().applyActionBar(appContext, this.getSupportActionBar());
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
        }
        return false;
    }

    private void onRoomSelected(CharSequence roomId)
    {
        final AppContext appContext= (AppContext)this.getApplicationContext();
        appContext.setRoomId(appContext.getAccount(), roomId);

        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        pager.setCurrentItem(SwipeSwitcher.POS_ROOM, true);

        appContext.getActionBarMode().applyActionBar(appContext, this.getSupportActionBar());
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

    private void onNotificationTapped(Intent intent)
    {
        final String accountName= intent.getStringExtra(Event.OnNotificationTapped.KEY_ACCOUNT_NAME);
        final String roomId= intent.getStringExtra(Event.OnNotificationTapped.KEY_ROOM_ID);
        final String messageId= intent.getStringExtra(Event.OnNotificationTapped.KEY_MESSAGE_ID);
        Log.i("notification tapped", "account name is " + accountName);
        Log.i("notification tapped", "room id is " + roomId);
        Log.i("notification tapped", "message id is " + messageId);

        final AppContext appContext= (AppContext)this.getApplicationContext();
        if(!appContext.getAccount().name.equals(accountName))
        {
            final Account account= Iterables.find(appContext.getAccounts(), new AccountNameEquals(accountName), null);
            if(account == null)
            {
                Toast.makeText(this, "Sorry, cannot find a message", Toast.LENGTH_SHORT);
                return;
            }

            Log.i("notification tapped", "change account");
            appContext.setAccount(account);

            final Intent event= new Intent(Event.AccountChange.ACTION);
            event.putExtra(Event.AccountChange.KEY_ACCOUNT, accountName);
            LocalBroadcastManager.getInstance(appContext).sendBroadcastSync(event);
        }

        final Account account= appContext.getAccount();
        if(!roomId.equals(appContext.getRoomId(account)))
        {
            Log.i("notification tapped", "change room");
            appContext.setRoomId(account, roomId);

            final Intent event= new Intent(Event.RoomChange.ACTION);
            event.putExtra(Event.RoomChange.KEY_ROOM_ID, roomId);
            LocalBroadcastManager.getInstance(appContext).sendBroadcastSync(event);
        }

        {
            Log.i("notification tapped", "find a message");
            final Intent event= new Intent(Event.FindMessage.ACTION);
            event.putExtra(Event.FindMessage.KEY_MESSAGE_ID, messageId);
            LocalBroadcastManager.getInstance(appContext).sendBroadcast(event);
        }
    }

    public static final class SwipeSwitcher
        extends FragmentStatePagerAdapter
    {
        public static final int POS_ROOM_LIST= 0;
        public static final int POS_ROOM= 1;
        public static final int NPAGES= 2;

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
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public int getCount()
        {
            return NPAGES;
        }

        private final Map<Integer, Fragment> fragments= Maps.newHashMap();
    }

    private static final class AccountNameEquals
        implements Predicate<Account>
    {
        public AccountNameEquals(CharSequence expects)
        {
            this.expects= expects.toString();
        }

        @Override
        public boolean apply(Account input)
        {
            return this.expects.equals(input.name);
        }

        private final String expects;
    }

    private static final int MENU_ITEM_RELOAD= 1;
    private static final int MENU_ITEM_PREFERENCE= 2;
    private static final int MENU_ITEM_APP_INFO= 3;
    private static final int MENU_ITEM_ACCOUNT= 4;

    private List<BroadcastReceiver> receivers= Lists.newLinkedList();
}

