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
package com.github.pockethub.android.ui.gist

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.github.pockethub.android.Intents.Builder
import com.github.pockethub.android.Intents.EXTRA_GIST_ID
import com.github.pockethub.android.Intents.EXTRA_POSITION
import com.github.pockethub.android.R
import com.github.pockethub.android.core.gist.GistStore
import com.github.pockethub.android.core.gist.RefreshGistTaskFactory
import com.github.pockethub.android.rx.AutoDisposeUtils
import com.github.pockethub.android.ui.base.BaseActivity
import com.github.pockethub.android.ui.helpers.PagerHandler
import com.meisolsson.githubsdk.model.Gist
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_pager_with_title.*
import javax.inject.Inject

/**
 * Activity to page through the content of all the files in a Gist
 */
class GistFilesViewActivity : BaseActivity() {

    private var gistId: String? = null

    private var initialPosition: Int = 0

    private var gist: Gist? = null

    @Inject
    lateinit var store: GistStore

    @Inject
    lateinit var refreshGistTaskFactory: RefreshGistTaskFactory

    private var pagerHandler: PagerHandler<GistFilesPagerAdapter>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pager_with_title)

        gistId = intent.getStringExtra(EXTRA_GIST_ID)
        initialPosition = intent.getIntExtra(EXTRA_POSITION, -1)

        if (initialPosition < 0) {
            initialPosition = 0
        }

        supportActionBar!!.title = getString(R.string.gist_title) + gistId!!

        gist = store.getGist(gistId)
        if (gist != null) {
            configurePager()
        } else {
            pb_loading.visibility = View.VISIBLE
            vp_pages.visibility = View.GONE
            sliding_tabs_layout.visibility = View.GONE
            refreshGistTaskFactory.create(this, gistId)
                .refresh()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDisposeUtils.bindToLifecycle(this))
                .subscribe { gist ->
                    this.gist = gist.gist
                    configurePager()
                }
        }
    }

    private fun configurePager() {
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        val author = gist!!.owner()
        if (author != null) {
            actionBar.subtitle = author.login()
        } else {
            actionBar.setSubtitle(R.string.anonymous)
        }

        pb_loading.visibility = View.GONE
        vp_pages.visibility = View.VISIBLE
        sliding_tabs_layout.visibility = View.VISIBLE

        val adapter = GistFilesPagerAdapter(this, gist)
        pagerHandler = PagerHandler(this, vp_pages, adapter)
        lifecycle.addObserver(pagerHandler!!)
        sliding_tabs_layout.setupWithViewPager(vp_pages)

        if (initialPosition < adapter.count) {
            vp_pages.scheduleSetItem(initialPosition)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(pagerHandler!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (gist != null) {
                    val intent = GistsViewActivity.createIntent(gist!!)
                    intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {

        /**
         * Create intent to show files with an initial selected file
         *
         * @param gist
         * @param position
         * @return intent
         */
        fun createIntent(gist: Gist, position: Int): Intent {
            return Builder("gist.files.VIEW").gist(gist.id())
                .add(EXTRA_POSITION, position).toIntent()
        }
    }
}
