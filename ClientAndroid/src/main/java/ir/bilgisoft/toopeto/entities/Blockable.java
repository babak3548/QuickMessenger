package ir.bilgisoft.toopeto.entities;

public interface Blockable {
	public boolean isBlocked();
	public boolean isDomainBlocked();
	public ir.bilgisoft.toopeto.xmpp.jid.Jid getBlockedJid();
	public ir.bilgisoft.toopeto.xmpp.jid.Jid getJid();
	public ir.bilgisoft.toopeto.entities.Account getAccount();
}
