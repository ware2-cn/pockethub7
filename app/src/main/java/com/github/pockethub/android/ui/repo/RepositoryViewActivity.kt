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
package com.github.pockethub.android.ui.repo

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.github.pockethub.android.Intents.Builder
import com.github.pockethub.android.Intents.EXTRA_REPOSITORY
import com.github.pockethub.android.R
import com.github.pockethub.android.ResultCodes.RESOURCE_CHANGED
import com.github.pockethub.android.core.repo.RepositoryUtils
import com.github.pockethub.android.rx.AutoDisposeUtils
import com.github.pockethub.android.ui.DialogResultListener
import com.github.pockethub.android.ui.base.BaseActivity
import com.github.pockethub.android.ui.helpers.PagerHandler
import com.github.pockethub.android.ui.user.UriLauncherActivity
import com.github.pockethub.android.ui.user.UserViewActivity
import com.github.pockethub.android.util.InfoUtils
import com.github.pockethub.android.util.ShareUtils
import com.github.pockethub.android.util.ToastUtils
import com.meisolsson.githubsdk.core.ServiceGenerator
import com.meisolsson.githubsdk.model.Repository
import com.meisolsson.githubsdk.service.activity.StarringService
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService
import com.meisolsson.githubsdk.service.repositories.RepositoryForkService
import com.meisolsson.githubsdk.service.repositories.RepositoryService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.pager_with_tabs.*
import kotlinx.android.synthetic.main.tabbed_progress_pager.*
import retrofit2.Response

/**
 * Activity to view a repository
 */
class RepositoryViewActivity : BaseActivity(), DialogResultListener {

    private var repository: Repository? = null

    private var isStarred: Boolean = false

    private var starredStatusChecked: Boolean = false

    private var hasReadme: Boolean = false

    private var pagerHandler: PagerHandler<RepositoryPagerAdapter>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tabbed_progress_pager)

        repository = intent.getParcelableExtra(EXTRA_REPOSITORY)
        val owner = repository!!.owner()!!

        val actionBar = supportActionBar!!
        actionBar.title = repository!!.name()
        actionBar.subtitle = owner.login()
        actionBar.setDisplayHomeAsUpEnabled(true)

        if (owner.avatarUrl() != null && RepositoryUtils.isComplete(repository!!)) {
            checkReadme()
        } else {
            pb_loading.visibility = View.VISIBLE
            ServiceGenerator.createService(this, RepositoryService::class.java)
                .getRepository(repository!!.owner()!!.login(), repository!!.name())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDisposeUtils.bindToLifecycle(this))
                .subscribe({ response ->
                    if (response.isSuccessful) {
                        repository = response.body()
                        checkReadme()
                    } else {
                        ToastUtils.show(this, R.string.error_repo_load)
                        pb_loading.visibility = View.GONE
                    }
                }, { e ->
                    ToastUtils.show(this, R.string.error_repo_load)
                    pb_loading.visibility = View.GONE
                })
        }
    }

    override fun onCreateOptionsMenu(optionsMenu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_repository, optionsMenu)
        return super.onCreateOptionsMenu(optionsMenu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val followItem = menu.findItem(R.id.m_star)

        followItem.isVisible = starredStatusChecked
        followItem.setTitle(if (isStarred) R.string.unstar else R.string.star)

        val parentRepo = menu.findItem(R.id.m_parent_repo)
        if (repository != null && repository!!.isFork != null)
            parentRepo.isVisible = repository!!.isFork!!

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onBackPressed() {
        if (pagerHandler?.adapter == null || vp_pages.currentItem != pagerHandler?.adapter!!.itemCode || !pagerHandler?.adapter!!.onBackPressed()) {
            super.onBackPressed()
        }
    }

    private fun checkReadme() {
        pb_loading.visibility = View.VISIBLE
        ServiceGenerator.createService(this, RepositoryContentService::class.java)
            .hasReadme(repository!!.owner()!!.login(), repository!!.name())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDisposeUtils.bindToLifecycle(this))
            .subscribe({ response ->
                hasReadme = response.code() == 200
                configurePager()
            }, { e ->
                hasReadme = false
                configurePager()
            })
    }

    private fun configurePager() {
        val adapter = RepositoryPagerAdapter(this, repository!!.hasIssues()!!, hasReadme)
        pagerHandler = PagerHandler(this, vp_pages, adapter)
        lifecycle.addObserver(pagerHandler!!)
        pagerHandler!!.tabs = sliding_tabs_layout

        pb_loading.visibility = View.GONE
        pagerHandler!!.setGone(false)
        checkStarredRepositoryStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(pagerHandler!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.m_star -> {
                starRepository()
                return true
            }
            R.id.m_fork -> {
                forkRepository()
                return true
            }
            R.id.m_contributors -> {
                startActivity(RepositoryContributorsActivity.createIntent(repository))
                return true
            }
            R.id.m_share -> {
                shareRepository()
                return true
            }
            R.id.m_parent_repo -> {
                if (repository!!.parent() == null) {
                    ServiceGenerator.createService(this, RepositoryService::class.java)
                        .getRepository(repository!!.owner()!!.login(), repository!!.name())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { response ->
                            val parent = response.body()!!.parent()
                            startActivity(RepositoryViewActivity.createIntent(parent!!))
                        }
                } else {
                    startActivity(RepositoryViewActivity.createIntent(repository!!.parent()!!))
                }
                return true
            }
            R.id.m_delete -> {
                deleteRepository()
                return true
            }
            R.id.m_refresh -> {
                checkStarredRepositoryStatus()
                return super.onOptionsItemSelected(item)
            }
            android.R.id.home -> {
                finish()
                val intent = UserViewActivity.createIntent(repository!!.owner()!!)
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, arguments: Bundle) {
        pagerHandler?.adapter!!.onDialogResult(vp_pages.currentItem, requestCode, resultCode, arguments)
    }

    private fun starRepository() {
        val service = ServiceGenerator.createService(this, StarringService::class.java)

        val starSingle: Single<Response<Void>>
        starSingle = if (isStarred) {
            service.unstarRepository(repository!!.owner()!!.login(), repository!!.name())
        } else {
            service.starRepository(repository!!.owner()!!.login(), repository!!.name())
        }

        starSingle.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDisposeUtils.bindToLifecycle(this))
            .subscribe({ aVoid ->
                isStarred = !isStarred
                setResult(RESOURCE_CHANGED)
            }, { e -> ToastUtils.show(this, if (isStarred) R.string.error_unstarring_repository else R.string.error_starring_repository) })
    }

    private fun checkStarredRepositoryStatus() {
        starredStatusChecked = false
        ServiceGenerator.createService(this, StarringService::class.java)
            .checkIfRepositoryIsStarred(repository!!.owner()!!.login(), repository!!.name())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDisposeUtils.bindToLifecycle(this))
            .subscribe { response ->
                isStarred = response.code() == 204
                starredStatusChecked = true
                invalidateOptionsMenu()
            }
    }

    private fun shareRepository() {
        var repoUrl = repository!!.htmlUrl()
        if (TextUtils.isEmpty(repoUrl)) {
            repoUrl = "https://github.com/" + InfoUtils.createRepoId(repository!!)
        }
        val sharingIntent = ShareUtils.create(InfoUtils.createRepoId(repository!!), repoUrl)
        startActivity(sharingIntent)
    }

    private fun forkRepository() {
        ServiceGenerator.createService(this, RepositoryForkService::class.java)
            .createFork(repository!!.owner()!!.login(), repository!!.name())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDisposeUtils.bindToLifecycle(this))
            .subscribe({ response ->
                val repo = response.body()!!
                UriLauncherActivity.launchUri(this, Uri.parse(repo.htmlUrl()))
            }, { e -> ToastUtils.show(this, R.string.error_forking_repository) })
    }

    private fun deleteRepository() {
        MaterialDialog.Builder(this)
            .title(R.string.are_you_sure)
            .content(R.string.unexpected_bad_things)
            .positiveText(R.string.not_sure)
            .negativeText(R.string.delete_cap)
            .callback(object : MaterialDialog.ButtonCallback() {
                override fun onPositive(dialog: MaterialDialog?) {
                    super.onPositive(dialog)
                    dialog!!.dismiss()
                }

                override fun onNegative(dialog: MaterialDialog?) {
                    super.onNegative(dialog)
                    dialog!!.dismiss()

                    ServiceGenerator.createService(dialog.context, RepositoryService::class.java)
                        .deleteRepository(repository!!.owner()!!.login(), repository!!.name())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .`as`(AutoDisposeUtils.bindToLifecycle(lifecycle))
                        .subscribe({ response ->
                            onBackPressed()
                            ToastUtils.show(this@RepositoryViewActivity, R.string.delete_successful)
                        }, { e -> ToastUtils.show(this@RepositoryViewActivity, R.string.error_deleting_repository) })
                }
            })
            .show()
    }

    companion object {
        val TAG = "RepositoryViewActivity"

        /**
         * Create intent for this activity
         *
         * @param repository
         * @return intent
         */
        fun createIntent(repository: Repository): Intent {
            return Builder("repo.VIEW").repo(repository).toIntent()
        }
    }
}
