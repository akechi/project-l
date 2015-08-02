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

public class AppContext
    extends Application
{
    public Account getAccount()
    {
        final String name= this.accountName;
        if(Strings.isNullOrEmpty(name))
        {
            Log.i("AppContext", "account not yet selected");
            return null;
        }

        final AccountManager manager= AccountManager.get(this);
        final Account[] accounts= manager.getAccountsByType("com.lingr");
        if(accounts.length <= 0)
        {
            Log.i("AppContext", "no accounts");
            return null;
        }

        for(final Account account : accounts)
        {
            if(name.equals(account.name))
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
        Log.i("AppContext", "setAccount() with " + account.name);
        this.accountName= account.name;
    }

    public String getRoomId(Account account)
    {
        final AccountManager manager= AccountManager.get(this);
        return manager.getUserData(account, "roomId");
    }

    public void setRoomId(Account account, CharSequence roomId)
    {
        final AccountManager manager= AccountManager.get(this);
        manager.setUserData(account, "roomId", roomId.toString());
    }

    public LingrClient getLingrClient()
    {
        return lingrFactory.newLingrClient();
    }

    private static final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(new ApacheHttpTransport());

    private String accountName;
}
