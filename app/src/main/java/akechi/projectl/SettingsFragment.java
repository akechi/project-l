package akechi.projectl;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SettingsFragment
    extends Fragment
    implements View.OnClickListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext());
        {
            final IntentFilter ifilter= new IntentFilter(Event.AccountChange.ACTION);
            final BroadcastReceiver receiver= new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    SettingsFragment.this.loadSettings();
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
        final View v= inflater.inflate(R.layout.fragment_settings, container, false);

        this.actionBarMode= (RadioGroup)v.findViewById(R.id.actionBarModeRadioGroup);
        this.iconCacheEnabledView= (CheckBox)v.findViewById(R.id.iconCacheCheck);
        this.inlineImageMode= (RadioGroup)v.findViewById(R.id.inlineImageModeRadioGroup);
        this.backgroundServiceEnabledView= (CheckBox)v.findViewById(R.id.backgroundServiceEnabledCheck);
        this.highlightPatternView= (EditText)v.findViewById(R.id.highlightPatternText);
        this.roomIdView= (EditText)v.findViewById(R.id.roomIdText);
        this.saveButton= (Button)v.findViewById(R.id.saveButton);
        this.saveButton.setOnClickListener(this);

        this.loadSettings();

        {
            v.findViewById(R.id.clearCacheButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AppContext appContext = (AppContext) SettingsFragment.this.getActivity().getApplicationContext();
                    final File iconCacheDir = appContext.getIconCacheDir();
                    if (!iconCacheDir.isDirectory()) {
                        Toast.makeText(appContext, "Cleared", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.i("SettingsFragment", "clear dir " + iconCacheDir.getAbsolutePath());
                    if (this.deleteRecursive(iconCacheDir)) {
                        Toast.makeText(appContext, "Cleared", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(appContext, "Didn't clear", Toast.LENGTH_SHORT).show();
                    }
                }

                private boolean deleteRecursive(File file) {
                    if (file.isDirectory()) {
                        final File[] children = file.listFiles();
                        if (children == null) {
                            return false;
                        }
                        for (final File child : children) {
                            if (!this.deleteRecursive(child)) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return file.delete();
                    }
                }
            });
        }
        {
            this.backgroundServiceEnabledView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SettingsFragment.this.highlightPatternView.setEnabled(isChecked);
                }
            });
        }

        return v;
    }

    @Override
    public void onClick(View v)
    {
        // validate
        {
            boolean hasError= false;
            try
            {
                final String patterns= this.highlightPatternView.getText().toString();
                for(final String pattern : Splitter.on(System.getProperty("line.separator")).split(patterns))
                {
                    Pattern.compile(pattern);
                }
                this.highlightPatternView.setError(null);
            }
            catch(PatternSyntaxException e)
            {
                this.highlightPatternView.setError("Invalid regular expression");
                hasError= true;
            }

            if(hasError)
            {
                return;
            }
        }

        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        // general settings
        {
            final int modeId= this.actionBarMode.getCheckedRadioButtonId();
            switch(modeId)
            {
                case R.id.modeDefaultRadio:{
                    appContext.setActionBarMode(AppContext.ActionBarMode.DEFAULT);
                    break;
                }
                case R.id.modeCurrentRoomRadio:{
                    appContext.setActionBarMode(AppContext.ActionBarMode.CURRENT_ROOM);
                    break;
                }
                case R.id.modeHiddenRadio:{
                    appContext.setActionBarMode(AppContext.ActionBarMode.HIDDEN);
                    break;
                }
            }
        }
        {
            appContext.setIconCacheEnabled(this.iconCacheEnabledView.isChecked());
        }
        {
            final int modeId= this.inlineImageMode.getCheckedRadioButtonId();
            switch(modeId)
            {
                case R.id.inlineImageAlwaysRadio:{
                    appContext.setInlineImageMode(AppContext.InlineImageMode.ALWAYS);
                    break;
                }
                case R.id.inlineImageWifiOnlyRadio:{
                    appContext.setInlineImageMode(AppContext.InlineImageMode.WIFI_ONLY);
                    break;
                }
                case R.id.inlineImageNeverRadio:{
                    appContext.setInlineImageMode(AppContext.InlineImageMode.NEVER);
                    break;
                }
            }
        }
        {
            appContext.setBackgroundServiceEnabled(this.backgroundServiceEnabledView.isChecked());
        }
        {
            appContext.setHighlightPattern(this.highlightPatternView.getText());
        }
        // account settings
        final Account account= appContext.getAccount();
        {
            final String ids= this.roomIdView.getText().toString();
            final Iterable<String> roomIds= Iterables.filter(
                    Splitter.on(System.getProperty("line.separator")).split(ids),
                    new Predicate<String>() {
                        @Override
                        public boolean apply(String input) {
                            return !Strings.isNullOrEmpty(input);
                        }
                    }
            );
            appContext.setRoomIds(account, roomIds);
        }

        Toast.makeText(this.getActivity(), "Saved", Toast.LENGTH_SHORT).show();

        // notify others for preference changed
        {
            final LocalBroadcastManager lbMan= LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext());
            final Intent intent= new Intent(Event.PreferenceChange.ACTION);
            lbMan.sendBroadcast(intent);
        }
    }

    private void loadSettings()
    {
        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        // general settings
        {
            final AppContext.ActionBarMode mode= appContext.getActionBarMode();
            switch(mode)
            {
                case DEFAULT:{
                    this.actionBarMode.check(R.id.modeDefaultRadio);
                    break;
                }
                case CURRENT_ROOM:{
                    this.actionBarMode.check(R.id.modeCurrentRoomRadio);
                    break;
                }
                case HIDDEN:{
                    this.actionBarMode.check(R.id.modeHiddenRadio);
                    break;
                }
                default:
                    throw new AssertionError("Unknown ActionBarMode " + mode);
            }
        }
        this.iconCacheEnabledView.setChecked(appContext.isIconCacheEnabled());
        {
            final AppContext.InlineImageMode mode= appContext.getInlineImageMode();
            switch(mode)
            {
                case ALWAYS:{
                    this.inlineImageMode.check(R.id.inlineImageAlwaysRadio);
                    break;
                }
                case WIFI_ONLY:{
                    this.inlineImageMode.check(R.id.inlineImageWifiOnlyRadio);
                    break;
                }
                case NEVER:{
                    this.inlineImageMode.check(R.id.inlineImageNeverRadio);
                    break;
                }
                default:
                    throw new AssertionError("Unknown InlineImageMode " + mode);
            }
        }
        this.backgroundServiceEnabledView.setChecked(appContext.isBackgroundServiceEnabled());
        this.highlightPatternView.setText(appContext.getHighlightPattern());
        this.highlightPatternView.setEnabled(appContext.isBackgroundServiceEnabled());
        // account settings
        final Account account= appContext.getAccount();
        if(account != null)
        {
            final Iterable<String> roomIds= appContext.getRoomIds(account);
            this.roomIdView.setText(Joiner.on(System.getProperty("line.separator")).join(roomIds));
        }
    }

    private RadioGroup actionBarMode;
    private CheckBox iconCacheEnabledView;
    private RadioGroup inlineImageMode;
    private CheckBox backgroundServiceEnabledView;
    private EditText highlightPatternView;
    private EditText roomIdView;
    private Button saveButton;

    private final List<BroadcastReceiver> receivers= Lists.newLinkedList();
}
