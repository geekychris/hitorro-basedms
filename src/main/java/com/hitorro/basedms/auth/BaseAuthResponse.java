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
package com.hitorro.basedms.auth;

import com.hitorro.base.objects.Permission;
import com.hitorro.base.objects.User;
import com.hitorro.util.auth.AuthResponse;
import com.hitorro.util.auth.PermissionInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class BaseAuthResponse extends AuthResponse {
    private List<PermissionInterface> perms;

    public BaseAuthResponse(AuthResponseState state, String userId, boolean userExists) {
        super(state, userId, userExists);
    }

    public BaseAuthResponse(AuthResponseState state, String userId, User user) {
        super(state, userId, true);
        perms = new ArrayList<PermissionInterface>();
        for (Permission p : user.getAllPermissions()) {
            perms.add(p);
        }
    }

    public List<PermissionInterface> getPermissions() {
        return perms;
    }
}
