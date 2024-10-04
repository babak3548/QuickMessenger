package ir.bilgisoft.toopeto.xmpp.stanzas;

public class AbstractStanza extends ir.bilgisoft.toopeto.xml.Element {

	protected AbstractStanza(final String name) {
		super(name);
	}

	public ir.bilgisoft.toopeto.xmpp.jid.Jid getTo() {
		return getAttributeAsJid("to");
	}

	public ir.bilgisoft.toopeto.xmpp.jid.Jid getFrom() {
		return getAttributeAsJid("from");
	}

	public String getId() {
		return this.getAttribute("id");
	}

	public void setTo(final ir.bilgisoft.toopeto.xmpp.jid.Jid to) {
		if (to != null) {
			setAttribute("to", to.toString());
		}
	}

	public void setFrom(final ir.bilgisoft.toopeto.xmpp.jid.Jid from) {
		if (from != null) {
			setAttribute("from", from.toString());
		}
	}

	public void setId(final String id) {
		setAttribute("id", id);
	}

	public boolean fromServer(final ir.bilgisoft.toopeto.entities.Account account) {
		return getFrom() == null
			|| getFrom().equals(account.getServer())
			|| getFrom().equals(account.getJid().toBareJid())
			|| getFrom().equals(account.getJid());
	}

	public boolean toServer(final ir.bilgisoft.toopeto.entities.Account account) {
		return getTo() == null
			|| getTo().equals(account.getServer())
			|| getTo().equals(account.getJid().toBareJid())
			|| getTo().equals(account.getJid());
	}
}
