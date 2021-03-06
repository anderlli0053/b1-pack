/*
 * Copyright 2012 b1.org                                                   
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

package org.b1.pack.standard.reader;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import org.b1.pack.api.common.FileBuilder;
import org.b1.pack.api.common.FileContent;
import org.b1.pack.standard.common.Constants;
import org.b1.pack.standard.common.Numbers;
import org.b1.pack.standard.common.RecordPointer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class StandardFileContent implements FileContent, InputSupplier<InputStream> {
    
    private final Long id;
    private final RecordPointer pointer;
    private final PackInputStream inputStream;
    private final FileBuilder builder;
    private final long size;

    public StandardFileContent(Long id, RecordPointer pointer, PackInputStream inputStream, FileBuilder builder, long size) {
        this.id = id;
        this.pointer = pointer;
        this.inputStream = inputStream;
        this.builder = builder;
        this.size = size;
    }

    public void save() throws IOException {
        builder.setContent(this);
        builder.save();
    }

    @Override
    public void writeTo(OutputStream stream, long start, Long end) throws IOException {
        long length = (end != null ? end : size) - start;
        InputSupplier<InputStream> slice = ByteStreams.slice(this, start, length);
        Preconditions.checkState(ByteStreams.copy(slice, stream) == length);
    }

    @Override
    public InputStream getInput() throws IOException {
        inputStream.seek(pointer);
        Preconditions.checkArgument(Numbers.readLong(inputStream) == Constants.COMPLETE_FILE);
        RecordHeader header = RecordHeader.readRecordHeader(inputStream);// ignore for now
        Preconditions.checkState(Objects.equal(header.id, id));
        return new ChunkedInputStream(inputStream);
    }
}
