package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrException;
import jp.michikusa.chitose.lingr.Room;

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
        final MessageListFragment fragment= (MessageListFragment)adapter.getFragment(SwipeSwitcher.POS_ROOM);

        fragment.onRoomSelected(roomId);
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
                    final Fragment fragment= new MessageListFragment();
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

