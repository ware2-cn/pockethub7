package com.github.pockethub.android.ui.item.news

import android.view.View
import com.github.pockethub.android.ui.StyledText
import com.github.pockethub.android.ui.view.OcticonTextView
import com.github.pockethub.android.util.AvatarLoader
import com.meisolsson.githubsdk.model.GitHubEvent
import com.meisolsson.githubsdk.model.payload.GistPayload
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.news_item.*

class GistEventItem(
        avatarLoader: AvatarLoader,
        gitHubEvent: GitHubEvent
) : NewsItem(avatarLoader, gitHubEvent) {

    override fun bind(holder: ViewHolder, position: Int) {
        super.bind(holder, position)
        holder.tv_event_icon.text = OcticonTextView.ICON_GIST

        val main = StyledText()
        boldActor(main, gitHubEvent)

        val payload = gitHubEvent.payload() as GistPayload?

        main.append(' ')
        val action = payload?.action()

        val status: String? = when (action) {
            GistPayload.Action.Created -> "created"
            GistPayload.Action.Updated -> "updated"
            else -> action?.name
        }

        main.append(status)
        main.append(" Gist ")
        main.append(payload?.gist()?.id())

        holder.tv_event.text = main
        holder.tv_event_details.visibility = View.GONE
    }
}
