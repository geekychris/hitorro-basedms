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
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.queue.QueueUtil;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.typesystem.BaseSession;
import org.apache.log4j.Level;

import java.io.IOException;
import java.util.List;

public class TransformerUtil {

	public static TransformJobParameters createJobParameters(String notificationGuid,
			String notificationGuidState,
			HTPredicate<Content> contentConstraint,
			String sysObjectGuid,
			HTPredicate<ConvertionEdge> constraint,
			String tagDomain,
			String tagValue,
			String targetFileNameSansExtension,
			boolean addAsChild) {
		return createJobParameters(notificationGuid, notificationGuidState, contentConstraint, sysObjectGuid,
				constraint, tagDomain, tagValue, targetFileNameSansExtension, addAsChild, null, null);
	}

	public static TransformJobParameters createJobParameters(String notificationGuid,
			String notificationGuidState,
			HTPredicate<Content> contentConstraint,
			String sysObjectGuid,
			HTPredicate<ConvertionEdge> constraint,
			String tagDomain,
			String tagValue,
			String targetFileNameSansExtension,
			boolean addAsChild,
			String templateGuid,
			String parameters) {
		// now let try to convertToPdf it.
		TransformJobParameters params = new TransformJobParameters();
		params.provisionId();

		params.setContentConstraint(contentConstraint);
		params.setJobGuid(sysObjectGuid);
		params.setTransformerMethodArgs(parameters);
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
		params.setTemplateGuid(templateGuid);

		return params;
	}

	public static PersistedSerializedObject queueTransformJob(TransformJobParameters params,
			DMSSession session,
			boolean commit) {
		return QueueUtil.enqueJob(params, session, commit, PersistedSerializedObject.CollectionID_TranscoderQueue, 0);
	}

	public static JobExecutionResult executeJobInline(String notificationGuid,
			String notificationGuidState,
			HTPredicate<Content> contentConstraint,
			String sysObjectGuid,
			HTPredicate<ConvertionEdge> constraint,
			String tagDomain,
			String tagValue,
			String targetFileNameSansExtension,
			boolean addAsChild,
			BaseSession session) throws IOException {
		TransformJobParameters params = createJobParameters(notificationGuid, notificationGuidState, contentConstraint,
				sysObjectGuid, constraint, tagDomain, tagValue, targetFileNameSansExtension, addAsChild, null, null);

		return getJobExecutionResult(params, session);
	}

	public static JobExecutionResult getJobExecutionResult(TransformJobParameters parameters, BaseSession session)
			throws IOException {
		TransformJobParameters params = parameters;

		String method = params.getTransformerMethod();
		TransformMethod m = TransformerService.getService().getMethod(method);
		if (m == null) {
			return new JobExecutionResult(Level.WARN, "Unable to find convertion edge for  %s", method);
		}
		if (!m.ensureServiceAvailable()) {
			return new JobExecutionResult(Level.WARN, "service not running for method %s", method);
		}
		String soGuid = params.getJobGuid();
		VersionableObject so = (VersionableObject) session.getObjectFromGuid(soGuid);
		if (so == null) {
			return new JobExecutionResult(Level.WARN, "Unable to find system object for transformation %s", soGuid);
		}

		Content c = so.getContentByConstraint(params.getContentConstraint(), false);
		if (c == null) {
			return new JobExecutionResult(Level.WARN, "Unable to find content for system object for transformation %s",
					soGuid);
		}

		BaseFile sourceFile = c.getContentFile();
		BaseFile targetFile = null;
		try {
			String methodArgs = params.getTransformerMethodArgs();
			String templateGuid = params.getTemplateGuid();
			if (!StringUtil.nullOrEmptyString(templateGuid)) {
				Content templateContent = (Content) session.getObjectFromGuid(templateGuid);
				if (templateContent != null) {
					BaseFile templateFile = templateContent.getContentFile();
					if (BaseFile.notNullAndExists(templateFile)) {
						String templatePath = templateFile.getAbsolutePath();
						if (StringUtil.nullOrEmptyString(methodArgs)) {
							methodArgs = "_template_path=" + templatePath;
						} else {
							methodArgs += ",_template_path=" + templatePath;
						}
					}
				}
			}

			targetFile = m.convert(sourceFile, params.getJobId(), methodArgs, params.getNotifyGuid(), 0);
			if (!BaseFile.notNullAndExists(targetFile)) {
				return new JobExecutionResult(Level.ERROR,
						"Unable to convertToPdf %s file %s no output file generated.", soGuid, sourceFile);
			}
			if (params.getAddContentAsChildOfContent()) {
				long size = sourceFile.length();
				params.getContentSetter().setFile(targetFile, session, c, true);
			} else {
				params.getContentSetter().setFile(targetFile, session, null, true);
			}
		} catch (IOException ioe) {
			return new JobExecutionResult(Level.WARN, "Unable to convertToPdf %s file %s error %s", soGuid, sourceFile,
					ioe.getMessage());
		} catch (CategoryException ce) {
			return new JobExecutionResult(Level.WARN, "Unable to convertToPdf %s file %s error %s", soGuid, sourceFile,
					ce.getMessage());
		} catch (StoreException se) {
			return new JobExecutionResult(Level.WARN, "Unable to convertToPdf %s file %s error %s", soGuid, sourceFile,
					se.getMessage());
		} finally {
			// if (FileUtil.notNullAndExists(sourceFile))
			// {
			// sourceFile.delete();
			// }
			if (!BaseFile.notNullAndExists(targetFile)) {
				targetFile.delete();
			}
		}
		return null;
	}

}
