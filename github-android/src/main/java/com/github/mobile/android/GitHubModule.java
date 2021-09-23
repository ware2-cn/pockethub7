package com.github.mobile.android;

import static com.github.mobile.android.authenticator.Constants.GITHUB_ACCOUNT_TYPE;
import android.accounts.Account;
import android.accounts.AccountManager;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.io.IOException;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.UserService;

public class GitHubModule extends AbstractModule {

    private static final String TAG = "GHMod";

    @Override
    protected void configure() {
        HttpRequest.keepAlive(false);
    }

    @Provides
    Account currentAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType(GITHUB_ACCOUNT_TYPE);
        if (accounts.length > 0) {
            return accounts[0]; // at some point, support more than one github
            // account, ie vanilla and fi
        }
        return null;
    }

    @Provides
    GitHubClient client(Account account, AccountManager accountManager) {
        GitHubClient client = new GitHubClient();
        if (account != null)
            client.setCredentials(account.name, accountManager.getPassword(account));
        return client;
    }

    @Provides
    IssueService issueService(GitHubClient client) {
        return new IssueService(client);
    }

    @Provides
    PullRequestService pullRequestService(GitHubClient client) {
        return new PullRequestService(client);
    }

    @Provides
    UserService userService(GitHubClient client) {
        return new UserService(client);
    }

    @Provides
    GistService gistService(GitHubClient client) {
        return new GistService(client);
    }

    @Provides
    User currentUser(UserService userService) throws IOException {
        return userService.getUser(); // actually, probably better to cache this
    }
}
