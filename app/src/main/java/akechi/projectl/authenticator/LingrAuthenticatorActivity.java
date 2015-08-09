package akechi.projectl.authenticator;

import akechi.projectl.AppContext;
import akechi.projectl.HomeActivity;
import akechi.projectl.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import akechi.projectl.async.LingrTaskLoader;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;
import jp.michikusa.chitose.lingr.LingrException;
import jp.michikusa.chitose.lingr.Session;

public class LingrAuthenticatorActivity
    extends AccountAuthenticatorActivity
    implements Button.OnClickListener, LoaderManager.LoaderCallbacks<Pair<String, Exception>>
{
    static final String ARG_ACCOUNT_TYPE = "accountType";
    static final String ARG_AUTH_TYPE = "authType";
    static final String ARG_IS_ADDING_NEW_ACCOUNT = "isAddingNewAccount";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_lingr_authenticator);

        this.findViewById(R.id.saveButton).setOnClickListener(this);
        this.userIdText= (EditText)this.findViewById(R.id.userIdText);
        this.passwordText= (EditText)this.findViewById(R.id.passwordText);
        this.apiKeyText= (EditText)this.findViewById(R.id.apiKeyText);

        this.getLoaderManager().initLoader(LOADER_LOGIN, null, this);
    }

    @Override
    public void onClick(View v)
    {
        this.findViewById(R.id.saveButton).setEnabled(false);
        this.getLoaderManager().getLoader(LOADER_LOGIN).forceLoad();
    }

    @Override
    public void onLoaderReset(Loader<Pair<String, Exception>> loader)
    {
    }

    @Override
    public Loader<Pair<String, Exception>> onCreateLoader(int id, Bundle args)
    {
        switch(id)
        {
            case LOADER_LOGIN:{
                return new LoginLoader(
                    this,
                    new Supplier<String>(){
                        @Override
                        public String get()
                        {
                            return LingrAuthenticatorActivity.this.userIdText.getText().toString();
                        }
                    },
                    new Supplier<String>(){
                        @Override
                        public String get()
                        {
                            return LingrAuthenticatorActivity.this.passwordText.getText().toString();
                        }
                    },
                    new Supplier<String>(){
                        @Override
                        public String get()
                        {
                            return LingrAuthenticatorActivity.this.apiKeyText.getText().toString();
                        }
                    }
                );
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Pair<String, Exception>> loader, Pair<String, Exception> data)
    {
        this.findViewById(R.id.saveButton).setEnabled(true);
        if(!Strings.isNullOrEmpty(data.first))
        {
            final Account account= new Account(this.userIdText.getText().toString(), "com.lingr");
            final Bundle bundle= new Bundle();
            bundle.putString("apiKey", this.apiKeyText.getText().toString());

            final AccountManager manager= AccountManager.get(this);
            manager.addAccountExplicitly(account, this.passwordText.getText().toString(), bundle);
            manager.setAuthToken(account, "", data.first);

            Toast.makeText(this, "Account successfuly added", Toast.LENGTH_SHORT).show();

            // ProjectL will be started
            {
                final Intent intent= new Intent(this, HomeActivity.class);
                this.startActivity(intent);
            }

            this.finish();
        }
        else if(data.second != null)
        {
            Log.e("LingrAuthActivity", "Login failed", data.second);
            Toast.makeText(this, "Sorry, detect an error. Retry, please...", Toast.LENGTH_SHORT).show();
        }
    }

    private static final class LoginLoader
        extends AsyncTaskLoader<Pair<String, Exception>>
    {
        public LoginLoader(Context context, Supplier<String> userIdSupplier, Supplier<String> passwordSupplier, Supplier<String> apiKeySupplier)
        {
            super(context);

            this.userIdSupplier= userIdSupplier;
            this.passworSupplier= passwordSupplier;
            this.apiKeySupplier= apiKeySupplier;
        }

        @Override
        public Pair<String, Exception> loadInBackground()
        {
            try
            {
                int nretries= 0;
                while(nretries < MAX_NRETRIES)
                {
                    try
                    {
                        final AppContext appContext= (AppContext)this.getContext();
                        final AccountManager manager= AccountManager.get(this.getContext());
                        final LingrClient lingr= appContext.getLingrClient();
                        final String userId= this.userIdSupplier.get();
                        final String password= this.passworSupplier.get();
                        final String apiKey= this.apiKeySupplier.get();

                        final String authToken;
                        if(Strings.isNullOrEmpty(apiKey))
                        {
                            authToken= lingr.createSession(userId, password);
                        }
                        else
                        {
                            authToken= lingr.createSession(userId, password, apiKey);
                        }
                        return Pair.create(authToken, null);
                    }
                    catch(EOFException e)
                    {
                        // android's bug, ignore and retry
                        final long waitTimeMillis= Math.min(this.getWaitTimeMillis(nretries++), MAX_WAIT_TIME_MILLIS);
                        Thread.sleep(waitTimeMillis);
                    }
                }
                return Pair.create(null, null);
            }
            catch(Exception e)
            {
                return Pair.create(null, e);
            }
        }

        private long getWaitTimeMillis(int nretries)
        {
            return (long)Math.pow(2, nretries) * 500L;
        }

        private static final int MAX_NRETRIES= 5;
        private static final long MAX_WAIT_TIME_MILLIS= TimeUnit.SECONDS.toMillis(10);

        private final Supplier<String> userIdSupplier;
        private final Supplier<String> passworSupplier;
        private final Supplier<String> apiKeySupplier;
    }

    private static final int LOADER_LOGIN= 0;

    private EditText userIdText;
    private EditText passwordText;
    private EditText apiKeyText;
}
