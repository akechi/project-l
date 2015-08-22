package akechi.projectl.component;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.util.DateTime;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import akechi.projectl.AppContext;
import akechi.projectl.R;
import jp.michikusa.chitose.lingr.Room;

public class MessageAdapter
    extends BaseAdapter
{
    public MessageAdapter(Context context, List<? extends Room.Message> messages)
    {
        this.context = context;
        this.messages.addAll(messages);
    }

    public void insertHead(Collection<? extends Room.Message> messages)
    {
        this.messages.addAll(messages);
    }

    public void add(Room.Message e)
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
    public Room.Message getItem(int position)
    {
        return Iterables.get(this.messages, position);
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

        final Room.Message data= this.getItem(position);
        if(Objects.equal(view.getTag(), data.getId()))
        {
            return view;
        }
        view.setTag(data.getId());
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

        final AppContext appContext= (AppContext)this.context.getApplicationContext();
        appContext.getInlineImageMode().doWork(textView);

        return view;
    }

    private final Context context;

    private final SortedSet<Room.Message> messages= Sets.newTreeSet(new Comparator<Room.Message>(){
        @Override
        public int compare(Room.Message lhs, Room.Message rhs)
        {
            return lhs.getId().compareTo(rhs.getId());
        }
    });
}

