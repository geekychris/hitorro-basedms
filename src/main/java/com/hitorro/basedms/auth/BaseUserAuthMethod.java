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

import com.hitorro.base.objects.User;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.auth.AbstractUser;
import com.hitorro.util.auth.AuthResponse;
import com.hitorro.util.auth.AuthenticationMethod;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;


public class BaseUserAuthMethod implements AuthenticationMethod {
    /**
     * global password
     */
    public static StringProperty Password =
            new StringProperty("ui.password", "cheapo authentication", "zz");
    public static BooleanProperty PasswordBackdoor =
            new BooleanProperty("ui.passwordbackdoor", "switch off auth", false);
    public static final String Key = "baseuserauth";


    public BaseUserAuthMethod() {

    }

    public boolean canSetPassord() {
        return true;
    }

    public AuthResponse changePasswordSU(String userName, String newPassword) {
        return changePassword(userName, null, newPassword, false);
    }

    public AuthResponse changePassword(String userName, String password, String newPassword) {
        return changePassword(userName, password, newPassword, true);

    }

    private AuthResponse changePassword(String userName, String password, String newPassword, boolean validate) {
        // see if that name is known in the database
        // no password check at present
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {

            User user = User.getUserForName(session, userName);

            // if the user doesn't have a password, that indicates that the user shouldn't be allowed to log in
            if (user == null || user.getPassword() == null) {
                // we didn't test a known user
                return new BaseAuthResponse(AuthResponse.AuthResponseState.Invalid, userName, false);
            } else {
                if (!validate || isPasswordValid(user, password)) {
                    BaseAuthResponse bar = new BaseAuthResponse(AuthResponse.AuthResponseState.Valid, userName, user);
                    user.setPassword(newPassword);
                    session.commit();
                    return bar;
                } else {
                    return new BaseAuthResponse(AuthResponse.AuthResponseState.Invalid, userName, true);
                }
                // set authentication in session
            }

        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }
    }

    public String getAuthenticationMethod() {
        return Key;
    }

    public AuthResponse authenticate(String userName, String password) {
        // see if that name is known in the database
        // no password check at present
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {

            User user = User.getUserForName(session, userName);

            // if the user doesn't have a password, that indicates that the user shouldn't be allowed to log in
            if (user == null || user.getPassword() == null) {
                // we didn't test a known user
                return new BaseAuthResponse(AuthResponse.AuthResponseState.Invalid, userName, false);
            } else {
                if (isPasswordValid(user, password)) {
                    BaseAuthResponse bar = new BaseAuthResponse(AuthResponse.AuthResponseState.Valid, userName, user);
                    return bar;
                } else {
                    return new BaseAuthResponse(AuthResponse.AuthResponseState.Invalid, userName, true);
                }
                // set authentication in session
            }

        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }


    }

    private boolean isPasswordValid(User user, String password) {
        String pwd = Password.apply();
        if (PasswordBackdoor.apply() == true) {
            return pwd.equals(password);
        }
        return user.getPassword().equals(password);
    }

    public AbstractUser getAbstractUserInfo(String userId) {
        return null;
    }

    public boolean isTokenGenerating() {
        return false;
    }


}