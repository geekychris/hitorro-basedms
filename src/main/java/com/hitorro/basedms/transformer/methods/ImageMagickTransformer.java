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
import com.hitorro.util.json.keys.StringProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Convert images using ImageMagick convert command
 * Supports resizing, format conversion, compression, etc.
 */
public class ImageMagickTransformer implements TransformMethod {
    public static final String METHOD_NAME = "imagemagick_convert";
    
    private static final StringProperty ConvertPath = new StringProperty(
            "transformer.imagemagick.path",
            "Path to ImageMagick convert executable",
            "convert");

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = ConvertPath.apply();
            Process p = Runtime.getRuntime().exec(new String[]{cmd, "-version"});
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("ImageMagick not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid, int maxWaitTimeMinutes) 
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("ImageMagick conversion requires local file system");
        }
        
        File sourceImage = ((FileFile) sourceFile).getJavaFile();
        if (!sourceImage.exists()) {
            throw new IOException("Source image does not exist: " + sourceImage.getAbsolutePath());
        }

        // Parse parameters: format=jpg,width=800,height=600,quality=85,resize=50%
        String format = getParameter(parameters, "format", "jpg").toLowerCase();
        String width = getParameter(parameters, "width", null);
        String height = getParameter(parameters, "height", null);
        String quality = getParameter(parameters, "quality", null);
        String resize = getParameter(parameters, "resize", null);
        
        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("img_conv_%s.%s", id, format));
        
        try {
            // Build ImageMagick command
            List<String> command = new ArrayList<>();
            command.add(ConvertPath.apply());
            command.add(sourceImage.getAbsolutePath());
            
            // Add resize options
            if (resize != null && !resize.isEmpty()) {
                command.add("-resize");
                command.add(resize);
            } else if (width != null || height != null) {
                String geometry = "";
                if (width != null) {
                    geometry += width;
                }
                if (height != null) {
                    geometry += "x" + height;
                } else {
                    geometry += "x";
                }
                command.add("-resize");
                command.add(geometry);
            }
            
            // Add quality for lossy formats
            if (quality != null && !quality.isEmpty()) {
                command.add("-quality");
                command.add(quality);
            }
            
            // Add output file
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
                    Log.transformer.debug("ImageMagick: %s", line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException(Fmt.S("ImageMagick conversion failed with exit code %d: %s", 
                        exitCode, output.toString()));
            }
            
            if (!outputFile.exists()) {
                throw new IOException("Output file was not created by ImageMagick");
            }
            
            Log.transformer.info("Successfully converted image to %s: %s", format, outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ImageMagick conversion interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to convert image: " + e.getMessage(), e);
        }
    }
    
    private String getParameter(String parameters, String key, String defaultValue) {
        if (parameters == null || parameters.isEmpty()) {
            return defaultValue;
        }
        
        for (String param : parameters.split(",")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(key)) {
                return parts[1].trim();
            }
        }
        
        return defaultValue;
    }
}
