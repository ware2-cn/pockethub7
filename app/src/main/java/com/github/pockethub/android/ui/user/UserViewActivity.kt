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
package com.github.pockethub.android.ui.user

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.pockethub.android.Intents.Builder
import com.github.pockethub.android.Intents.EXTRA_USER
import com.github.pockethub.android.R
import com.github.pockethub.android.accounts.AccountUtils
import com.github.pockethub.android.rx.AutoDisposeUtils
import com.github.pockethub.android.ui.base.BaseActivity
import com.github.pockethub.android.ui.MainActivity
import com.github.pockethub.android.ui.helpers.PagerHandler
import com.github.pockethub.android.util.ToastUtils
import com.meisolsson.githubsdk.core.ServiceGenerator
import com.meisolsson.githubsdk.model.User
import com.meisolsson.githubsdk.service.users.UserFollowerService
import com.meisolsson.githubsdk.service.users.UserService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.pager_with_tabs.*
import kotlinx.android.synthetic.main.tabbed_progress_pager.*
import retrofit2.Response

/**
 * Activity to view a user's various pages
 */
class UserViewActivity : BaseActivity(), OrganizationSelectionProvider {

    private var user: User? = null

    private var isFollowing: Boolean = false

    private var followingStatusChecked: Boolean = false

    private var pagerHandler: PagerHandler<UserPagerAdapter>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tabbed_progress_pager)

        user = intent.getParcelableExtra(EXTRA_USER)

        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = user!!.login()

        if (!TextUtils.isEmpty(user!!.avatarUrl())) {
            configurePager()
        } else {
            pb_loading.visibility = View.VISIBLE
            ServiceGenerator.createService(this, UserService::class.java)
                .getUser(user!!.login())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDisposeUtils.bindToLifecycle(this))
                .subscribe({ response ->
                    user = response.body()
                    configurePager()
                }, { e ->
                    ToastUtils.show(this, R.string.error_person_load)
                    pb_loading.visibility = View.GONE
                })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_user_follow, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val followItem = menu.findItem(R.id.m_follow)
        val isCurrentUser = user!!.login() == AccountUtils.getLogin(this)

        followItem.isVisible = followingStatusChecked && !isCurrentUser
        followItem.setTitle(if (isFollowing) R.string.unfollow else R.string.follow)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.m_follow -> {
                followUser()
                true
            }
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun configurePager() {
        val adapter = UserPagerAdapter(this)
        pagerHandler = PagerHandler(this, vp_pages, adapter)
        lifecycle.addObserver(pagerHandler!!)
        pagerHandler!!.tabs = sliding_tabs_layout

        pb_loading.visibility = View.GONE
        pagerHandler!!.setGone(false)
        checkFollowingUserStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(pagerHandler!!)
    }

    override fun addListener(listener: OrganizationSelectionListener): User? {
        return user
    }

    override fun removeListener(
        listener: OrganizationSelectionListener
    ): OrganizationSelectionProvider {
        return this
    }

    private fun followUser() {
        val service = ServiceGenerator.createService(this, UserFollowerService::class.java)

        val followSingle: Single<Response<Void>>
        followSingle = if (isFollowing) {
            service.unfollowUser(user!!.login())
        } else {
            service.followUser(user!!.login())
        }

        followSingle.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDisposeUtils.bindToLifecycle(this))
            .subscribe({ aVoid -> isFollowing = !isFollowing },
                { e ->
                    val message = if (isFollowing)
                        R.string.error_unfollowing_person
                    else
                        R.string.error_following_person

                    ToastUtils.show(this, message)
                })
    }

    private fun checkFollowingUserStatus() {
        followingStatusChecked = false
        ServiceGenerator.createService(this, UserFollowerService::class.java)
            .isFollowing(user!!.login())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDisposeUtils.bindToLifecycle(this))
            .subscribe { response ->
                isFollowing = response.code() == 204
                followingStatusChecked = true
                invalidateOptionsMenu()
            }
    }

    companion object {

        /**
         * Create intent for this activity
         *
         * @param user
         * @return intent
         */
        fun createIntent(user: User): Intent {
            return Builder("user.VIEW").user(user).toIntent()
        }
    }
}
