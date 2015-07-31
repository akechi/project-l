package akechi.projectl.authenticator;

import akechi.projectl.R;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.view.View;
import android.widget.EditText;

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

        final Account account= new Account(name, "com.lingr");
        final Bundle data= new Bundle();
        data.putString("apiKey", apiKey);

        AccountManager.get(this).addAccountExplicitly(account, password, data);

        this.finish();
    }

    private EditText userIdText;
    private EditText passwordText;
    private EditText apiKeyText;
}
