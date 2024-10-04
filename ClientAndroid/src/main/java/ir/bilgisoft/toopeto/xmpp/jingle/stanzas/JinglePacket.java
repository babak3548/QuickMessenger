package ir.bilgisoft.toopeto.xmpp.jingle.stanzas;

public class JinglePacket extends ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket {
	ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content content = null;
	ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Reason reason = null;
	ir.bilgisoft.toopeto.xml.Element jingle = new ir.bilgisoft.toopeto.xml.Element("jingle");

	@Override
	public ir.bilgisoft.toopeto.xml.Element addChild(ir.bilgisoft.toopeto.xml.Element child) {
		if ("jingle".equals(child.getName())) {
			ir.bilgisoft.toopeto.xml.Element contentElement = child.findChild("content");
			if (contentElement != null) {
				this.content = new ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content();
				this.content.setChildren(contentElement.getChildren());
				this.content.setAttributes(contentElement.getAttributes());
			}
			ir.bilgisoft.toopeto.xml.Element reasonElement = child.findChild("reason");
			if (reasonElement != null) {
				this.reason = new ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Reason();
				this.reason.setChildren(reasonElement.getChildren());
				this.reason.setAttributes(reasonElement.getAttributes());
			}
			this.jingle.setAttributes(child.getAttributes());
		}
		return child;
	}

	public JinglePacket setContent(ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content content) {
		this.content = content;
		return this;
	}

	public ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content getJingleContent() {
		if (this.content == null) {
			this.content = new ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content();
		}
		return this.content;
	}

	public JinglePacket setReason(ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Reason reason) {
		this.reason = reason;
		return this;
	}

	public ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Reason getReason() {
		return this.reason;
	}

	private void build() {
		this.children.clear();
		this.jingle.clearChildren();
		this.jingle.setAttribute("xmlns", "urn:xmpp:jingle:1");
		if (this.content != null) {
			jingle.addChild(this.content);
		}
		if (this.reason != null) {
			jingle.addChild(this.reason);
		}
		this.children.add(jingle);
		this.setAttribute("type", "set");
	}

	public String getSessionId() {
		return this.jingle.getAttribute("sid");
	}

	public void setSessionId(String sid) {
		this.jingle.setAttribute("sid", sid);
	}

	@Override
	public String toString() {
		this.build();
		return super.toString();
	}

	public void setAction(String action) {
		this.jingle.setAttribute("action", action);
	}

	public String getAction() {
		return this.jingle.getAttribute("action");
	}

	public void setInitiator(final ir.bilgisoft.toopeto.xmpp.jid.Jid initiator) {
		this.jingle.setAttribute("initiator", initiator.toString());
	}

	public boolean isAction(String action) {
		return action.equalsIgnoreCase(this.getAction());
	}
}
