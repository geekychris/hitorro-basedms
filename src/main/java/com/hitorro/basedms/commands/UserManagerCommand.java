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

import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.User;
import com.hitorro.base.objects.UserMark;
import com.hitorro.base.objects.UserPreferences;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ResponseShape;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.DoubleProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;

import java.util.Date;
import java.util.List;

/**
 * Create Activate validate delete getAllDetails setpassword
 * <p/>
 * Here are the following commands.  You can issue them from a telnet session or execute them from the REST interface:
 * such as: http://localhost:8072/internal/action/manageuser?operation=create&name=p1&%20password=p1password&hash=1234&displayname=&P1&activationtoken=p1tok&email=p1@HiTorro.net
 * <p/>
 * where you would get a response like:
 * <p/>
 * <manageuser> <usermanagerresult> <c>OK</c> <de>Created</de> </usermanagerresult> </manageuser>
 * <p/>
 * create: > manageuser operation="create"  name="p1" password="p1password" hash="1234" displayname="P1"
 * activationtoken="p1tok" email="p1@HiTorro.net" Code  Description OK    Created details: > manageuser operation
 * ="details" name="p1";
 * <p/>
 * Name  Password    Hash  DisplayName  ActivationToken  isActive p1    p1password  1234  P1           p1tok false
 * <p/>
 * validate hash: > manageuser operation=validate name="p1" hash="1234"; Code  Description OK    Valid
 * <p/>
 * activate: > manageuser operation=activate name="p1" activationtoken="p1tok";
 * <p/>
 * change password: > manageuser operation=setpassword name="p1" hash="1234" newpassword="p2" newhash=4321; Code
 * Description OK    Valid
 * <p/>
 * > manageuser operation ="details" name="p1"; Name  Password  Hash  DisplayName  ActivationToken  isActive p1    p2
 * 4321  P1           p1tok            true
 * <p/>
 * delete user: > manageuser operation ="delete" name="p1"; Info: deleted
 * <p/>
 * >manageuser operation ="details" name="p1"; Code   Description Error  User p1 does not exist to perform operation
 * details
 * <p/>
 * manageuser operation="listbookmarks" name="p1";
 * <p/>
 * > manageuser operation="addbookmark" name="chris" id="34:4bd9f:65f75" type="bookmark" score="7.9" description="scoble
 * rules the world sometimes";
 */
@CommandDef(command = "user.manage", description = "Manage a user account")
public class UserManagerCommand extends Command {
    public static final String Create = "create";
    public static final String Activate = "activate";
    public static final String SetPassword = "setpassword";
    public static final String Validate = "validate";
    public static final String Delete = "delete";
    public static final String Details = "details";
    public static final String ListBookmarks = "listbookmarks";
    public static final String AddBookmark = "addbookmark";
    public static final String All = Create + "|" + Activate + "|" + SetPassword + "|" + Validate + "|" + Delete + "|" + Details;
    private static final String OK = "OK";
    private static final String NACK = "Error";
    @CommandArgument(required = true)
    private StringProperty Operation = new StringProperty("operation", "operation to perform", All);
    @CommandArgument(required = true)
    private StringProperty UserName = new StringProperty("name", "User Name", null);
    @CommandArgument(required = false)
    private StringProperty Password = new StringProperty("password", "password in clear text", null);
    @CommandArgument(required = false)
    private StringProperty NewHash = new StringProperty("newhash", "password hash", null);
    @CommandArgument(required = false)
    private StringProperty NewPassword = new StringProperty("newpassword", "password in clear text", null);
    @CommandArgument(required = false)
    private StringProperty Hash = new StringProperty("hash", "password hash", null);
    @CommandArgument(required = false)
    private StringProperty DisplayName = new StringProperty("displayname", "Display Foundation", null);
    @CommandArgument(required = false)
    private StringProperty ActivationToken = new StringProperty("activationtoken", "Token to activate an account", null);
    @CommandArgument(required = false)
    private StringProperty EmailAddress = new StringProperty("email", "email address", null);
    @CommandArgument(required = false)
    private StringProperty Type = new StringProperty("type", "bookmark type", null);
    @CommandArgument(required = false)
    private StringProperty Id = new StringProperty("id", "bookmark type", null);
    @CommandArgument(required = false)
    private StringProperty IdType = new StringProperty("idtype", "type of id", "guid");
    @CommandArgument(required = false)
    private DoubleProperty Score = new DoubleProperty("score", "bookmark type", 0.0);
    @CommandArgument(required = false)
    private StringProperty Description = new StringProperty("description", "bookmark description", null);
    @CommandArgument(required = false)

    @ResponseDefinition(command = "details",
            rowname = "detail",
            columns = {@RespColumn(name = "Name", lName = "class"),
                    @RespColumn(name = "Password", lName = "password"),
                    @RespColumn(name = "Hash", lName = "hash"),
                    @RespColumn(name = "Display Name", lName = "dispname"),
                    @RespColumn(name = "Active Token", lName = "activetoken"),
                    @RespColumn(name = "Is Active", lName = "isactive")})
    private ResponseShape detailsHeader = new ResponseShape();
    @ResponseDefinition(command = "bookmarks",
            rowname = "detail",
            columns = {@RespColumn(name = "Type", lName = "type"),
                    @RespColumn(name = "DateAdded", lName = "dateadded"),
                    @RespColumn(name = "Score", lName = "score"),
                    @RespColumn(name = "Description", lName = "desc"),
                    @RespColumn(name = "ID", lName = "id")})
    private ResponseShape bookmarksHeader = new ResponseShape();
    @ResponseDefinition(command = "code",
            rowname = "row",
            columns = {@RespColumn(name = "Code", lName = "code"),
                    @RespColumn(name = "Description", lName = "desc")})
    private ResponseShape codeHeader = new ResponseShape();

    public boolean execute(String rawValue, JsonNode args, Response response, CommandSession session) throws Exception {
        String operation = Operation.apply(args);
        String user = UserName.apply(args);

        String hash = Hash.apply(args);

        if (StringUtil.nullOrEmptyString(operation)) {
            writeResponse(response, NACK, "No operation provided");
            this.writeSimpleError(response, "No operation provided");
            return false;
        }
        if (StringUtil.nullOrEmptyString(user)) {
            writeResponse(response, NACK, "User not provided", operation);
            return false;
        }
        BaseSession sess = DMSSessionFactory.getFactory().getSession();
        User u = User.getUserForName(sess, user);
        try {
            if (Create.equalsIgnoreCase(operation)) {
                return createUser(u, response, user, args, hash, sess);
            } else {
                if (u == null) {
                    writeResponse(response, NACK, "User %s does not exist to perform operation %s", user, operation);
                    return false;
                }
                if (Activate.equalsIgnoreCase(operation)) {
                    activate(args, u, sess, response);
                } else if (SetPassword.equalsIgnoreCase(operation)) {
                    if (setPassword(u, hash, args, sess, response, operation)) {
                        return false;
                    }
                } else if (Validate.equalsIgnoreCase(operation)) {
                    if (u.getPasswordHash().equals(hash)) {
                        writeResponse(response, OK, "Valid");
                    } else {
                        writeResponse(response, NACK, "User: %s has invalid password", operation);
                        return false;
                    }
                } else if (Delete.equalsIgnoreCase(operation)) {
                    u.delete(sess);
                    sess.commit();
                    this.writeSuccess(response, "deleted");
                } else if (Details.equalsIgnoreCase(operation)) {
                    writeDetailsResponse(response, u);
                } else if (ListBookmarks.equalsIgnoreCase(operation)) {
                    writeBookmarksResponse(response, u);
                } else if (AddBookmark.equalsIgnoreCase(operation)) {
                    if (addBookmark(args, response, u, sess)) {
                        return false;
                    }

                } else {
                    writeResponse(response, NACK, "Operation invalid: %s", operation);
                }
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(sess);
        }

        return false;

    }

    private boolean createUser(User u, Response response, String user, JsonNode args, String hash, BaseSession sess) {
        if (u != null) {
            writeResponse(response, NACK, "User: %s exists already, cant create.", user);
            return false;
        }
        String pwd = Password.apply(args);
        String tok = ActivationToken.apply(args);
        String dn = DisplayName.apply(args);
        String email = EmailAddress.apply(args);
        u = new User();
        u.setName(user);
        u.setPassword(pwd);
        u.setPasswordHash(hash);
        u.setActivationToken(tok);
        u.setDisplayName(dn);
        u.setEmailAddress(email);
        u.setState(Constants.InactiveState);
        sess.persist(u);
        sess.commit();
        writeResponse(response, OK, "Created");
        return true;
    }

    private void activate(JsonNode args, User u, BaseSession sess, Response response) {
        String tok = ActivationToken.apply(args);
        if (u.getActivationToken().equals(tok)) {
            u.setState(Constants.ActiveState);
            sess.commit();
            writeResponse(response, OK, "Activated");
        } else {
            writeResponse(response, NACK, "activation token invalid");
        }
    }

    private boolean setPassword(User u, String hash, JsonNode args, BaseSession sess, Response response, String operation) {
        if (u.getPasswordHash().equals(hash)) {
            String newHash = NewHash.apply(args);
            String newPassword = NewPassword.apply(args);
            u.setPassword(newPassword);
            u.setPasswordHash(newHash);
            sess.commit();
            writeResponse(response, OK, "Valid");
        } else {
            writeResponse(response, NACK, "User: %s has invalid password", operation);
            return true;
        }
        return false;
    }

    private boolean addBookmark(JsonNode args, Response response, User u, BaseSession sess) {
        String id = Id.apply(args);
        String idType = IdType.apply(args);
        String desc = Description.apply(args);
        String type = Type.apply(args);
        double score = Score.apply(args);
        UserPreferences.UserMarkType t = UserPreferences.UserMarkType.getTypeName(type);
        if (t == null) {
            writeResponse(response, NACK, "Type %s is not valid", type);
            return true;
        }
        UserPreferences.IdType idt = UserPreferences.IdType.getTypeName(idType);
        if (idt == null) {
            writeResponse(response, NACK, "ID type %s is not valid", idType);
            return true;
        }
        UserPreferences up = u.getUserPreferences();
        UserMark um = new UserMark();
        um.setType(t.getOrdinal());
        um.setId(id);
        um.setScore(score);
        um.setDescription(desc);
        um.setAddDate(new Date());
        up.getUserMarks().add(um);
        Content c = u.setUserPreferences(up);
        sess.saveOrUpdate(u);
        sess.commit();
        writeResponse(response, OK, "Updated");
        return false;
    }

    private void writeDetailsResponse(Response r, User u) {
        r.setResponseShape(detailsHeader);
        String t;
        r.addRow(u.getName(), u.getPassword(), u.getPasswordHash(), u.getDisplayName(), u.getActivationToken(), u.isActive());
        r.end();
    }

    private void writeBookmarksResponse(Response r, User u) {
        r.setResponseShape(bookmarksHeader);
        String t;
        UserPreferences up = u.getUserPreferences();
        if (up != null) {
            List<UserMark> l = up.getUserMarks();
            for (UserMark um : l) {
                r.addRow(um.getTypeName(), um.getAddDate(), um.getScore(), um.getDescription(), um.getId());
                r.addRow();

            }
        }
        r.end();
    }

    private void writeResponse(Response r, String code, String description, String... args) {
        r.setResponseShape(codeHeader);
        String t;
        if (args != null && args.length > 0) {
            t = Fmt.Sargs(description, args);
        } else {
            t = description;
        }
        r.addRow(code, t);
        r.end();
    }
}
