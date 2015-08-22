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
import com.google.common.collect.Lists;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
        this.messages.addAll(0, messages);
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

        final Room.Message data= this.getItem(position);
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

    private final List<Room.Message> messages= Lists.newLinkedList();
}

