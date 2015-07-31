package akechi.projectl.authenticator;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;

public class LingrAuthenticatorService
    extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        return new LingrAuthenticator(this).getIBinder();
    }
}
