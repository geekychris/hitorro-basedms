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
package com.hitorro.basedms.ssh.scp;

import com.hitorro.basedms.ssh.HostsUtil;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.ScpFile,
        isView = false,
        isPersisted = false,
        schemaVersion = ScpFile.SerializationVersion)
public class ScpFile implements HTSerializable {
    public static final int SerializationVersion = 1;
    Direction m_direction = Direction.unknown;
    String m_host;
    int m_port = 22;
    boolean trust = true;
    String m_user;
    String m_password;
    String sourceFilePathName;
    String targetBaseFilePath;
    String targetFilePathName;
    public ScpFile() {
    }


    public ScpFile(Direction direction, String host, int port, boolean trust, String user, String password, String sourceFilepathName, String targetFilepathName) {
        m_direction = direction;
        m_host = host;
        m_port = port;
        this.trust = trust;
        m_user = user;
        m_password = password;
        sourceFilePathName = sourceFilepathName;
        targetBaseFilePath = targetFilepathName;
        targetFilePathName = targetFilepathName;
    }


    public ScpFile(Direction direction, JVS transferProperties, String sourceFilepathName, String targetFilePathName) {
        m_direction = direction;
        m_host = HostsUtil.HostKey.apply(transferProperties.getJsonNode());
        m_port = HostsUtil.PortKey.apply(transferProperties.getJsonNode());
        trust = HostsUtil.TrustKey.apply(transferProperties.getJsonNode());
        m_user = HostsUtil.UserKey.apply(transferProperties.getJsonNode());
        m_password = HostsUtil.PasswordKey.apply(transferProperties.getJsonNode());
        targetBaseFilePath = HostsUtil.TargetBasePathKey.apply(transferProperties.getJsonNode());
        this.targetFilePathName = targetFilePathName;
        sourceFilePathName = sourceFilepathName;
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());

        os.writeString(getDirection().toString());
        os.writeString(getHost());
        os.writeInt(getPort());
        os.writeBoolean(getTrust());
        os.writeString(getUser());
        os.writeString(getPassword());
        os.writeString(getSourceFilepathName());
        os.writeString((getTargetBaseFilePath()));
        os.writeString(getTargetFilepathName());
    }

    public void deserialize(HTObjectInputStream is) throws IOException, ClassNotFoundException, StoreException {
        int version = is.readInt();
        switch (version) {
            case 1:
                setDirection(Direction.getDirection(is.readString()));
                setHost(is.readString());
                setPort(is.readInt());
                setTrust(is.readBoolean());
                setUser(is.readString());
                setPassword(is.readString());
                setSourceFilepathName(is.readString());
                setTargetBaseFilePath(is.readString());
                setTargetFilePathName(is.readString());
        }
    }

    public void copy(ScpFile source) {
        m_direction = source.m_direction;

        m_host = source.m_host;
        m_port = source.m_port;
        trust = source.trust;
        m_user = source.m_user;
        m_password = source.m_password;
        sourceFilePathName = source.sourceFilePathName;
        targetBaseFilePath = source.targetBaseFilePath;
        targetFilePathName = source.targetFilePathName;
    }

    public int getSerializationVersion() {
        return 1;
    }

    public boolean isPersisted() {
        return false;
    }

    public boolean hasGuid() {
        return false;
    }

    public boolean hasSoftGuid() {
        return false;
    }

    public Direction getDirection() {
        return m_direction;
    }

    public void setDirection(Direction direction) {
        m_direction = direction;
    }

    public String getHost() {
        return m_host;
    }

    public void setHost(String host) {
        m_host = host;
    }

    public int getPort() {
        return m_port;
    }

    public void setPort(int port) {
        m_port = port;
    }

    public boolean getTrust() {
        return trust;
    }

    public void setTrust(boolean trust) {
        this.trust = trust;
    }

    public String getUser() {
        return m_user;
    }

    public void setUser(String user) {
        m_user = user;
    }

    public String getPassword() {
        return m_password;
    }

    public void setPassword(String password) {
        m_password = password;
    }

    public String getSourceFilepathName() {
        return sourceFilePathName;
    }

    public void setSourceFilepathName(String sourceFilepathName) {
        sourceFilePathName = sourceFilepathName;
    }

    public String getTargetBaseFilePath() {
        return targetBaseFilePath;
    }

    public void setTargetBaseFilePath(String targetBaseFilePath) {
        this.targetBaseFilePath = targetBaseFilePath;
    }

    public String getTargetFilepathName() {
        return targetFilePathName;
    }

    public void setTargetFilePathName(String targetFilePathName) {
        this.targetFilePathName = targetFilePathName;
    }


    public enum Direction {
        unknown("unknown"), scpto("scpto"), scpfrom("scpfrom");

        private static Map<String, Direction> s_map = null;
        private String m_direction;


        Direction(String direction) {
            m_direction = direction;
        }

        public static Direction getDirection(String direction) {
            if (s_map == null) {
                s_map = new HashMap<String, Direction>();

                for (Direction direct : Direction.values()) {
                    s_map.put(direct.toString().toLowerCase(), direct);
                }
            }
            return s_map.get(direction.toLowerCase());
        }

        public String toString() {
            return m_direction;
        }


    }


}
