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
package com.github.pockethub.android.tests.user;

import android.net.Uri;
import com.github.pockethub.android.core.user.UserUriMatcher;
import com.meisolsson.githubsdk.model.User;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests of {@link UserUriMatcher}
 */
public class UserUriMatcherTest {

    /**
     * Verify empty URI
     */
    @Test
    public void testEmptyUri() {
        assertNull(UserUriMatcher.getUser(Uri.parse("")));
    }

    /**
     * Verify no name
     */
    @Test
    public void testUriWithNoName() {
        assertNull(UserUriMatcher.getUser(Uri.parse("http://github.com")));
        assertNull(UserUriMatcher.getUser(Uri.parse("https://github.com")));
        assertNull(UserUriMatcher.getUser(Uri.parse("http://github.com/")));
        assertNull(UserUriMatcher.getUser(Uri.parse("http://github.com//")));
    }

    /**
     * Verify URI with name
     */
    @Test
    public void testHttpUriWithName() {
        User user = UserUriMatcher.getUser(Uri
                .parse("http://github.com/defunkt"));
        assertNotNull(user);
        assertEquals("defunkt", user.login());
    }

    /**
     * Verify URI with name
     */
    @Test
    public void testHttpsUriWithName() {
        User user = UserUriMatcher.getUser(Uri
                .parse("https://github.com/mojombo"));
        assertNotNull(user);
        assertEquals("mojombo", user.login());
    }

    /**
     * Verify URI with name
     */
    @Test
    public void testUriWithTrailingSlash() {
        User user = UserUriMatcher.getUser(Uri
                .parse("http://github.com/defunkt/"));
        assertNotNull(user);
        assertEquals("defunkt", user.login());
    }

    /**
     * Verify URI with name
     */
    @Test
    public void testUriWithTrailingSlashes() {
        User user = UserUriMatcher.getUser(Uri
                .parse("http://github.com/defunkt//"));
        assertNotNull(user);
        assertEquals("defunkt", user.login());
    }
}
