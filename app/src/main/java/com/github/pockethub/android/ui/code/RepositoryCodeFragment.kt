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
package com.github.pockethub.android.ui.code

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.pockethub.android.Intents.EXTRA_REPOSITORY
import com.github.pockethub.android.R
import com.github.pockethub.android.RequestCodes.REF_UPDATE
import com.github.pockethub.android.core.code.FullTree
import com.github.pockethub.android.core.code.FullTree.Folder
import com.github.pockethub.android.core.code.RefreshTreeTask
import com.github.pockethub.android.core.ref.RefUtils
import com.github.pockethub.android.rx.AutoDisposeUtils
import com.github.pockethub.android.ui.base.BaseActivity
import com.github.pockethub.android.ui.DialogResultListener
import com.github.pockethub.android.ui.base.BaseFragment
import com.github.pockethub.android.ui.item.code.BlobItem
import com.github.pockethub.android.ui.item.code.FolderItem
import com.github.pockethub.android.ui.item.code.PathHeaderItem
import com.github.pockethub.android.ui.ref.BranchFileViewActivity
import com.github.pockethub.android.ui.ref.RefDialog
import com.github.pockethub.android.ui.ref.RefDialogFragment
import com.github.pockethub.android.util.ToastUtils
import com.github.pockethub.android.util.android.text.clickable
import com.meisolsson.githubsdk.model.Repository
import com.meisolsson.githubsdk.model.git.GitReference
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_repo_code.*
import kotlinx.android.synthetic.main.ref_footer.*
import java.util.LinkedList

/**
 * Fragment to display a repository's source code tree
 */
class RepositoryCodeFragment : BaseFragment(), OnItemClickListener, DialogResultListener {

    private val adapter = GroupAdapter<ViewHolder>()

    private val mainSection = Section()

    private var tree: FullTree? = null

    private var folder: Folder? = null

    private var repository: Repository? = null

    private var dialog: RefDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        adapter.add(mainSection)
        adapter.setOnItemClickListener(this)
        repository = activity!!.intent.getParcelableExtra(EXTRA_REPOSITORY)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (tree == null || folder == null) {
            refreshTree(null)
        } else {
            setFolder(tree, folder!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_refresh, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.m_refresh -> {
                if (tree != null) {
                    val ref = GitReference.builder()
                            .ref(tree!!.reference.ref())
                            .build()
                    refreshTree(ref)
                } else {
                    refreshTree(null)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLoading(loading: Boolean) {
        if (loading) {
            pb_loading.visibility = View.VISIBLE
            list.visibility = View.GONE
            rl_branch.visibility = View.GONE
        } else {
            pb_loading.visibility = View.GONE
            list.visibility = View.VISIBLE
            rl_branch.visibility = View.VISIBLE
        }
    }

    private fun refreshTree(reference: GitReference?) {
        showLoading(true)
        RefreshTreeTask(activity, repository, reference)
                .refresh()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDisposeUtils.bindToLifecycle(this))
                .subscribe({ fullTree ->
                    if (folder == null || folder!!.isRoot) {
                        setFolder(fullTree, fullTree.root)
                    } else {
                        // Look for current folder in new tree or else reset to root
                        var current: Folder = folder!!
                        val stack = LinkedList<Folder>()
                        while (!current.isRoot) {
                            stack.addFirst(current)
                            current = current.parent
                        }
                        var refreshed: Folder? = fullTree.root
                        while (!stack.isEmpty()) {
                            refreshed = refreshed!!.folders[stack.removeFirst().name]
                            if (refreshed == null) {
                                break
                            }
                        }
                        if (refreshed != null) {
                            setFolder(fullTree, refreshed)
                        } else {
                            setFolder(fullTree, fullTree.root)
                        }
                    }
                }, { e ->
                    Log.d(TAG, "Exception loading tree", e)

                    showLoading(false)
                    ToastUtils.show(activity, R.string.error_code_load)
                })
    }

    private fun switchBranches() {
        if (tree == null) {
            return
        }

        if (dialog == null) {
            dialog = RefDialog(activity as BaseActivity?, REF_UPDATE, repository)
        }
        dialog!!.show(tree!!.reference)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, arguments: Bundle) {
        if (RESULT_OK != resultCode) {
            return
        }

        when (requestCode) {
            REF_UPDATE -> refreshTree(RefDialogFragment.getSelected(arguments))
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_repo_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = adapter

        rl_branch.setOnClickListener { _ -> switchBranches() }

        mainSection.setHeader(PathHeaderItem(""))
    }

    /**
     * Back up the currently viewed folder to its parent
     *
     * @return true if directory changed, false otherwise
     */
    fun onBackPressed(): Boolean {
        return if (folder != null && !folder!!.isRoot) {
            setFolder(tree, folder!!.parent)
            true
        } else {
            false
        }
    }

    private fun setFolder(tree: FullTree?, folder: Folder) {
        this.folder = folder
        this.tree = tree

        showLoading(false)

        tv_branch.text = tree!!.branch
        if (RefUtils.isTag(tree.reference)) {
            tv_branch_icon.setText(R.string.icon_tag)
        } else {
            tv_branch_icon.setText(R.string.icon_fork)
        }

        if (folder.entry != null) {
            val textLightColor = resources.getColor(R.color.text_light)
            val segments = folder.entry.path()!!.split("/")
            val text = buildSpannedString {
                bold {
                    clickable(onClick = {
                        setFolder(tree, tree.root)
                    }) {
                        append(repository!!.name()!!)
                    }
                }
                append(' ')
                color(textLightColor) {
                    append('/')
                }
                append(' ')
                for (i in 0 until segments.size - 1) {
                    clickable(onClick = {
                        var clicked: Folder? = folder
                        for (i1 in i until segments.size - 1) {
                            clicked = clicked!!.parent
                            if (clicked == null) {
                                return@clickable
                            }
                        }
                        setFolder(tree, clicked!!)
                    }) {
                        append(segments[i])
                    }
                    append(' ')
                    color(textLightColor) {
                        append('/')
                    }
                    append(' ')
                }
                bold {
                    append(segments[segments.size - 1])
                }
                append(' ')
                color(textLightColor) {
                    append('/')
                }
                append(' ')
            }

            mainSection.setHeader(PathHeaderItem(text))
        } else {
            mainSection.removeHeader()
        }


        val items: List<Item<*>> =
            folder.folders.values.map { FolderItem(it) } +
            folder.files.values.map { BlobItem(activity!!, it) }

        mainSection.update(items)
    }

    override fun onItemClick(item: Item<*>, view: View) {
        if (tree == null) {
            return
        }

        if (item is BlobItem) {
            val entry = item.file
            startActivity(BranchFileViewActivity.createIntent(
                    repository!!,
                    tree!!.branch,
                    entry.entry.path()!!,
                    entry.entry.sha()!!
            ))
        } else if (item is FolderItem) {
            val folder = item.folder
            setFolder(tree, folder)
        }
    }

    companion object {

        private const val TAG = "RepositoryCodeFragment"
    }
}
