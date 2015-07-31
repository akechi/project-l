package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

public class HomeActivity
    extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);

        final AccountManager manager= AccountManager.get(this);
        final Account[] accounts= manager.getAccountsByType("com.lingr");

        try
        {
            final Bundle bundle= manager.getAuthToken(accounts[0], "", null, this, null, null).getResult();
            Toast.makeText(this, String.format("bundle=%s", bundle), Toast.LENGTH_LONG).show();
        }
        catch(Exception e)
        {
            Log.e("HomeActivity", "getAuthToken() failed", e);
            Toast.makeText(this, "Couldn't connect to lingr", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(akechi.projectl.R.menu.main, menu);
        return true;
    }
}

