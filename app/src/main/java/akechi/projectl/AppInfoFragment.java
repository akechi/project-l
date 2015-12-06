package akechi.projectl;

import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class AppInfoFragment
    extends DialogFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View v= inflater.inflate(R.layout.fragment_app_info, container, false);

        try
        {
            final TextView textView= (TextView)v.findViewById(R.id.appVersionView);
            final PackageInfo info= this.getActivity().getPackageManager().getPackageInfo(this.getActivity().getPackageName(), 0);
            textView.setText(info.versionName);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new AssertionError(e);
        }
        {
            final TextView textView= (TextView)v.findViewById(R.id.issuesUrlView);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(Html.fromHtml("<a href=\"https://github.com/akechi/project-l/issues\">here</a>"));
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        final Dialog dialog= this.getDialog();
        final WindowManager.LayoutParams lp= dialog.getWindow().getAttributes();
        final DisplayMetrics dm= this.getResources().getDisplayMetrics();

        lp.width= (int)(dm.widthPixels * 0.8);

        dialog.getWindow().setAttributes(lp);
    }
}
