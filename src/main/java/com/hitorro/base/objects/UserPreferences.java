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
package com.hitorro.base.objects;

import gnu.trove.map.hash.TIntObjectHashMap;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Sep 30, 2005 Time: 10:01:28 AM
 * <p/>
 * Serialized object that tracks user prefrences, such as bookmarks, playlists, subscriptions
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.UserPreferences,
        isView = false,
        isPersisted = false,
        schemaVersion = UserPreferences.SerialVersion)
public class UserPreferences implements HTSerializable {
    public static final int SerialVersion = 1;

    private List<UserMark> userMarks = new ArrayList<UserMark>();


    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        os.writeListOfHTSerializable(getUserMarks());
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        os.readListOfHTSerializable(getUserMarks());
    }

    public int getSerializationVersion() {
        return SerialVersion;
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

    public List<UserMark> getUserMarks() {
        return userMarks;
    }

    public void setUserMarks(List<UserMark> userMarks) {
        this.userMarks = userMarks;
    }

    public enum UserMarkType {
        Bookmark("bookmark", 1),
        Playlist("playlist", 2);

        private static HashMap<String, UserMarkType> s_byShortName;
        private static TIntObjectHashMap m_ords;
        private String m_name;
        private int m_ordinal;

        UserMarkType(String name, int ord) {
            m_name = name.toLowerCase();
            m_ordinal = ord;
            setMapEntry(this);

        }

        public static UserMarkType getTypeName(String name) {
            return s_byShortName.get(name.toLowerCase());
        }

        public static int size() {
            return s_byShortName.size();
        }

        public static UserMarkType getByOrdinal(int ord) {
            return (UserMarkType) m_ords.get(ord);
        }

        private static void setMapEntry(UserMarkType filter) {
            if (s_byShortName == null) {
                s_byShortName = new HashMap<String, UserMarkType>();
            }
            s_byShortName.put(filter.getName(), filter);

            if (m_ords == null) {
                m_ords = new TIntObjectHashMap();
            }
            m_ords.put(filter.m_ordinal, filter);
        }

        public String getName() {
            return m_name;
        }

        public int getOrdinal() {
            return m_ordinal;
        }
    }


    public enum IdType {
        Bookmark("guid", 1),
        Playlist("queryparams", 2);

        private static HashMap<String, IdType> s_byShortName;
        private static TIntObjectHashMap m_ords;
        private String m_name;
        private int m_ordinal;

        IdType(String name, int ord) {
            m_name = name.toLowerCase();
            m_ordinal = ord;
            setMapEntry(this);

        }

        public static IdType getTypeName(String name) {
            return s_byShortName.get(name.toLowerCase());
        }

        public static int size() {
            return s_byShortName.size();
        }

        public static IdType getByOrdinal(int ord) {
            return (IdType) m_ords.get(ord);
        }

        private static void setMapEntry(IdType filter) {
            if (s_byShortName == null) {
                s_byShortName = new HashMap<String, IdType>();
            }
            s_byShortName.put(filter.getName(), filter);

            if (m_ords == null) {
                m_ords = new TIntObjectHashMap();
            }
            m_ords.put(filter.m_ordinal, filter);
        }

        public String getName() {
            return m_name;
        }

        public int getOrdinal() {
            return m_ordinal;
        }
    }

}

