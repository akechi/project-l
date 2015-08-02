package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.google.common.collect.Maps;

import java.util.Map;

public class HomeActivity
    extends AppCompatActivity
    implements RoomListFragment.OnRoomSelectedListener
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

        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        pager.setAdapter(new SwipeSwitcher(this.getSupportFragmentManager()));
        pager.setCurrentItem(SwipeSwitcher.POS_ROOM_LIST);

        final ActionBar bar= this.getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayUseLogoEnabled(true);
        bar.setLogo(R.drawable.icon_logo);
    }

    /* @Override */
    /* public boolean onCreateOptionsMenu(Menu menu) */
    /* { */
    /*     // Inflate the menu; this adds items to the action bar if it is present. */
    /*     this.getMenuInflater().inflate(akechi.projectl.R.menu.main, menu); */
    /*     return true; */
    /* } */

    @Override
    public void onRoomSelected(CharSequence roomId)
    {
        final ViewPager pager= (ViewPager)this.findViewById(R.id.pager);
        pager.setCurrentItem(SwipeSwitcher.POS_ROOM, true);

        final SwipeSwitcher adapter= (SwipeSwitcher)pager.getAdapter();
        final Fragment fragment= adapter.getFragment(SwipeSwitcher.POS_ROOM);
        ((RoomListFragment.OnRoomSelectedListener)fragment).onRoomSelected(roomId);
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
}

