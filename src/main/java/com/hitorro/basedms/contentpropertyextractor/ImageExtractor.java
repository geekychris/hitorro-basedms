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

import com.hitorro.basedms.ContentProperties;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.ImageManipulator;

import java.io.IOException;


public abstract class ImageExtractor implements ContentPropertiesExtractor {
    public boolean extract(ContentProperties props, BaseFile file) {
        ImageManipulator im = new ImageManipulator();
        try {
            im.setInputFile(file);
        } catch (IOException e) {
            Log.util.error("Unable to read image file %s with error %s %e", file, e, e);
            return false;
        }
        props.setHeight(im.getImageHeight());
        props.setWidth(im.getImageWidth());
        props.setResolutionAux(Fmt.S("%sx%s", im.getImageHeight(), im.getImageWidth()));

        return true;
    }
}

