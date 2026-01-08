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
package com.hitorro.basedms.commands;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.auth.AuthResponse;
import com.hitorro.util.auth.AuthenticationService;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.json.keys.StringProperty;


@CommandDef(command = "user.changepassword", description = "Change a users password")
public class ChangePasswordCommand extends Command {
    @CommandArgument(required = true)
    private StringProperty Username = new StringProperty("user", "username", "");
    @CommandArgument(required = true)
    private StringProperty Password = new StringProperty("password", "password", "");

    public boolean execute(String rawValue, JVS args, Response response, CommandSession session, RestOperations operation) throws Exception {
        AuthResponse resp = AuthenticationService.getService().changeUserPasswordSU(Username.apply(args),
                Password.apply(args));

        if (resp.isValid()) {
            this.writeSuccess(response, "Changed password for %s", Username.apply(args));
        } else {
            this.writeSimpleError(response, "Unable to change  password for %s", Username.apply(args));
        }
        return true;
    }
}
