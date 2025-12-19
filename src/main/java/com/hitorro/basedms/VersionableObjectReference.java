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

import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.basedms.session.DMSSession;


/**
 * Hold a reference to an object, which may be the object, or a guid. Holding an object for long periods of time is
 * fraught with peril.  Instead, we should keep the guid and use it to refetch the object safely.  But newly created
 * objects, or objects which are not VersionableObjects (for instance non-persisted java objects) can't be handled that
 * way.  So in those cases we need to hang onto the actual object.  This class provides a handy way to abstract that
 * different behavior.
 */
public class VersionableObjectReference {
    private Object _obj;
    private String _guid;
    private boolean _useGuid;

    /**
     * Construct the reference. If obj is a VersionableObject we will store by guid.
     *
     * @param obj Object we're storing a reference to.
     */
    public VersionableObjectReference(Object obj) {
        this(obj, obj instanceof VersionableObject);
    }

    /**
     * Construct the reference, controlling whether or not to use the guid. If possible, VersionableObjects should be
     * stored by guid but that is not possible for newly created objects. Use this routine to explicitly store newly
     * created objects as objects.
     *
     * @param obj     Object we're storing a reference to.
     * @param useGuid if true, use guid for reference.  If false, use the object itself.
     */
    public VersionableObjectReference(Object obj, boolean useGuid) {
        _useGuid = useGuid;
        if (_useGuid) {
            _guid = ((VersionableObject) obj).getGuid();
        } else {
            _obj = obj;
        }
    }

    public Object getObject(DMSSession session) {
        if (_useGuid) {
            return session.getObjectFromGuid(_guid);
        } else {
            return _obj;
        }
    }

    /**
     * Tell the reference that it should start using the guid. If an object had not been persisted, but has just been
     * persisted, we should start referring to it via the guid.  This method allows that conversion to take place.
     */
    public void useGuidNow() {
        if (!_useGuid && (_obj instanceof GuidBaseType)) {
            _useGuid = true;
            _guid = ((GuidBaseType) _obj).getGuid();
        }
    }
}
