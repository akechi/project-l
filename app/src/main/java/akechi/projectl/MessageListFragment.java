package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.DateTime;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.EOFException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import akechi.projectl.async.LingrTaskLoader;
import jp.michikusa.chitose.lingr.Archive;
import jp.michikusa.chitose.lingr.Events;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;
import jp.michikusa.chitose.lingr.LingrException;
import jp.michikusa.chitose.lingr.Room;
import jp.michikusa.chitose.lingr.Room.Message;

public class MessageListFragment
    extends Fragment
    implements SwipeRefreshLayout.OnRefreshListener, LoaderManager.LoaderCallbacks<Iterable<Message>>, CometService.OnCometEventListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getLoaderManager().initLoader(LOADER_SHOW_ROOM, null, this);
        this.getLoaderManager().initLoader(LOADER_GET_ARCHIVE, null, this);

        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        final Account account= appContext.getAccount();
        if(account != null)
        {
            final String roomId= appContext.getRoomId(account);
            if(!Strings.isNullOrEmpty(roomId))
            {
                this.getLoaderManager().getLoader(LOADER_SHOW_ROOM).forceLoad();
            }
        }

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext());
        {
            final IntentFilter ifilter= new IntentFilter(Event.AccountChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final AppContext appContext1= (AppContext)MessageListFragment.this.getActivity().getApplicationContext();
                    final Account account= appContext.getAccount();
                    final String roomId= appContext.getRoomId(account);

                    MessageListFragment.this.onRoomSelected(roomId);
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

                    MessageListFragment.this.onRoomSelected(roomId);
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
        {
            final IntentFilter ifilter= new IntentFilter(Event.Reload.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final AppContext appContext= (AppContext)MessageListFragment.this.getActivity().getApplicationContext();
                    final Account account= appContext.getAccount();
                    final String roomId= appContext.getRoomId(account);
                    if(Strings.isNullOrEmpty(roomId))
                    {
                        return;
                    }
                    MessageListFragment.this.onRoomSelected(roomId);
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext());
        for(final BroadcastReceiver receiver : this.receivers)
        {
            lbMan.unregisterReceiver(receiver);
        }
        this.receivers.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View view=  inflater.inflate(R.layout.fragment_message_list, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.scrollView);
        this.messageView = (ListView)view.findViewById(R.id.messageListView);

        this.swipeRefreshLayout.setOnRefreshListener(this);
        this.messageView.setAdapter(new MessageAdapter(this.getActivity(), Collections.<Message>emptyList()));
        this.registerForContextMenu(this.messageView);

        return view;
    }

    private void onRoomSelected(CharSequence roomId)
    {
        Log.i("MessageListFragment", "On room selected " + roomId);
        final MessageAdapter adapter= (MessageAdapter)this.messageView.getAdapter();
        adapter.clear();
        adapter.notifyDataSetChanged();

        this.swipeRefreshLayout.setRefreshing(true);
        this.getLoaderManager().getLoader(LOADER_SHOW_ROOM).forceLoad();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        final MenuInflater inflator= this.getActivity().getMenuInflater();
        inflator.inflate(R.menu.fragment_message_list_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        super.onContextItemSelected(item);

        switch(item.getItemId())
        {
            case R.id.menu_item_copy:{
                final ClipboardManager clipMan= (ClipboardManager)this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                final int pos= ((ListView.AdapterContextMenuInfo)item.getMenuInfo()).position;
                final Message message= (Message)this.messageView.getAdapter().getItem(pos);
                clipMan.setPrimaryClip(ClipData.newPlainText("Lingr Message Text", message.getText()));

                Toast.makeText(this.getActivity(), "Copied text to clipboard", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.menu_item_reply:{
                final int pos= ((ListView.AdapterContextMenuInfo)item.getMenuInfo()).position;
                final Message message= (Message)this.messageView.getAdapter().getItem(pos);
                final Intent intent= new Intent("akechi.projectl.ReplyAction");
                intent.putExtra("text", message.getText());

                final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext());
                lbMan.sendBroadcast(intent);
                break;
            }
            case R.id.menu_item_share:{
                final int pos= ((ListView.AdapterContextMenuInfo)item.getMenuInfo()).position;
                final Message message= (Message)this.messageView.getAdapter().getItem(pos);
                final Intent intent= new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, message.getText());

                this.getActivity().startActivity(intent);
                break;
            }
        }
        return false;
    }

    @Override
    public void onRefresh()
    {
        this.getLoaderManager().getLoader(LOADER_GET_ARCHIVE).forceLoad();
    }

    @Override
    public void onLoadFinished(Loader<Iterable<Message>> loader, Iterable<Message> data)
    {
        switch(loader.getId())
        {
            case LOADER_SHOW_ROOM:{
                if(data != null && !Iterables.isEmpty(data))
                {
                    final MessageAdapter adapter= (MessageAdapter)this.messageView.getAdapter();
                    for(final Message m : data)
                    {
                        adapter.add(m);
                    }
                    ((MessageAdapter)this.messageView.getAdapter()).notifyDataSetChanged();
                    this.messageView.setSelection(this.messageView.getAdapter().getCount() - 1);
                }
                this.swipeRefreshLayout.setRefreshing(false);
                break;
            }
            case LOADER_GET_ARCHIVE:{
                if(data != null && !Iterables.isEmpty(data))
                {
                    final MessageAdapter adapter= (MessageAdapter)this.messageView.getAdapter();
                    adapter.insertHead(Lists.newArrayList(data));
                    adapter.notifyDataSetChanged();
                    this.messageView.setSelection(Iterables.size(data));
                }
                this.swipeRefreshLayout.setRefreshing(false);
                break;
            }
            default:
                throw new AssertionError("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public Loader<Iterable<Message>> onCreateLoader(int id, Bundle args)
    {
        switch(id)
        {
            case LOADER_SHOW_ROOM:
                return new MessageListLoader(this.getActivity());
            case LOADER_GET_ARCHIVE:
                return new ArchiveLoader(this.getActivity(), new Supplier<Message>(){
                    @Override
                    public Message get()
                    {
                        if(messageView.getCount() <= 0)
                        {
                            return null;
                        }
                        return (Message)messageView.getAdapter().getItem(0);
                    }
                });
            default:
                throw new AssertionError("Unknown loader id: " + id);
        }
    }

    @Override
    public void onLoaderReset(Loader<Iterable<Message>> loader)
    {
        Log.i("MessageListFragment", "On reset");
    }

    @Override
    public void onCometEvent(Events events)
    {
        Log.i("MessageListFragment", "events = " + events);
        final List<Message> messages= Lists.newLinkedList();
        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        final Account account= appContext.getAccount();
        final String roomId= appContext.getRoomId(account);
        for(final Events.Event event : events.getEvents())
        {
            Log.i("onCometEvent", "message = " + event.getMessage());
            if(event.getMessage() != null && event.getMessage().getRoom().equals(roomId))
            {
                messages.add(event.getMessage());
            }
        }
        if(!messages.isEmpty())
        {
            // follow selection when last message is visible
            boolean follow= (this.messageView.getCount() - 1) == this.messageView.getLastVisiblePosition();
            final MessageAdapter adapter= (MessageAdapter)this.messageView.getAdapter();
            for(final Message message : messages)
            {
                adapter.add(message);
            }
            adapter.notifyDataSetChanged();
            if(follow)
            {
                this.messageView.smoothScrollByOffset(this.messageView.getCount() - 1);
            }
        }
    }

    public static final class MessageAdapter
        extends BaseAdapter
    {
        public MessageAdapter(Context context, List<? extends Message> messages)
        {
            this.context = context;
            this.messages.addAll(messages);
        }

        public void insertHead(Collection<? extends Message> messages)
        {
            this.messages.addAll(0, messages);
        }

        public void add(Message e)
        {
            this.messages.add(e);
        }

        public void clear()
        {
            this.messages.clear();
        }

        @Override
        public int getCount()
        {
            return this.messages.size();
        }

        @Override
        public Message getItem(int position)
        {
            return this.messages.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            final View view;
            if(convertView == null)
            {
                final LayoutInflater inflater= (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view= inflater.inflate(R.layout.custom_message, null);
            }
            else
            {
                view= convertView;
            }

            final Message data= this.getItem(position);
            final ImageView iconView= (ImageView)view.findViewById(R.id.iconView);
            iconView.setImageURI(Uri.parse(data.getIconUrl()));

            final TextView nicknameView= (TextView)view.findViewById(R.id.nicknameView);
            nicknameView.setText(data.getNickname());

            final TextView timestampView= (TextView)view.findViewById(R.id.timestampView);
            {
                final Date timestamp= new Date(new DateTime(data.getTimestamp()).getValue());
                timestampView.setText(DateFormat.getDateTimeInstance().format(timestamp));
            }

            final TextView textView= (TextView)view.findViewById(R.id.textView);
            textView.setText(data.getText());

            return view;
        }

        private final Context context;

        private final List<Message> messages = new LinkedList<>();
    }

    private static final class MessageListLoader
        extends LingrTaskLoader<Iterable<Message>>
    {
        public MessageListLoader(Context context)
        {
            super(context);
        }

        @Override
        public Iterable<Message> loadInBackground(CharSequence authToken, LingrClient lingr)
            throws IOException, LingrException
        {
            final AppContext appContext= this.getApplicationContext();
            final Account account= appContext.getAccount();
            final String roomId= appContext.getRoomId(account);
            if(Strings.isNullOrEmpty(roomId))
            {
                return Collections.<Message>emptyList();
            }

            final Room room= lingr.showRoom(authToken, roomId);
            final Room.RoomInfo info= Iterables.find(room.getRooms(), new Predicate<Room.RoomInfo>() {
                @Override
                public boolean apply(Room.RoomInfo input) {
                    return roomId.equals(input.getId());
                }
            });
            return info.getMessages();
        }
    }

    private static final class ArchiveLoader
        extends LingrTaskLoader<Iterable<Message>>
    {
        public ArchiveLoader(Context context, Supplier<Message> oldestMessageSupplier)
        {
            super(context);

            this.oldestMessageSupplier= oldestMessageSupplier;
        }

        @Override
        public Iterable<Message> loadInBackground(CharSequence authToken, LingrClient lingr)
            throws IOException, LingrException
        {
            final Message oldest= this.oldestMessageSupplier.get();
            if(oldest == null)
            {
                return Collections.emptyList();
            }
            final AppContext appContext= this.getApplicationContext();
            final Account account= appContext.getAccount();
            final String roomId= appContext.getRoomId(account);
            if(Strings.isNullOrEmpty(roomId))
            {
                return Collections.emptyList();
            }
            final Archive archive= lingr.getArchive(authToken, roomId, oldest.getId(), 100);
            return archive.getMessages();
        }

        private final Supplier<Message> oldestMessageSupplier;
    }

    private static final int LOADER_SHOW_ROOM= 0;
    private static final int LOADER_GET_ARCHIVE= 1;

    private SwipeRefreshLayout swipeRefreshLayout;

    private ListView messageView;

    private final List<BroadcastReceiver> receivers= Lists.newLinkedList();
}
