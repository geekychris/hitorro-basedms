/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms.contentpropertyextractor;

import com.hitorro.base.objects.ContentType;
import com.hitorro.basedms.ContentProperties;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class MP3Extractor implements ContentPropertiesExtractor {
    public String getMimeType() {
        return ContentType.MimeTypeMP3;
    }

    public boolean extract(ContentProperties props, BaseFile file) {
        MP3File mp3 = null;
        try {
            if (!file.isLocal()) {
                Log.filesystem.error("MP3Extractor needs a local file to work with");
                return false;
            }
            mp3 = new MP3File(((FileFile) file).getJavaFile());
            String struct = mp3.displayStructureAsXML();
            MP3AudioHeader header = mp3.getMP3AudioHeader();
            if (header != null) {
                props.setDurationSeconds(header.getTrackLength());
                String sampleRate = header.getSampleRate();
                if (!StringUtil.nullOrEmptyString(sampleRate)) {
                    props.setBitRate(Integer.parseInt(sampleRate));
                }
                props.setResolutionAux(header.getMpegVersion());
            }
            return true;
        } catch (IOException e) {
            Log.util.error("%s %e", e, e);
        } catch (TagException e) {
            Log.util.error("%s %e", e, e);
        } catch (ReadOnlyFileException e) {
            Log.util.error("%s %e", e, e);
        } catch (InvalidAudioFrameException e) {
            Log.util.error("%s %e", e, e);
        } catch (CannotReadException e) {
			throw new RuntimeException(e);
		}

		return false;
    }
}
