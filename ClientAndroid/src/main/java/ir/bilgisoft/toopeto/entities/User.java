package ir.bilgisoft.toopeto.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class User extends ir.bilgisoft.toopeto.xml.Element implements ListItem {

    private ir.bilgisoft.toopeto.entities.Account account;
    private Conversation mJoinedConversation;

    public User(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
        super("conference");
        this.setAttribute("jid", jid.toString());
        this.account = account;
    }

    private User(ir.bilgisoft.toopeto.entities.Account account) {
        super("conference");
        this.account = account;
    }

    public String getName() {
        return this.getAttribute("name");
    }

    public void setName(String name) {
        this.name = name;
    }

    public static User parse(ir.bilgisoft.toopeto.xml.Element element, ir.bilgisoft.toopeto.entities.Account account) {
        User user = new User(account);
        user.setAttributes(element.getAttributes());
        user.setChildren(element.getChildren());
        return user;
    }

    public void setAutojoin(boolean autojoin) {
        if (autojoin) {
            this.setAttribute("autojoin", "true");
        } else {
            this.setAttribute("autojoin", "false");
        }
    }

    @Override
    public boolean equals( final Object another)
    {
        return this.getDisplayName().equals(
                ((ListItem) another).getDisplayName());
    }
    @Override
    public int compareTo(final ListItem another) {
        return this.getDisplayName().compareToIgnoreCase(
                another.getDisplayName());
    }
//eslah nashode
    @Override
    public String getDisplayName() {
        if (this.mJoinedConversation != null
                && (this.mJoinedConversation.getMucOptions().getSubject() != null)) {
            return this.mJoinedConversation.getMucOptions().getSubject();
        } else if (getName() != null) {
            return getName();
        } else {
            return this.getJid().getLocalpart();
        }
    }
    //eslah nashode
    @Override
    public ir.bilgisoft.toopeto.xmpp.jid.Jid getJid() {
        return this.getAttributeAsJid("jid");
    }

    @Override
    public List<Tag> getTags() {
        ArrayList<Tag> tags = new ArrayList<Tag>();
        for (ir.bilgisoft.toopeto.xml.Element element : getChildren()) {
            if (element.getName().equals("group") && element.getContent() != null) {
                String group = element.getContent();
                tags.add(new Tag(group, ir.bilgisoft.toopeto.utils.UIHelper.getColorForName(group)));
            }
        }
        return tags;
    }

    public String getNick() {
        ir.bilgisoft.toopeto.xml.Element nick = this.findChild("nick");
        if (nick != null) {
            return nick.getContent();
        } else {
            return null;
        }
    }

    public void setNick(String nick) {
        ir.bilgisoft.toopeto.xml.Element element = this.findChild("nick");
        if (element == null) {
            element = this.addChild("nick");
        }
        element.setContent(nick);
    }

    public boolean autojoin() {
        return this.getAttributeAsBoolean("autojoin");
    }

    public String getPassword() {
        ir.bilgisoft.toopeto.xml.Element password = this.findChild("password");
        if (password != null) {
            return password.getContent();
        } else {
            return null;
        }
    }

    public void setPassword(String password) {
        ir.bilgisoft.toopeto.xml.Element element = this.findChild("password");
        if (element != null) {
            element.setContent(password);
        }
    }

    public boolean match(String needle) {
        if (needle == null) {
            return true;
        }
        needle = needle.toLowerCase(Locale.US);
        final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = getJid();
        return (jid != null && jid.toString().contains(needle)) ||
                getDisplayName().toLowerCase(Locale.US).contains(needle) ||
                matchInTag(needle);
    }

    private boolean matchInTag(String needle) {
        needle = needle.toLowerCase(Locale.US);
        for (Tag tag : getTags()) {
            if (tag.getName().toLowerCase(Locale.US).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public ir.bilgisoft.toopeto.entities.Account getAccount() {
        return this.account;
    }

    public Conversation getConversation() {
        return this.mJoinedConversation;
    }

    public void setConversation(Conversation conversation) {
        this.mJoinedConversation = conversation;
    }



    public void unregisterConversation() {
        if (this.mJoinedConversation != null) {
            this.mJoinedConversation.deregisterWithBookmark();
        }
    }
}
