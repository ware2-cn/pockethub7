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
package com.github.pockethub.android.ui.commit

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Bundle
import android.view.MenuItem
import com.github.pockethub.android.Intents.Builder
import com.github.pockethub.android.Intents.EXTRA_BASES
import com.github.pockethub.android.Intents.EXTRA_POSITION
import com.github.pockethub.android.Intents.EXTRA_REPOSITORY
import com.github.pockethub.android.R
import com.github.pockethub.android.core.commit.CommitUtils
import com.github.pockethub.android.ui.base.BaseActivity
import com.github.pockethub.android.ui.helpers.PagerHandler
import com.github.pockethub.android.ui.item.commit.CommitItem
import com.github.pockethub.android.ui.repo.RepositoryViewActivity
import com.github.pockethub.android.util.InfoUtils
import com.meisolsson.githubsdk.model.Repository
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_pager.*

/**
 * Activity to display a commit
 */
class CommitViewActivity : BaseActivity() {

    private var repository: Repository? = null

    private var ids: Array<CharSequence>? = null

    private var initialPosition: Int = 0

    private lateinit var pagerHandler: PagerHandler<CommitPagerAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pager)

        repository = intent.getParcelableExtra(EXTRA_REPOSITORY)
        ids = intent.getCharSequenceArrayExtra(EXTRA_BASES)
        initialPosition = intent.getIntExtra(EXTRA_POSITION, -1)

        val adapter = CommitPagerAdapter(this, repository, ids)
        pagerHandler = PagerHandler(this, vp_pages, adapter)
        lifecycle.addObserver(pagerHandler)

        pagerHandler.onPagedChanged = this::onPageChanged
        vp_pages.scheduleSetItem(initialPosition, pagerHandler)
        onPageChanged(initialPosition)

        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.subtitle = InfoUtils.createRepoId(repository!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(pagerHandler)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = RepositoryViewActivity.createIntent(repository!!)
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onPageChanged(position: Int) {
        val id = CommitUtils.abbreviate(ids!![position].toString())
        supportActionBar!!.title = getString(R.string.commit_prefix) + id!!
    }

    companion object {

        /**
         * Create intent for this activity
         *
         * @param repository
         * @param id
         * @return intent
         */
        fun createIntent(repository: Repository,
            id: String): Intent {
            return createIntent(repository, 0, id)
        }

        /**
         * Create intent for this activity
         *
         * @param repository
         * @param position
         * @param commits
         * @return intent
         */
        fun createIntent(
            repository: Repository,
            position: Int,
            commits: Collection<Item<*>>
        ): Intent {
            val ids = commits.map { (it as CommitItem).commit.sha()!! }.toTypedArray()
            return createIntent(repository, position, *ids)
        }

        /**
         * Create intent for this activity
         *
         * @param repository
         * @param position
         * @param ids
         * @return intent
         */
        fun createIntent(repository: Repository, position: Int, vararg ids: String): Intent {
            val builder = Builder("commits.VIEW")
            builder.add(EXTRA_POSITION, position)
            builder.add(EXTRA_BASES, ids)
            builder.repo(repository)
            return builder.toIntent()
        }
    }
}
