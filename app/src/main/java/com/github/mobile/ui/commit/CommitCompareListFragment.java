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
package com.github.mobile.ui.commit;

import static com.github.mobile.Intents.EXTRA_BASE;
import static com.github.mobile.Intents.EXTRA_HEAD;
import static com.github.mobile.Intents.EXTRA_REPOSITORY;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.github.mobile.R.id;
import com.github.mobile.core.commit.CommitCompareTask;
import com.github.mobile.ui.DialogFragment;
import com.github.mobile.ui.HeaderFooterListAdapter;
import com.github.mobile.util.AvatarLoader;
import com.github.mobile.util.ViewUtils;
import com.google.inject.Inject;
import com.viewpagerindicator.R.layout;

import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryCommitCompare;

import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;

/**
 * Fragment to display a list of commits being compared
 */
public class CommitCompareListFragment extends DialogFragment implements
        OnItemClickListener {

    private DiffStyler diffStyler;

    @InjectView(android.R.id.list)
    private ListView list;

    @InjectView(id.pb_loading)
    private ProgressBar progress;

    @InjectExtra(EXTRA_REPOSITORY)
    private Repository repository;

    @InjectExtra(EXTRA_BASE)
    private String base;

    @InjectExtra(EXTRA_HEAD)
    private String head;

    @Inject
    private AvatarLoader avatars;

    private HeaderFooterListAdapter<CommitFileListAdapter> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        diffStyler = new DiffStyler(getResources());
        compareCommits();
    }

    private void compareCommits() {
        new CommitCompareTask(getActivity(), repository, base, head) {

            @Override
            protected RepositoryCommitCompare run() throws Exception {
                RepositoryCommitCompare compare = super.run();

                List<CommitFile> files = compare.getFiles();
                diffStyler.setFiles(files);
                if (files != null)
                    Collections.sort(files, new CommitFileComparator());
                return compare;
            }

            @Override
            protected void onSuccess(RepositoryCommitCompare compare)
                    throws Exception {
                super.onSuccess(compare);

                updateList(compare);
            }

        }.execute();
    }

    private void updateList(RepositoryCommitCompare compare) {
        if (!isUsable())
            return;

        ViewUtils.setGone(progress, true);
        ViewUtils.setGone(list, false);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        adapter.clearHeaders();
        List<RepositoryCommit> commits = compare.getCommits();
        if (commits != null && !commits.isEmpty()) {
            CommitListAdapter commitAdapter = new CommitListAdapter(
                    layout.commit_item, inflater, commits, avatars);
            for (int i = 0; i < commits.size(); i++) {
                RepositoryCommit commit = commits.get(i);
                View view = commitAdapter.getView(i, null, null);
                adapter.addHeader(view, commit, true);
            }
        }

        List<CommitFile> files = compare.getFiles();
        if (files != null && !files.isEmpty())
            adapter.getWrappedAdapter().setItems(
                    files.toArray(new CommitFile[files.size()]));
        else
            adapter.getWrappedAdapter().setItems(null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        list.setOnItemClickListener(this);
        adapter = new HeaderFooterListAdapter<CommitFileListAdapter>(list,
                new CommitFileListAdapter(layout.commit_file_item,
                        getActivity().getLayoutInflater(), diffStyler));
        list.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(layout.item_list, container);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Object item = parent.getItemAtPosition(position);
        if (item instanceof RepositoryCommit)
            startActivity(CommitViewActivity.createIntent(repository,
                    ((RepositoryCommit) item).getSha()));
    }
}
