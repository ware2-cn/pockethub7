package com.github.pockethub.android.ui.item.news

import android.view.View
import androidx.core.text.buildSpannedString
import com.github.pockethub.android.ui.view.OcticonTextView
import com.github.pockethub.android.util.AvatarLoader
import com.meisolsson.githubsdk.model.GitHubEvent
import com.meisolsson.githubsdk.model.payload.MemberPayload
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.news_item.*

class MemberEventItem(
        avatarLoader: AvatarLoader,
        gitHubEvent: GitHubEvent
) : NewsItem(avatarLoader, gitHubEvent) {

    override fun bind(holder: ViewHolder, position: Int) {
        super.bind(holder, position)
        holder.tv_event_icon.text = OcticonTextView.ICON_ADD_MEMBER
        holder.tv_event.text = buildSpannedString {
            val context = holder.root.context
            val payload = gitHubEvent.payload() as MemberPayload?
            boldActor(context, this, gitHubEvent)
            append(" added ")
            boldUser(context, this, payload?.member())
            append(" as a collaborator to ")
            boldRepo(context, this, gitHubEvent)
        }
        holder.tv_event_details.visibility = View.GONE
    }
}
