/*
 * Copyright 2011 b1.org
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

package org.b1.pack.cli;

import org.b1.pack.api.builder.BuilderFolder;

import java.util.List;

public class FsBuilderFolder implements BuilderFolder {

    private final FsObject fsObject;

    public FsBuilderFolder(FsObject fsObject) {
        this.fsObject = fsObject;
    }

    @Override
    public List<String> getPath() {
        return fsObject.getPath();
    }

    @Override
    public Long getLastModifiedTime() {
        return fsObject.getFile().lastModified();
    }

    @Override
    public void beforeAdd() {
        System.out.println("Adding " + fsObject.getFile());
    }
}
