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
package com.hitorro.basedms;


public class ContentPropertiesImpl {
    private int m_width = 0;
    private int m_height = 0;
    private int m_bitRate = 0;
    private int m_durationSeconds = 0;
    private String m_codec;
    private long m_contentSize = 0;
    private String m_resolutionAux = new String();

    public String getCodec() {
        return m_codec;
    }

    public void setCodec(String codec) {
        m_codec = codec;
    }

    public long getContentSize() {
        return m_contentSize;
    }

    public void setContentSize(long contentSize) {
        m_contentSize = contentSize;
    }

    public int getBitRate() {
        return m_bitRate;
    }

    public void setBitRate(int bitRate) {
        m_bitRate = bitRate;
    }

    public int getDurationSeconds() {
        return m_durationSeconds;
    }

    public void setDurationSeconds(int dur) {
        m_durationSeconds = dur;
    }

    public int getWidth() {
        return m_width;
    }

    public void setWidth(int width) {
        m_width = width;
    }

    public int getHeight() {
        return m_height;
    }

    public void setHeight(int height) {
        m_height = height;
    }

    /**
     * Resolution of the content if that is known (such as 48kbs for cd quality audio, 320x240)
     *
     * @return
     */
    public String getResolutionAux() {
        return m_resolutionAux;
    }

    public void setResolutionAux(String resolution) {
        m_resolutionAux = resolution;
    }

}
