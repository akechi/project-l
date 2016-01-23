package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.Selection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

import akechi.projectl.async.LingrTaskLoader;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;
import jp.michikusa.chitose.lingr.LingrException;
import jp.michikusa.chitose.lingr.Room;

public class SayFragment
    extends Fragment
    implements Button.OnClickListener, LoaderManager.LoaderCallbacks<Room.Message>, View.OnKeyListener
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View v= inflater.inflate(R.layout.fragment_say, container, false);

        this.inputText= (EditText) v.findViewById(R.id.messageView);
        this.sayButton= (Button) v.findViewById(R.id.sayButton);

        this.sayButton.setOnClickListener(this);
        this.inputText.setOnKeyListener(this);

        this.getLoaderManager().initLoader(LOADER_SAY_FROM_WIDGET, null, this);
        this.getLoaderManager().initLoader(LOADER_SAY_FROM_TEXT, null, this);

        // handle implicit intent
        {
            final Intent intent= this.getActivity().getIntent();
            final String text= intent.getStringExtra(Intent.EXTRA_TEXT);
            if(!Strings.isNullOrEmpty(text))
            {
                this.inputText.getText().append(text);
            }
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext());
        {
            final IntentFilter ifilter= new IntentFilter("akechi.projectl.ReplyAction");
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final String replyText= intent.getStringExtra("text");
                    SayFragment.this.reply(replyText);
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

                    SayFragment.this.onRoomSelected(roomId);
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
        {
            final IntentFilter ifilter= new IntentFilter(Event.PostMessage.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final String text= intent.getStringExtra(Event.PostMessage.KEY_TEXT);

                    SayFragment.this.say(text);
                }
            };
            lbMan.registerReceiver(receiver, ifilter);
            this.receivers.add(receiver);
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext());
        for(final BroadcastReceiver receiver : this.receivers)
        {
            lbMan.unregisterReceiver(receiver);
        }
        this.receivers.clear();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_LEFT:{
                // avoid left key to swipe fragment
                final int pos= Selection.getSelectionStart(this.inputText.getText());
                if(pos <= 0)
                {
                    return true;
                }
                break;
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT:{
                // avoid right key to swipe fragment
                final int pos= Selection.getSelectionEnd(this.inputText.getText());
                final int length= this.inputText.getText().length();
                if(pos >= length)
                {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void reply(String text)
    {
        if(Strings.isNullOrEmpty(text))
        {
            return;
        }
        final Iterable<String> lines= Splitter.on(Pattern.compile("\\r?\\n")).split(text);
        for(final String line : lines)
        {
            this.inputText.getText().append("> " + line + System.getProperty("line.separator"));
        }
    }

    public void say(String text)
    {
        if(text == null)
        {
            return;
        }

        this.texts.add(text);
        this.getLoaderManager().getLoader(LOADER_SAY_FROM_TEXT).forceLoad();
    }

    @Override
    public void onClick(View v)
    {
        Log.i("SayFragment", "input area and say button are disabled");
        this.sayButton.setEnabled(false);
        this.inputText.setEnabled(false);
        this.getLoaderManager().getLoader(LOADER_SAY_FROM_WIDGET).forceLoad();
    }

    private void onRoomSelected(CharSequence roomId)
    {
        Log.i("SayFragment", "input area and say button are enabled");
        this.sayButton.setEnabled(true);
        this.inputText.setEnabled(true);
    }

    @Override
    public Loader<Room.Message> onCreateLoader(int id, Bundle args)
    {
        switch(id)
        {
            case LOADER_SAY_FROM_WIDGET:
                return new MessagePostLoader(this.getActivity(), new Supplier<String>(){
                    @Override
                    public String get()
                    {
                        return inputText.getText().toString();
                    }
                });
            case LOADER_SAY_FROM_TEXT:
                return new MessagePostLoader(this.getActivity(), new Supplier<String>(){
                    @Override
                    public String get()
                    {
                        return texts.poll();
                    }
                });
            default:
                throw new AssertionError("Unknown loader id " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Room.Message> loader, Room.Message data)
    {
        // TODO: graceful implementations
        if(loader.getId() == LOADER_SAY_FROM_TEXT)
        {
            Toast.makeText(this.getActivity(), "Posted", Toast.LENGTH_SHORT).show();
            return;
        }

        try
        {
            if(data == null)
            {
                return;
            }
            this.inputText.setText("");
            // close keypad
            final InputMethodManager imeManager= (InputMethodManager)this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imeManager.hideSoftInputFromWindow(this.inputText.getWindowToken(), 0);
            Toast.makeText(this.getActivity(), "Posted", Toast.LENGTH_SHORT).show();
        }
        finally
        {
            Log.i("SayFragment", "input area and say button are enabled");
            this.sayButton.setEnabled(true);
            this.inputText.setEnabled(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Room.Message> loader)
    {
    }

    private static final class MessagePostLoader
        extends LingrTaskLoader<Room.Message>
    {
        public MessagePostLoader(Context context, Supplier<String> textSupplier)
        {
            super(context);
            this.textSupplier= textSupplier;
        }

        @Override
        public Room.Message loadInBackground(CharSequence authToken, LingrClient lingr)
            throws IOException, LingrException
        {
            final String text= this.textSupplier.get();
            Log.i("SayFragment", "loadInBackground(), text = " + text);
            if(Strings.isNullOrEmpty(text))
            {
                return null;
            }
            final AppContext appContext= this.getApplicationContext();
            final Account account= appContext.getAccount();
            final String roomId= appContext.getRoomId(account);

            return lingr.say(authToken, roomId, text);
        }

        private final Supplier<String> textSupplier;
    }

    private static final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());
    private static final int LOADER_SAY_FROM_WIDGET= 0;
    private static final int LOADER_SAY_FROM_TEXT= 1;

    private EditText inputText;
    private Button sayButton;
    // for LOADER_SAY_FROM_TEXT
    private Queue<String> texts= Queues.newArrayDeque();

    private List<BroadcastReceiver> receivers= Lists.newLinkedList();
}
