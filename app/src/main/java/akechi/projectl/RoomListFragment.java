package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
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
import java.util.Collections;
import java.util.List;

import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;

public class RoomListFragment
    extends Fragment
    implements ListView.OnItemClickListener, LoaderManager.LoaderCallbacks<Iterable<String>>, SwipeRefreshLayout.OnRefreshListener, AccountListFragment.OnAccountSelectedListener
{
    public static interface OnRoomSelectedListener
    {
        void onRoomSelected(CharSequence roomId);
    }

    @Override
    public void onAccountSelected(Account account)
    {
        this.swipeRefreshLayout.setRefreshing(true);
        this.onRefresh();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getLoaderManager().initLoader(0, null, this);
        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        if(appContext.getAccount() != null)
        {
            this.getLoaderManager().getLoader(0).forceLoad();
        }
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(activity instanceof OnRoomSelectedListener)
        {
            this.listeners.add((OnRoomSelectedListener)activity);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.listeners.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Iterable<OnRoomSelectedListener> listeners = Lists.newArrayList(this.listeners);
        final CharSequence roomId= (CharSequence) this.listView.getAdapter().getItem(position);

        for(final OnRoomSelectedListener listener : listeners)
        {
            try
            {
                listener.onRoomSelected(roomId.toString());
            }
            catch(Exception e)
            {
                Log.e("RoomListFragment", "Couldn't call a listener", e);
            }
        }
    }

    @Override
    public Loader<Iterable<String>> onCreateLoader(int id, final Bundle args)
    {
        Log.i("RoomListFragment", String.format("onCreateLoader() with id=%d, args=%s", id, args));
        return new RoomListLoader(this.getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Iterable<String>> loader, Iterable<String> data) {
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
        this.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<Iterable<String>> loader) {
        Log.i("RoomListFragment", "On reset");
        /* Log.i("RoomListFragment", "On reset, force loading"); */
        /* loader.forceLoad(); */
    }

    @Override
    public void onRefresh() {
        // this.getLoaderManager().destroyLoader(0);
        Log.i("RoomListFragment", "On refresh, restart");
        this.getLoaderManager().getLoader(0).forceLoad();
    }

    private static final class RoomListLoader
        extends AsyncTaskLoader<Iterable<String>>
    {
        public RoomListLoader(Context context)
        {
            super(context);
        }

        @Override
        public Iterable<String> loadInBackground()
        {
            final AppContext appCtx= (AppContext)this.getContext().getApplicationContext();
            final Account account= appCtx.getAccount();
            Log.i("RoomListFragment", "Start to load room list for " + account);
            if(account == null)
            {
                return Collections.emptyList();
            }
            try
            {
                final AccountManager manager= AccountManager.get(this.getContext());
                String authToken= manager.blockingGetAuthToken(account, "", true);
                final LingrClient lingr= lingrFactory.newLingrClient();
                if(!lingr.verifySession(authToken))
                {
                    Log.i("RoomListFragment", "authToken " + authToken + " is expired");
                    manager.invalidateAuthToken("com.lingr", authToken);
                    authToken= manager.blockingGetAuthToken(account, "", true);
                }

                Log.i("RoomListFragment", "call getRooms() with " + authToken);
                return lingr.getRooms(authToken);
            }
            catch(EOFException e)
            {
                Log.e("RoomListFragment", "Couldn't get room list for " + account.name, e);
                final Handler handler= new Handler(Looper.getMainLooper());
                final Context ctx= this.getContext();
                handler.post(new Runnable(){
                    @Override
                    public void run()
                    {
                        Toast.makeText(ctx, "Unexpectedly EOFException detected", Toast.LENGTH_LONG).show();
                    }
                });
                return Collections.<String>emptyList();
            }
            catch(Exception e)
            {
                Log.e("RoomListFragment", "Couldn't get room list for " + account.name, e);
                return Collections.<String>emptyList();
            }
        }
    }

    private static final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());

    private SwipeRefreshLayout swipeRefreshLayout;

    private ListView listView;

    private List<OnRoomSelectedListener> listeners= Lists.newArrayList();
}
