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
package com.github.pockethub.android.core.issue

import com.meisolsson.githubsdk.model.GitHubComment
import com.meisolsson.githubsdk.model.Issue
import com.meisolsson.githubsdk.model.IssueEvent

/**
 * Issue model with comments
 */
data class FullIssue(val issue: Issue, val comments: Collection<GitHubComment>, val events: Collection<IssueEvent>)
