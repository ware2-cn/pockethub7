/*
 * Copyright 2012 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mobile.gist;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.github.mobile.RequestCodes.COMMENT_CREATE;
import static com.github.mobile.util.GitHubIntents.EXTRA_COMMENTS;
import static com.github.mobile.util.GitHubIntents.EXTRA_COMMENT_BODY;
import static com.github.mobile.util.GitHubIntents.EXTRA_GIST_ID;
import static com.google.common.collect.Lists.newArrayList;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.mobile.R.id;
import com.github.mobile.R.layout;
import com.github.mobile.R.menu;
import com.github.mobile.R.string;
import com.github.mobile.RefreshAnimation;
import com.github.mobile.async.AuthenticatedUserTask;
import com.github.mobile.comment.CommentViewHolder;
import com.github.mobile.comment.CreateCommentActivity;
import com.github.mobile.core.gist.FullGist;
import com.github.mobile.core.gist.GistStore;
import com.github.mobile.util.AccountUtils;
import com.github.mobile.util.AvatarUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.ToastUtils;
import com.github.mobile.util.TypefaceUtils;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.google.inject.Inject;
import com.madgag.android.listviews.ReflectiveHolderFactory;
import com.madgag.android.listviews.ViewHoldingListAdapter;
import com.madgag.android.listviews.ViewInflator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.GistService;

import roboguice.inject.ContextScopedProvider;
import roboguice.inject.InjectView;

/**
 * Activity to display an existing Gist
 */
public class GistFragment extends RoboSherlockFragment implements OnItemClickListener {

    private static final String TAG = "GistFragment";

    private String gistId;

    private List<Comment> comments;

    private Gist gist;

    @InjectView(android.R.id.list)
    private ListView list;

    @Inject
    private GistStore store;

    @Inject
    private ContextScopedProvider<GistService> service;

    private View headerView;

    private GistHeaderViewHolder headerHolder;

    private View loadingView;

    private RefreshAnimation refreshAnimation = new RefreshAnimation();

    private boolean starred;

    private boolean loadFinished;

    @Inject
    private AvatarUtils avatarHelper;

    @Inject
    private ContextScopedProvider<GistService> gistServiceProvider;

    private Executor executor = Executors.newFixedThreadPool(1);

    private List<View> fileHeaders = newArrayList();

    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        gistId = getArguments().getString(EXTRA_GIST_ID);
        comments = (List<Comment>) getArguments().getSerializable(EXTRA_COMMENTS);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(layout.gist_view, null);

        headerView = inflater.inflate(layout.gist_header, null);
        headerHolder = new GistHeaderViewHolder(headerView);

        loadingView = inflater.inflate(layout.load_item, null);

        return root;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        list.setOnItemClickListener(this);
        list.setFastScrollEnabled(true);
        list.addHeaderView(headerView, null, false);

        gist = store.getGist(gistId);

        if (gist != null) {
            headerHolder.updateViewFor(gist);
            updateFiles(gist);
        } else {
            ((TextView) loadingView.findViewById(id.tv_loading)).setText(string.loading_gist);
            headerView.setVisibility(GONE);
        }

        if (gist == null || (gist.getComments() > 0 && comments == null))
            list.addHeaderView(loadingView, null, false);

        List<Comment> initialComments = comments;
        if (initialComments == null)
            initialComments = Collections.emptyList();
        list.setAdapter(new ViewHoldingListAdapter<Comment>(initialComments, ViewInflator.viewInflatorFor(
                getActivity(), layout.comment_view_item), ReflectiveHolderFactory.reflectiveFactoryFor(
                CommentViewHolder.class, avatarHelper)));

        if (gist != null && comments != null)
            updateList(gist, comments);

        refreshGist();
    }

    private boolean isOwner() {
        if (gist == null)
            return false;
        User user = gist.getUser();
        if (user == null)
            return false;
        String login = AccountUtils.getLogin(getActivity());
        return login != null && login.equals(user.getLogin());
    }

    @Override
    public void onCreateOptionsMenu(Menu options, MenuInflater inflater) {
        inflater.inflate(menu.gist_view, options);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean owner = isOwner();
        menu.findItem(id.gist_delete).setEnabled(owner);
        MenuItem starItem = menu.findItem(id.gist_star);
        starItem.setEnabled(loadFinished && !owner);
        if (starred)
            starItem.setTitle(string.unstar_gist);
        else
            starItem.setTitle(string.star_gist);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (gist == null)
            return super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
        case id.gist_comment:
            String title = getString(string.gist_title) + gistId;
            User user = gist.getUser();
            String subtitle = user != null ? user.getLogin() : null;
            startActivityForResult(CreateCommentActivity.createIntent(title, subtitle, user), COMMENT_CREATE);
            return true;
        case id.gist_star:
            if (starred)
                unstarGist();
            else
                starGist();
            return true;
        case id.refresh:
            refreshAnimation.setRefreshItem(item).start(getActivity());
            refreshGist();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void starGist() {
        ToastUtils.show(getActivity(), string.starring_gist);
        new AuthenticatedUserTask<Gist>(getActivity()) {

            public Gist run() throws Exception {
                gistServiceProvider.get(getContext()).starGist(gistId);
                starred = true;
                return null;
            }

            protected void onException(Exception e) throws RuntimeException {
                Log.d(TAG, "Exception starring gist", e);
                ToastUtils.show((Activity) getContext(), e.getMessage());
            }
        }.execute();
    }

    private void unstarGist() {
        ToastUtils.show(getActivity(), string.unstarring_gist);
        new AuthenticatedUserTask<Gist>(getActivity()) {

            public Gist run() throws Exception {
                gistServiceProvider.get(getActivity()).unstarGist(gistId);
                starred = false;
                return null;
            }

            protected void onException(Exception e) throws RuntimeException {
                Log.d(TAG, "Exception unstarring gist", e);
                ToastUtils.show((Activity) getContext(), e.getMessage());
            }
        }.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode && COMMENT_CREATE == requestCode && data != null) {
            String comment = data.getStringExtra(EXTRA_COMMENT_BODY);
            if (comment != null && comment.length() > 0) {
                createComment(comment);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void createComment(final String comment) {
        final ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setMessage(getString(string.creating_comment));
        progress.setIndeterminate(true);
        progress.show();
        new AuthenticatedUserTask<Comment>(getActivity()) {

            public Comment run() throws Exception {
                return gistServiceProvider.get(getActivity()).createComment(gistId, comment);
            }

            protected void onSuccess(Comment comment) throws Exception {
                progress.dismiss();
                refreshGist();
            }

            protected void onException(Exception e) throws RuntimeException {
                progress.dismiss();

                Log.d(TAG, "Exception creating comment on gist", e);

                ToastUtils.show((Activity) getContext(), e.getMessage());
            }
        }.execute();

    }

    private void updateFiles(Gist gist) {
        for (View header : fileHeaders)
            list.removeHeaderView(header);
        fileHeaders.clear();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        Typeface octocons = TypefaceUtils.getOctocons(getActivity());
        for (GistFile file : gist.getFiles().values()) {
            View fileView = inflater.inflate(layout.gist_view_file_item, null);
            TextView nameText = (TextView) fileView.findViewById(id.tv_file);
            nameText.setText(file.getFilename());
            ((TextView) fileView.findViewById(id.tv_file_icon)).setTypeface(octocons);
            list.addHeaderView(fileView, file, true);
            fileHeaders.add(fileView);
        }
    }

    private void updateList(Gist gist, List<Comment> comments) {
        list.removeHeaderView(loadingView);
        headerView.setVisibility(VISIBLE);
        headerHolder.updateViewFor(gist);

        updateFiles(gist);

        ViewHoldingListAdapter<Comment> adapter = getRootAdapter();
        if (adapter != null)
            adapter.setList(comments);
    }

    @SuppressWarnings("unchecked")
    private ViewHoldingListAdapter<Comment> getRootAdapter() {
        ListAdapter adapter = list.getAdapter();
        if (adapter == null)
            return null;
        adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        if (adapter instanceof ViewHoldingListAdapter<?>)
            return (ViewHoldingListAdapter<Comment>) adapter;
        else
            return null;
    }

    private void refreshGist() {
        new AuthenticatedUserTask<FullGist>(getActivity(), executor) {

            public FullGist run() throws Exception {
                Gist gist = store.refreshGist(gistId);
                GistService gistService = service.get(getContext());
                List<Comment> comments;
                if (gist.getComments() > 0)
                    comments = gistService.getComments(gistId);
                else
                    comments = Collections.emptyList();
                for (Comment comment : comments)
                    comment.setBodyHtml(HtmlUtils.format(comment.getBodyHtml()).toString());
                return new FullGist(gist, gistService.isStarred(gistId), comments);
            }

            protected void onException(Exception e) throws RuntimeException {
                Log.d(TAG, "Exception refreshing gist", e);

                ToastUtils.show(getActivity(), e, string.error_gist_load);
            }

            protected void onSuccess(FullGist fullGist) throws Exception {
                if (getActivity() == null)
                    return;

                starred = fullGist.isStarred();
                loadFinished = true;
                gist = fullGist.getGist();
                comments = fullGist;
                getArguments().putSerializable(EXTRA_COMMENTS, fullGist);
                updateList(fullGist.getGist(), fullGist);
            }

            protected void onFinally() throws RuntimeException {
                refreshAnimation.stop();
            }
        }.execute();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = parent.getItemAtPosition(position);
        if (item instanceof GistFile)
            startActivity(ViewGistFilesActivity.createIntent(gist, position - 1));
    }
}
