package akechi.projectl.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.common.base.Strings;

import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;
import jp.michikusa.chitose.lingr.Session;

public class LingrAuthenticator
    extends AbstractAccountAuthenticator
{
    public LingrAuthenticator(Context context)
    {
        super(context);

        this.context= context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
    {
        final Intent intent= new Intent(this.context, LingrAuthenticatorActivity.class);
        intent.putExtra(LingrAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(LingrAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(LingrAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle= new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
    {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
    {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType)
    {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
    {
        Log.i("LingrAuthenticator", "getAuthToken() with " + account.name);
        final AccountManager manager= AccountManager.get(this.context);
        String authToken= manager.peekAuthToken(account, authTokenType);
        if(Strings.isNullOrEmpty(authToken))
        {
            final String password= manager.getPassword(account);
            final String apiKey= manager.getUserData(account, "apiKey");
            final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());
            final LingrClient lingr= lingrFactory.newLingrClient();
            try
            {
                if(Strings.isNullOrEmpty(apiKey))
                {
                    authToken= lingr.createSession(account.name, password);
                }
                else
                {
                    authToken= lingr.createSession(account.name, password, apiKey);
                }
            }
            catch(Exception e)
            {
                Log.e("LingrAuthenticator", "Couldn't get authToken", e);
                final Bundle bundle= new Bundle();
                bundle.putInt(AccountManager.KEY_ERROR_CODE, 1);
                bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "Wrong userId or password, maybe Lingr is dead?");
                return bundle;
            }
        }
        if(Strings.isNullOrEmpty(authToken))
        {
            Log.i("LingrAuthenticator", "Edit account properties");
            // invalid password? try again
            final Intent intent= new Intent(this.context, LingrAuthenticatorActivity.class);
            intent.putExtra(LingrAuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
            intent.putExtra(LingrAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

            final Bundle bundle= new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        Log.i("LingrAuthenticator", "Got authToken " + authToken);
        final Bundle bundle= new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType)
    {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
    {
        return null;
    }

    private final Context context;
}
