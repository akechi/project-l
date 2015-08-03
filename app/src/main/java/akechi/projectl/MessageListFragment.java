package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
    implements SwipeRefreshLayout.OnRefreshListener, LoaderManager.LoaderCallbacks<Iterable<Message>>, RoomListFragment.OnRoomSelectedListener
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View view=  inflater.inflate(R.layout.fragment_message_list, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.scrollView);
        this.messageView = (ListView)view.findViewById(R.id.messageListView);

        this.swipeRefreshLayout.setOnRefreshListener(this);
        this.messageView.setAdapter(new MessageAdapter(this.getActivity(), Collections.<Message>emptyList()));

        return view;
    }

    @Override
    public void onRoomSelected(CharSequence roomId)
    {
        Log.i("MessageListFragment", "On room selected " + roomId);
        final MessageAdapter adapter= (MessageAdapter)this.messageView.getAdapter();
        adapter.clear();
        adapter.notifyDataSetChanged();

        this.swipeRefreshLayout.setRefreshing(true);
        this.getLoaderManager().getLoader(LOADER_SHOW_ROOM).forceLoad();
    }

    @Override
    public void onRefresh()
    {
        this.getLoaderManager().getLoader(LOADER_GET_ARCHIVE).forceLoad();
    }

    @Override
    public void onLoadFinished(Loader<Iterable<Message>> loader, Iterable<Message> data)
    {
        this.swipeRefreshLayout.setRefreshing(false);
        if(data == null)
        {
            return;
        }
        Log.i("MessageListFragment", String.format("got %d messages", Iterables.size(data)));
        switch(loader.getId())
        {
            case LOADER_SHOW_ROOM:{
                final MessageAdapter adapter= (MessageAdapter)this.messageView.getAdapter();
                for(final Message m : data)
                {
                    adapter.add(m);
                }
                ((MessageAdapter)this.messageView.getAdapter()).notifyDataSetChanged();
                this.messageView.setSelection(this.messageView.getAdapter().getCount() - 1);
                break;
            }
            case LOADER_GET_ARCHIVE:{
                final MessageAdapter adapter= (MessageAdapter)this.messageView.getAdapter();
                adapter.insertHead(Lists.newArrayList(data));
                adapter.notifyDataSetChanged();
                this.messageView.setSelection(Iterables.size(data));
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
            final Room.RoomInfo info= Iterables.find(room.getRooms(), new Predicate<Room.RoomInfo>(){
                @Override
                public boolean apply(Room.RoomInfo input)
                {
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
}
