package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.common.collect.Lists;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import akechi.projectl.async.LingrTaskLoader;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;
import jp.michikusa.chitose.lingr.LingrException;

public class RoomListFragment
    extends Fragment
    implements ListView.OnItemClickListener, LoaderManager.LoaderCallbacks<Iterable<String>>, SwipeRefreshLayout.OnRefreshListener
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getLoaderManager().initLoader(0, null, this);
        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        if(appContext.getAccount() != null)
        {
            this.getLoaderManager().getLoader(0).forceLoad();
        }

        {
            final IntentFilter ifilter= new IntentFilter(Event.AccountChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    RoomListFragment.this.swipeRefreshLayout.setRefreshing(true);
                    RoomListFragment.this.onRefresh();
                }
            };
            this.getActivity().registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        for(final BroadcastReceiver receiver : this.receivers)
        {
            this.getActivity().unregisterReceiver(receiver);
        }
        this.receivers.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.fragment_room_list, container, false);

        this.swipeRefreshLayout= (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshView);
        this.swipeRefreshLayout.setOnRefreshListener(this);
        this.listView = (ListView)view.findViewById(android.R.id.list);
        this.listView.setAdapter(new ArrayAdapter<String>(
                this.getActivity(),
                android.R.layout.simple_list_item_1,
                new String[0]
        ));
        this.listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final CharSequence roomId= (CharSequence) this.listView.getAdapter().getItem(position);

        final Intent intent= new Intent(Event.RoomChange.ACTION);
        intent.putExtra(Event.RoomChange.KEY_ROOM_ID, roomId.toString());
        this.getActivity().sendBroadcast(intent);
    }

    @Override
    public Loader<Iterable<String>> onCreateLoader(int id, final Bundle args)
    {
        return new RoomListLoader(this.getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Iterable<String>> loader, Iterable<String> data) {
        this.swipeRefreshLayout.setRefreshing(false);

        final Iterable<String> loaded = (data != null) ? data : Collections.<String>emptyList();
        final List<String> roomIds= Lists.newArrayList();
        for(final String roomId : loaded)
        {
            roomIds.add(roomId);
        }
        this.listView.setAdapter(new ArrayAdapter<String>(
                        this.getActivity(),
                        android.R.layout.simple_list_item_1,
                        roomIds.toArray(new String[0]))
        );
        Log.i("RoomListFragment", "Done for refreshing");
    }

    @Override
    public void onLoaderReset(Loader<Iterable<String>> loader) {
        Log.i("RoomListFragment", "On reset");
        /* Log.i("RoomListFragment", "On reset, force loading"); */
        /* loader.forceLoad(); */
    }

    @Override
    public void onRefresh() {
        Log.i("RoomListFragment", "On refresh, restart");
        this.getLoaderManager().getLoader(0).forceLoad();
    }

    private static final class RoomListLoader
        extends LingrTaskLoader<Iterable<String>>
    {
        public RoomListLoader(Context context)
        {
            super(context);
        }

        @Override
        public Iterable<String> loadInBackground(CharSequence authToken, LingrClient lingr)
            throws IOException, LingrException
        {
            return lingr.getRooms(authToken);
        }
    }

    private SwipeRefreshLayout swipeRefreshLayout;

    private ListView listView;

    private final List<BroadcastReceiver> receivers= Lists.newLinkedList();
}
