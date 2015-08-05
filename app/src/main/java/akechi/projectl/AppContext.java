package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;

import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class AppContext
    extends Application
{
    public Account getAccount()
    {
        final String name= this.accountName;
        final boolean autoselect= Strings.isNullOrEmpty(name);

        final AccountManager manager= AccountManager.get(this);
        final Account[] accounts= manager.getAccountsByType("com.lingr");
        if(accounts.length <= 0)
        {
            Log.i("AppContext", "no accounts");
            return null;
        }
        if(autoselect && accounts.length == 1)
        {
            return accounts[0];
        }

        for(final Account account : accounts)
        {
            if(account.name.equals(name))
            {
                Log.i("AppContext", "Found account " + name);
                return account;
            }
        }
        Log.i("AppContext", "Couldn't find account name " + name);
        return null;
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

    public LingrClient getLingrClient()
    {
        return lingrFactory.newLingrClient();
    }

    private static final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());

    private String accountName;
}
