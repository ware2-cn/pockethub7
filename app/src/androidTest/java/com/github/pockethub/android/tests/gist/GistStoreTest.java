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
package com.github.pockethub.android.tests.gist;

import androidx.test.filters.SmallTest;
import com.github.pockethub.android.core.gist.GistStore;
import com.meisolsson.githubsdk.model.Gist;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Unit tests of {@link GistStore}
 */
@SmallTest
public class GistStoreTest {

    /**
     * Verify issue is updated when re-added
     */
    @Test
    public void testReuseIssue() {
        GistStore store = new GistStore();
        assertNull(store.getGist("abcd"));

        Gist gist = Gist.builder()
                .id("abcd")
                .description("description")
                .build();

        // The gist is added and the store will return the given gist
        assertEquals(gist, store.addGist(gist));
        assertEquals(gist, store.getGist("abcd"));

        Gist gist2 = Gist.builder()
                .id("abcd")
                .description("description2")
                .build();

        // The gist has now been updated and should not return the same gist
        assertNotEquals(gist, store.addGist(gist2));
        assertNotEquals(gist.description(), gist2.description());
        assertNotEquals(gist, store.getGist("abcd"));
    }
}
