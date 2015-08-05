package akechi.projectl.authenticator;

import akechi.projectl.HomeActivity;
import akechi.projectl.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.common.base.Strings;

import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;
import jp.michikusa.chitose.lingr.Session;

public class LingrAuthenticatorActivity
    extends AccountAuthenticatorActivity
    implements Button.OnClickListener
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
    }

    @Override
    public void onClick(View v)
    {
        final String name= this.userIdText.getText().toString();
        final String password= this.passwordText.getText().toString();
        final String apiKey= this.apiKeyText.getText().toString();
        final LingrAuthenticatorActivity that= this;
        new AsyncTask<Void, Void, String>(){
            @Override
            public String doInBackground(Void... params)
            {
                try
                {
                    final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());
                    final LingrClient lingr= lingrFactory.newLingrClient();
                    if(Strings.isNullOrEmpty(apiKey))
                    {
                        return lingr.createSession(name, password);
                    }
                    else
                    {
                        return lingr.createSession(name, password, apiKey);
                    }
                }
                catch(Exception e)
                {
                    Log.e("LingrAuthActivity", "Couldn't create session", e);
                    return null;
                }
            }

            @Override
            public void onPostExecute(String authToken)
            {
                if(Strings.isNullOrEmpty(authToken))
                {
                    Toast.makeText(that, "Something wrong?", Toast.LENGTH_SHORT).show();
                    return;
                }

                final Account account= new Account(name, "com.lingr");
                final Bundle data= new Bundle();
                data.putString("apiKey", apiKey);

                final AccountManager manager= AccountManager.get(that);
                manager.addAccountExplicitly(account, password, data);
                manager.setAuthToken(account, "", authToken);
                Toast.makeText(that, "Account successfuly added", Toast.LENGTH_SHORT).show();

                {
                    final Intent intent= new Intent(that, HomeActivity.class);
                    that.startActivity(intent);
                }

                that.finish();
            }
        }.execute();
    }

    private EditText userIdText;
    private EditText passwordText;
    private EditText apiKeyText;
}
