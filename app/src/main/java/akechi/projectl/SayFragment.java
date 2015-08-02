package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;

import java.io.IOException;

import jp.michikusa.chitose.lingr.LingrClient;
import jp.michikusa.chitose.lingr.LingrClientFactory;

public class SayFragment
    extends Fragment
    implements Button.OnClickListener, RoomListFragment.OnRoomSelectedListener
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View v= inflater.inflate(R.layout.fragment_say, container, false);

        this.inputText= (EditText) v.findViewById(R.id.messageView);
        this.sayButton= (Button) v.findViewById(R.id.sayButton);

        this.sayButton.setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View v)
    {
        final SayFragment that= this;
        final AccountManager manager= AccountManager.get(this.getActivity());
        new AsyncTask<String, Void, Boolean>(){
            @Override
            protected Boolean doInBackground(String... params) {
                final String text= params[0];
                final String roomId= that.roomId;
                final Account[] accounts= manager.getAccountsByType("com.lingr");
                if(accounts.length <= 0)
                {
                    this.message= "First, you have to create lingr account";
                    return Boolean.FALSE;
                }
                final Account account= accounts[0];
                try
                {
                    final LingrClient lingr= lingrFactory.newLingrClient();
                    String authToken= manager.blockingGetAuthToken(account, "", true);
                    if(!lingr.verifySession(authToken))
                    {
                        manager.invalidateAuthToken("com.lingr", authToken);
                        authToken= manager.blockingGetAuthToken(account, "", true);
                    }
                    lingr.say(authToken, roomId, text);
                    return true;
                }
                catch (Exception e)
                {
                    Log.e("SayFragment", "Couldn't say message to " + roomId, e);
                    this.message= e.toString();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if(this.message != null)
                {
                    Toast.makeText(that.getActivity(), this.message, Toast.LENGTH_SHORT).show();
                }
                if(Boolean.TRUE.equals(success))
                {
                    that.inputText.setText("");
                    // close keypad
                    final InputMethodManager imeManager= (InputMethodManager)that.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imeManager.hideSoftInputFromWindow(that.inputText.getWindowToken(), 0);
                    Toast.makeText(that.getActivity(), "Posted", Toast.LENGTH_SHORT).show();
                }
            }

            private String message;
        }.execute(this.inputText.getText().toString());
    }

    @Override
    public void onRoomSelected(CharSequence roomId)
    {
        this.roomId= roomId.toString();
    }

    private static final LingrClientFactory lingrFactory= LingrClientFactory.newLingrClientFactory(AndroidHttp.newCompatibleTransport());

    private EditText inputText;
    private Button sayButton;

    private String roomId;
}
