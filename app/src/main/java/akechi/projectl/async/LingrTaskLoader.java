package akechi.projectl.async;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.widget.Toast;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import akechi.projectl.AppContext;
import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrException;

public abstract class LingrTaskLoader<R>
    extends AsyncTaskLoader<R>
{
    public LingrTaskLoader(Context context)
    {
        super(context);
    }

    @Override
    public final R loadInBackground()
    {
        try
        {
            final AppContext appContext= this.getApplicationContext();
            int nretries= 0;
            while(nretries < MAX_NRETRIES)
            {
                try
                {
                    final Account account= appContext.getAccount();
                    if(account == null)
                    {
                        this.showMessage("Choose account first");
                        return null;
                    }

                    final AccountManager manager= AccountManager.get(this.getContext());
                    String authToken= manager.blockingGetAuthToken(account, "", true);
                    final LingrClient lingr= appContext.getLingrClient();
                    if(!lingr.verifySession(authToken))
                    {
                        manager.invalidateAuthToken("com.lingr", authToken);
                        authToken= manager.blockingGetAuthToken(account, "", true);
                    }

                    return this.loadInBackground(authToken, lingr);
                }
                catch(EOFException e)
                {
                    // android's bug, ignore and retry
                    Thread.sleep(this.getWaitTimeMillis(nretries++));
                }
            }
            this.showMessage("Giving up");
            return null;
        }
        catch(Exception e)
        {
            Log.e("LingrTaskLoader", "Loading failed", e);
            this.showMessage("Oh, my god");
            return null;
        }
    }

    public abstract R loadInBackground(CharSequence authToken, LingrClient lingr)
        throws IOException, LingrException;

    protected AppContext getApplicationContext()
    {
        return (AppContext)this.getContext().getApplicationContext();
    }

    protected void showMessage(CharSequence message)
    {
        final Context ctx= this.getContext();
        final String text= message.toString();
        final Handler handler= new Handler(Looper.getMainLooper());
        handler.post(new Runnable(){
            @Override
            public void run()
            {
                Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private long getWaitTimeMillis(int nretries)
    {
        return (long)Math.pow(2, nretries) * 500L;
    }

    private static final int MAX_NRETRIES= 5;
    private static final long MAX_WAIT_TIME_MILLIS= TimeUnit.SECONDS.toMillis(10);
}
