package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.text.style.URLSpan;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import akechi.projectl.async.InlineImageHandler;
import akechi.projectl.component.CachedImageView;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class AppContext
    extends Application
{
    public static enum ActionBarMode
    {
        DEFAULT
        {
            @Override
            public void applyActionBar(Context ctx, ActionBar bar)
            {
                bar.setDisplayShowHomeEnabled(true);
                bar.setDisplayShowCustomEnabled(true);
                bar.setDisplayUseLogoEnabled(true);
                bar.setLogo(R.drawable.icon_logo);
                bar.setIcon(R.drawable.icon_logo);
            }
        },
        CURRENT_ROOM
        {
            @Override
            public void applyActionBar(Context ctx, ActionBar bar)
            {
                bar.setDisplayShowHomeEnabled(true);
                bar.setDisplayShowCustomEnabled(true);
                bar.setDisplayUseLogoEnabled(true);
                bar.setLogo(R.drawable.icon_logo);
                bar.setIcon(R.drawable.icon_logo);

                final AppContext appContext= (AppContext)ctx.getApplicationContext();
                final Account account= appContext.getAccount();
                if(Iterables.size(appContext.getAccounts()) <= 1)
                {
                    bar.setTitle(appContext.getRoomId(account));
                }
                else
                {
                    bar.setTitle(String.format("%s / %s", appContext.getRoomId(account), account.name));
                }
            }
        },
        HIDDEN
        {
            @Override
            public void applyActionBar(Context ctx, ActionBar bar)
            {
                bar.hide();
            }
        },
        ;

        public abstract void applyActionBar(Context ctx, ActionBar bar);
    }

    public static enum InlineImageMode
    {
        ALWAYS
        {
            @Override
            public void doWork(TextView view)
            {
                new InlineImageHandler(view);
            }
        },
        WIFI_ONLY
        {
            @Override
            public void doWork(TextView view)
            {
                final ConnectivityManager connMan= (ConnectivityManager)view.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo netInfo= connMan.getActiveNetworkInfo();
                if(netInfo == null)
                {
                    return;
                }
                if((netInfo.getType() & ConnectivityManager.TYPE_WIFI) == ConnectivityManager.TYPE_WIFI)
                {
                    ALWAYS.doWork(view);
                }
            }
        },
        NEVER
        {
            @Override
            public void doWork(TextView view)
            {
                // do nothing
            }
        },
        ;

        public abstract void doWork(TextView view);
    }

    public Iterable<Account> getAccounts()
    {
        final AccountManager manager= AccountManager.get(this);
        final Account[] accounts= manager.getAccountsByType("com.lingr");
        return Arrays.asList(accounts);
    }

    public Account getAccount()
    {
        final String name= this.accountName;
        final Iterable<Account> accounts= this.getAccounts();
        if(Iterables.isEmpty(accounts))
        {
            return null;
        }

        final Optional<Account> account= Iterables.tryFind(accounts, new Predicate<Account>() {
            @Override
            public boolean apply(Account input) {
                return input.name.equals(name);
            }
        });
        if(account.isPresent())
        {
            return account.get();
        }
        return Iterables.getFirst(accounts, null);
    }

    public void setAccount(Account account)
    {
        checkNotNull(account, "account is null");

        this.accountName= account.name;
    }

    public String getRoomId(Account account)
    {
        if(account == null)
        {
            return null;
        }
        final AccountManager manager= AccountManager.get(this);
        return manager.getUserData(account, "roomId");
    }

    public void setRoomId(Account account, CharSequence roomId)
    {
        checkNotNull(account, "account is null");

        final AccountManager manager= AccountManager.get(this);
        manager.setUserData(account, "roomId", roomId.toString());
    }

    public Iterable<String> getRoomIds(Account account)
    {
        checkNotNull(account);

        final AccountManager manager= AccountManager.get(this);
        final String ids= manager.getUserData(account, "roomIdList");

        if(Strings.isNullOrEmpty(ids))
        {
            return Collections.emptyList();
        }
        return Splitter.on(',').split(ids);
    }

    public void setRoomIds(Account account, Iterable<? extends CharSequence> roomIds)
    {
        checkNotNull(account);

        final Iterable<String> ids= Iterables.transform(roomIds, Functions.toStringFunction());
        final String value= Joiner.on(',').join(ids);

        final AccountManager manager= AccountManager.get(this);
        manager.setUserData(account, "roomIdList", value);
    }

    public boolean isIconCacheEnabled()
    {
        final Boolean oldVar= this.iconCacheEnabled;
        if(oldVar != null)
        {
            return oldVar;
        }
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        final boolean value= prefs.getBoolean("iconCacheEnabled", true);
        this.iconCacheEnabled= value;
        return value;
    }

    public void setIconCacheEnabled(boolean value)
    {
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean("iconCacheEnabled", value)
            .commit()
        ;
        this.iconCacheEnabled= value;
    }

    public File getIconCacheDir()
    {
        return new File(this.getCacheDir(), "icons");
    }

    public ActionBarMode getActionBarMode()
    {
        final ActionBarMode oldVar= this.actionBarMode;
        if(oldVar != null)
        {
            return oldVar;
        }
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        final ActionBarMode value= ActionBarMode.valueOf(prefs.getString("actionBarMode", ActionBarMode.DEFAULT.name()));
        this.actionBarMode= value;
        return value;
    }

    public void setActionBarMode(ActionBarMode value)
    {
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putString("actionBarMode", value.name())
            .commit()
        ;
        this.actionBarMode= value;
    }

    public InlineImageMode getInlineImageMode()
    {
        final InlineImageMode oldVar= this.inlineImageMode;
        if(oldVar != null)
        {
            return oldVar;
        }
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        final InlineImageMode value= InlineImageMode.valueOf(prefs.getString("inlineImageMode", InlineImageMode.NEVER.name()));
        this.inlineImageMode= value;
        return value;
    }

    public void setInlineImageMode(InlineImageMode value)
    {
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putString("inlineImageMode", value.name())
            .commit()
        ;
        this.inlineImageMode= value;
    }
    public boolean isBackgroundServiceEnabled()
    {
        final Boolean oldVar= this.backgroundServiceEnabled;
        if(oldVar != null)
        {
            return oldVar;
        }
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        final boolean value= prefs.getBoolean("backgroundServiceEnabled", true);
        this.backgroundServiceEnabled= value;
        return value;
    }

    public void setBackgroundServiceEnabled(boolean value)
    {
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean("backgroundServiceEnabled", value)
            .commit()
        ;
        this.backgroundServiceEnabled= value;
    }

    public String getHighlightPattern()
    {
        final String oldVar= this.highlightPattern;
        if(oldVar != null)
        {
            return oldVar;
        }
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        final String value= prefs.getString("highlightPattern", "");
        this.highlightPattern= value;
        return Strings.nullToEmpty(value);
    }

    public void setHighlightPattern(CharSequence value)
    {
        String sval= (value != null)
            ? value.toString()
            : "";
        final SharedPreferences prefs= this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putString("highlightPattern", sval)
            .commit()
        ;
        this.highlightPattern= sval;
    }

    public LingrClient getLingrClient()
    {
        return lingrFactory.newLingrClient();
    }

    private static final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());

    private String accountName;

    private Boolean iconCacheEnabled;

    private ActionBarMode actionBarMode;

    private InlineImageMode inlineImageMode;

    private Boolean backgroundServiceEnabled;

    private String highlightPattern;
}
