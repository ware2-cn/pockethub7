package com.github.mobile.ui;

import static com.github.mobile.ui.NavigationDrawerObject.TYPE_SEPERATOR;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.github.mobile.R;
import com.github.mobile.accounts.AccountUtils;
import com.github.mobile.core.user.UserComparator;
import com.github.mobile.persistence.AccountDataManager;
import com.github.mobile.ui.gist.GistsPagerFragment;
import com.github.mobile.ui.issue.FiltersViewFragment;
import com.github.mobile.ui.issue.IssueDashboardPagerFragment;
import com.github.mobile.ui.repo.OrganizationLoader;
import com.github.mobile.ui.user.HomePagerFragment;
import com.github.mobile.util.AvatarLoader;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.User;

public class MainActivity extends BaseActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
    LoaderManager.LoaderCallbacks<List<User>> {

    private static final String TAG = "MainActivity";

    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Inject
    private AccountDataManager accountDataManager;

    @Inject
    private Provider<UserComparator> userComparatorProvider;

    private List<User> orgs = Collections.emptyList();

    private NavigationDrawerAdapter navigationAdapter;

    private User org;

    @Inject
    private AvatarLoader avatars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportLoaderManager().initLoader(0, null, this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
            getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
    }

    private void reloadOrgs() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu optionMenu) {
        getMenuInflater().inflate(R.menu.home, optionMenu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = optionMenu.findItem(R.id.m_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return super.onCreateOptionsMenu(optionMenu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restart loader if default account doesn't match currently loaded
        // account
        List<User> currentOrgs = orgs;
        if (currentOrgs != null && !currentOrgs.isEmpty()
            && !AccountUtils.isUser(this, currentOrgs.get(0)))
            reloadOrgs();
    }

    @Override
    public Loader<List<User>> onCreateLoader(int i, Bundle bundle) {
        return new OrganizationLoader(this, accountDataManager,
            userComparatorProvider);
    }

    @Override
    public void onLoadFinished(Loader<List<User>> listLoader, final List<User> orgs) {
        org = orgs.get(0);
        this.orgs = orgs;

        if (navigationAdapter != null)
            navigationAdapter.setOrgs(orgs);
        else {
            navigationAdapter = new NavigationDrawerAdapter(MainActivity.this, orgs, avatars);
            mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout), navigationAdapter, avatars, org);

            Window window = getWindow();
            if (window == null)
                return;
            View view = window.getDecorView();
            if (view == null)
                return;

            view.post(new Runnable() {

                @Override
                public void run() {
                    MainActivity.this.onNavigationDrawerItemSelected(0);
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<List<User>> listLoader) {

    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (navigationAdapter.getItem(position).getType() == TYPE_SEPERATOR)
            return;
        Fragment fragmet;
        Bundle args = new Bundle();
        switch (position) {
            case 0:
                fragmet = new HomePagerFragment();
                args.putSerializable("org", org);
                break;
            case 1:
                fragmet = new GistsPagerFragment();
                break;
            case 2:
                fragmet = new IssueDashboardPagerFragment();
                break;
            case 3:
                fragmet = new FiltersViewFragment();
                break;
            default:
                fragmet = new HomePagerFragment();
                args.putSerializable("org", orgs.get(position-5));
                break;
        }
        fragmet.setArguments(args);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.container,fragmet).commit();
    }

}
