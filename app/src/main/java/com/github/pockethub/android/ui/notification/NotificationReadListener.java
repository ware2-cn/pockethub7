package com.github.pockethub.android.ui.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Repository;

/**
 * Created by savio on 2017-03-22.
 */

public interface NotificationReadListener {

    void readNotification(@NonNull NotificationThread thread);

    void readNotifications(@Nullable Repository repository);
}
