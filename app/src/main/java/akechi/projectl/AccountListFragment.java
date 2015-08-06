package akechi.projectl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class AccountListFragment
    extends Fragment
    implements ListView.OnItemClickListener
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View v= inflater.inflate(R.layout.fragment_account_list, container, false);

        this.accountListView= (ListView)v.findViewById(R.id.accountListView);
        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        final Iterable<String> names= Iterables.transform(appContext.getAccounts(), new Function<Account, String>(){
            @Override
            public String apply(Account input)
            {
                return input.name;
            }
        });
        this.accountListView.setAdapter(new ArrayAdapter<String>(
            this.getActivity(),
            android.R.layout.simple_list_item_1,
            Iterables.toArray(names, String.class)
        ));
        this.accountListView.setOnItemClickListener(this);

        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        final String accountName= (String)this.accountListView.getAdapter().getItem(position);
        final AppContext appContext= (AppContext)this.getActivity().getApplicationContext();
        final Optional<Account> account= Iterables.tryFind(appContext.getAccounts(), new Predicate<Account>(){
            @Override
            public boolean apply(@Nullable Account input)
            {
                return accountName.equals(input.name);
            }
        });

        if(account.isPresent())
        {
            final Intent intent= new Intent(Event.AccountChange.ACTION);
            intent.putExtra(Event.AccountChange.KEY_ACCOUNT, account.get());
            this.getActivity().sendBroadcast(intent);
        }
        else
        {
            Toast.makeText(this.getActivity(), "Account information have modified, restart this app.", Toast.LENGTH_SHORT);
        }
    }

    private ListView accountListView;
}
