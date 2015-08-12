package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.apache.ApacheHttpTransport;
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

import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class AppContext
    extends Application
{
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

    public LingrClient getLingrClient()
    {
        return lingrFactory.newLingrClient();
    }

    private static final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());

    private String accountName;

    private Boolean iconCacheEnabled;
}
