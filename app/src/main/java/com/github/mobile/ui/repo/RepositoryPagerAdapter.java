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
package com.github.mobile.ui.repo;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.github.mobile.R.string;
import com.github.mobile.ui.commit.CommitListFragment;
import com.github.mobile.ui.issue.IssuesFragment;

/**
 * Adapter to view a repository's various pages
 */
public class RepositoryPagerAdapter extends FragmentPagerAdapter {

    private final Resources resources;

    private final boolean hasIssues;

    /**
     * Create repository pager adapter
     *
     * @param fm
     * @param resources
     * @param hasIssues
     */
    public RepositoryPagerAdapter(FragmentManager fm, Resources resources,
            boolean hasIssues) {
        super(fm);

        this.resources = resources;
        this.hasIssues = hasIssues;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
        case 0:
            return resources.getString(string.commits);
        case 1:
            return resources.getString(string.news);
        case 2:
            return resources.getString(string.issues);
        default:
            return null;
        }
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
        case 0:
            return new CommitListFragment();
        case 1:
            return new RepositoryNewsFragment();
        case 2:
            return new IssuesFragment();
        default:
            return null;
        }
    }

    @Override
    public int getCount() {
        return hasIssues ? 3 : 2;
    }
}
