package akechi.projectl.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LingrAuthenticatorService
    extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        return new LingrAuthenticator(this).getIBinder();
    }
}
