package com.github.mobile.android.ui;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import com.github.mobile.android.ResourcePager;
import com.github.mobile.android.ThrowableLoader;

import java.io.IOException;
import java.util.List;

/**
 * List fragment that adds more elements when the bottom of the list is scrolled to
 *
 * @param <E>
 */
public abstract class PagedListFragment<E> extends ListLoadingFragment<E> implements OnScrollListener {

    /**
     * Resource pager
     */
    protected ResourcePager<E> pager;

    private ResourceLoadingIndicator loadingIndicator;

    /**
     * Create pager that provides resources
     *
     * @return pager
     */
    protected abstract ResourcePager<E> createPager();

    /**
     * Get resource id of {@link String} to display when loading
     *
     * @return string resource id
     */
    protected abstract int getLoadingMessage();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pager = createPager();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingIndicator = new ResourceLoadingIndicator(getActivity(), getLoadingMessage());
        loadingIndicator.setList(getListView());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnScrollListener(this);
    }

    @Override
    public Loader<List<E>> onCreateLoader(int id, Bundle bundle) {
        return new ThrowableLoader<List<E>>(getActivity(), listItems) {

            public List<E> loadData() throws IOException {
                pager.next();
                return pager.getResources();
            }
        };
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Intentionally left blank
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (!pager.hasMore())
            return;
        if (getLoaderManager().hasRunningLoaders())
            return;
        if (getListView().getLastVisiblePosition() >= pager.size())
            showMore();
    }

    @Override
    public void refresh() {
        pager.reset();
        super.refresh();
    }

    /**
     * Show more events while retaining the current pager state
     */
    private void showMore() {
        super.refresh();
    }

    public void onLoadFinished(Loader<List<E>> loader, List<E> items) {
        if (pager.hasMore())
            loadingIndicator.showLoading();
        else
            loadingIndicator.setVisible(false);

        super.onLoadFinished(loader, items);
    }
}
