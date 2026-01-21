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
package com.hitorro.basedms.transformer.methods;

import com.hitorro.basedms.transformer.Log;
import com.hitorro.basedms.transformer.TransformMethod;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Generate thumbnail images from video files using FFmpeg
 * Supports timestamp selection, resolution, and format options
 */
public class VideoThumbnailTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "video_thumbnail";

    private static final StringProperty FFmpegPath = new StringProperty(
            "transformer.ffmpeg.path",
            "Path to ffmpeg executable",
            "ffmpeg");

    private static final IntegerProperty DefaultWidth = new IntegerProperty(
            "transformer.video.thumbnail.width",
            "Default thumbnail width",
            640);

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = FFmpegPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "-version" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("FFmpeg not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("Video thumbnail generation requires local file system");
        }

        File sourceVideo = ((FileFile) sourceFile).getJavaFile();
        if (!sourceVideo.exists()) {
            throw new IOException("Source video file does not exist: " + sourceVideo.getAbsolutePath());
        }

        // Parse parameters:
        // timestamp=00:00:05,width=640,height=480,format=jpg,quality=85
        String timestamp = getParameter(parameters, "timestamp", "00:00:05");
        String width = getParameter(parameters, "width", String.valueOf(DefaultWidth.apply()));
        String height = getParameter(parameters, "height", null);
        String format = getParameter(parameters, "format", "jpg").toLowerCase();
        String quality = getParameter(parameters, "quality", "85");

        // Determine output format and extension
        String extension;
        switch (format) {
            case "png":
                extension = "png";
                break;
            case "jpeg":
            case "jpg":
            default:
                extension = "jpg";
                break;
        }

        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("video_thumb_%s.%s", id, extension));

        try {
            // Build ffmpeg command
            // -ss: seek to timestamp
            // -i: input file
            // -vframes 1: extract one frame
            // -vf scale: resize
            // -q:v: quality (for JPEG, 2-31, lower is better)
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(FFmpegPath.apply());

            // Seek to timestamp (before input for faster seeking)
            command.add("-ss");
            command.add(timestamp);

            // Input file
            command.add("-i");
            command.add(sourceVideo.getAbsolutePath());

            // Extract one frame
            command.add("-vframes");
            command.add("1");

            // Scale filter
            String scaleFilter;
            if (height != null && !height.isEmpty()) {
                scaleFilter = Fmt.S("scale=%s:%s", width, height);
            } else {
                scaleFilter = Fmt.S("scale=%s:-1", width); // Maintain aspect ratio
            }
            command.add("-vf");
            command.add(scaleFilter);

            // Quality for JPEG
            if (extension.equals("jpg")) {
                command.add("-q:v");
                // Convert quality percentage to ffmpeg scale (2-31, lower is better)
                int q = Integer.parseInt(quality);
                int ffmpegQuality = 31 - ((q * 29) / 100); // Map 0-100 to 31-2
                command.add(String.valueOf(Math.max(2, Math.min(31, ffmpegQuality))));
            }

            // Overwrite output file
            command.add("-y");

            // Output file
            command.add(outputFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Log.transformer.info("Executing: %s", String.join(" ", command));

            Process process = pb.start();

            // Capture output for debugging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    Log.transformer.debug("FFmpeg: %s", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("FFmpeg thumbnail generation failed with exit code %d: %s",
                        exitCode, output.toString()));
            }

            if (!outputFile.exists()) {
                throw new IOException("Thumbnail was not created: " + outputFile.getAbsolutePath());
            }

            Log.transformer.info("Successfully generated video thumbnail (timestamp=%s, size=%sx%s): %s",
                    timestamp, width, height != null ? height : "auto", outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Video thumbnail generation interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to generate video thumbnail: " + e.getMessage(), e);
        }
    }
}
