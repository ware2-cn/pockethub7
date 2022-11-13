package com.github.mobile.android.repo;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.github.mobile.android.AccountDataManager;
import com.github.mobile.android.AsyncLoader;
import com.github.mobile.android.R.layout;
import com.github.mobile.android.ui.fragments.ListLoadingFragment;
import com.google.inject.Inject;
import com.madgag.android.listviews.ReflectiveHolderFactory;
import com.madgag.android.listviews.ViewHoldingListAdapter;
import com.madgag.android.listviews.ViewInflator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.User;

/**
 * Fragment to load a list of GitHub organizations
 */
public class OrgListFragment extends ListLoadingFragment<User> {

    private static final String TAG = "OLF";

    @Inject
    private AccountDataManager cache;

    public Loader<List<User>> onCreateLoader(int id, Bundle args) {
        return new AsyncLoader<List<User>>(getActivity()) {

            public List<User> loadInBackground() {
                try {
                    return cache.getOrgs();
                } catch (IOException e) {
                    Log.d(TAG, "Exception loading organizations", e);
                }
                return Collections.emptyList();
            }
        };
    }

    protected ListAdapter adapterFor(List<User> items) {
        return new ViewHoldingListAdapter<User>(items, ViewInflator.viewInflatorFor(getActivity(), layout.org_item),
                ReflectiveHolderFactory.reflectiveFactoryFor(OrgViewHolder.class, getActivity()));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        User user = (User) l.getItemAtPosition(position);
        startActivity(RepoBrowseActivity.createIntent(user));
    }
}
