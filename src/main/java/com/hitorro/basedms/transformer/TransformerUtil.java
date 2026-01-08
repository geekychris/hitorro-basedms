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
package com.hitorro.basedms.transformer;

import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.ContentSetter;
import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.queue.QueueUtil;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;

import java.util.List;


public class TransformerUtil {

    public static final TransformJobParameters createJobParameters(String notificationGuid,
                                                                   String notificationGuidState,
                                                                   HTPredicate<Content> contentConstraint,
                                                                   String sysObjectGuid,
                                                                   HTPredicate<ConvertionEdge> constraint,
                                                                   String tagDomain,
                                                                   String tagValue,
                                                                   String targetFileNameSansExtension,
                                                                   boolean addAsChild) {
        // now let try to convertToPdf it.
        TransformJobParameters params = new TransformJobParameters();
        params.provisionId();

        params.setContentConstraint(contentConstraint);
        params.setJobGuid(sysObjectGuid);
        ContentSetter setter = new ContentSetter();

        TransformerService s = TransformerService.getService();
        List<ConvertionEdge> edges = TransformerService.getService().getConvertionContext().visit(constraint);
        if (ListUtil.nullOrEmpty(edges)) {
            Log.transformer.error("Unable to find a transformation edge");
            return null;
        }
        ConvertionEdge edge = edges.get(0);
        params.setTransformerMethod(edge.getTransformerMethod());
        setter.setMimeType(edge.getTargetMimeType());
        setter.setSysGuid(sysObjectGuid);

        String extension = ContentTypeCache.getCache().getFileNameExtensionForContentType(edge.getTargetMimeType());

        if (!StringUtil.nullOrEmptyString(extension)) {
            setter.setFileName(Fmt.S("%s.%s", targetFileNameSansExtension, extension));
        } else {
            setter.setFileName(targetFileNameSansExtension);
        }

        setter.setTagDomain(tagDomain);
        setter.setTagValue(tagValue);
        params.setContentSetter(setter);

        params.setNotifyGuid(notificationGuid);
        params.setNotifyGuidState(notificationGuidState);
        params.setAddContentAsChildOfContent(addAsChild);

        return params;
    }

    public static PersistedSerializedObject queueTransformJob(TransformJobParameters params,
                                                              DMSSession session,
                                                              boolean commit) {
        return QueueUtil.enqueJob(params, session, commit, PersistedSerializedObject.CollectionID_TranscoderQueue, 0);
    }
}
