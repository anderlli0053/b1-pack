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

package org.b1.pack.standard.maker;

import com.google.common.base.Preconditions;
import com.google.common.io.CountingOutputStream;
import com.google.common.primitives.Ints;
import org.b1.pack.api.maker.PmProvider;
import org.b1.pack.api.maker.PmVolume;
import org.b1.pack.standard.common.Constants;
import org.b1.pack.standard.common.Numbers;
import org.b1.pack.standard.common.RecordPointer;
import org.b1.pack.standard.common.Volumes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

public class PackRecordStream extends OutputStream {

    private static final int PLAIN_BLOCK_OVERHEAD = Numbers.getSerializedSize(Constants.MAX_CHUNK_SIZE) + 6;

    private final ByteArrayOutputStream chunk = new ByteArrayOutputStream(Constants.MAX_CHUNK_SIZE);
    private final String archiveId = Volumes.createArchiveId();
    private final PmProvider provider;
    private RecordPointer catalogPointer;
    private long volumeNumber;
    private PmVolume volume;
    private CountingOutputStream volumeStream;
    private CheckedOutputStream chunkStream;
    private long volumeLimit;
    private int chunkLimit;
    private long volumeSize;

    public PackRecordStream(PmProvider provider) {
        this.provider = provider;
    }

    public RecordPointer getCurrentPointer() throws IOException {
        ensureFreeSpace();
        return new RecordPointer(volumeNumber, volumeStream.getCount(), chunk.size());
    }

    public void startCatalog() throws IOException {
        Preconditions.checkState(catalogPointer == null);
        //todo ensure more free space
        catalogPointer = getCurrentPointer();
        setVolumeLimit();
        setChunkLimit();
    }

    @Override
    public void write(int b) throws IOException {
        ensureFreeSpace();
        chunkStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            ensureFreeSpace();
            int size = Math.min(len, chunkLimit - chunk.size());
            chunkStream.write(b, off, size);
            off += size;
            len -= size;
        }
    }

    @Override
    public void close() throws IOException {
        endChunk();

        endVolume(true);
    }

    public void completeVolume() throws IOException {
        if (volume != null) {
            volume.complete();
            volume = null;
        }
    }

    private void ensureFreeSpace() throws IOException {
        if (chunk.size() < chunkLimit) {
            return;
        }
        endChunk();
        startChunk();
        setChunkLimit();
        if (chunkLimit > 0) {
            return;
        }
        endVolume(false);
        completeVolume();
        startVolume();
        setVolumeLimit();
        setChunkLimit();
        Preconditions.checkState(chunkLimit > 0, "Volume size too small");
    }

    private void startVolume() throws IOException {
        Preconditions.checkState(volume == null);
        Preconditions.checkState(volumeStream == null);
        volume = provider.getVolume(++volumeNumber);
        volumeSize = volume.getSize();
        volumeStream = new CountingOutputStream(volume.getOutputStream());
        volumeStream.write(Volumes.createVolumeHead(archiveId, volumeNumber, null));
    }

    private void setVolumeLimit() {
        volumeLimit = volumeSize == 0 ? Long.MAX_VALUE : volumeSize - Volumes.createVolumeTail(false, catalogPointer, 0).length - 1;
    }

    private void endVolume(boolean lastVolume) throws IOException {
        if (volumeStream == null) return;
        try {
            Numbers.writeLong(null, volumeStream);
            volumeStream.write(Volumes.createVolumeTail(lastVolume, catalogPointer, lastVolume ? 0 : volumeSize - volumeStream.getCount()));
        } finally {
            volumeStream.close();
            volumeStream = null;
        }
    }

    private void startChunk() {
        Preconditions.checkState(chunk.size() == 0);
        Preconditions.checkState(chunkStream == null);
        chunkStream = new CheckedOutputStream(chunk, new Adler32());
    }

    private void setChunkLimit() {
        chunkLimit = volumeStream == null ? 0 : (int) Math.min(Constants.MAX_CHUNK_SIZE, volumeLimit - volumeStream.getCount() - PLAIN_BLOCK_OVERHEAD);
    }

    private void endChunk() throws IOException {
        if (chunk.size() > 0) {
            Numbers.writeLong(Constants.PLAIN_BLOCK, volumeStream);
            Numbers.writeLong((long) chunk.size(), volumeStream);
            chunk.writeTo(volumeStream);
            Numbers.writeLong(0L, volumeStream);
            volumeStream.write(Ints.toByteArray((int) chunkStream.getChecksum().getValue()));
            chunk.reset();
        }
        chunkStream = null;
    }
}
