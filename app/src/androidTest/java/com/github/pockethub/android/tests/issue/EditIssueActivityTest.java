/*
 * Copyright (c) 2015 PocketHub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pockethub.android.tests.issue;

import android.view.View;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.rule.ActivityTestRule;
import com.github.pockethub.android.R;
import com.github.pockethub.android.tests.ViewVisibilityIdlingResource;
import com.github.pockethub.android.ui.issue.EditIssueActivity;
import com.github.pockethub.android.util.InfoUtils;
import com.meisolsson.githubsdk.model.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.not;

/**
 * Tests of {@link EditIssueActivity}
 */
public class EditIssueActivityTest {

    @Rule
    public ActivityTestRule<EditIssueActivity> activityTestRule =
            new ActivityTestRule<>(EditIssueActivity.class, true, false);

    @Before
    public void setUp() {
        Repository repo = InfoUtils.createRepoFromData("owner", "repo");
        activityTestRule.launchActivity(EditIssueActivity.Companion.createIntent(repo));
    }

    /**
     * Verify save menu is properly enabled/disable depending on the issue have
     * a non-empty title
     *
     * @throws Throwable
     */
    @Test
    public void testSaveMenuEnabled() {
        ViewInteraction createMenu = onView(withId(R.id.m_apply));
        ViewInteraction comment = onView(withId(R.id.et_issue_title));
        ViewVisibilityIdlingResource idlingResource =
                new ViewVisibilityIdlingResource(
                        activityTestRule.getActivity(),
                        R.id.sv_issue_content,
                        View.VISIBLE
                );

        IdlingRegistry.getInstance().register(idlingResource);
        createMenu.check(ViewAssertions.matches(not(isEnabled())));

        closeSoftKeyboard();
        comment.perform(ViewActions.typeText("a"));
        createMenu.check(ViewAssertions.matches(isEnabled()));
        comment.perform(ViewActions.replaceText(""));

        createMenu.check(ViewAssertions.matches(not(isEnabled())));
        IdlingRegistry.getInstance().unregister(idlingResource);
    }
}
