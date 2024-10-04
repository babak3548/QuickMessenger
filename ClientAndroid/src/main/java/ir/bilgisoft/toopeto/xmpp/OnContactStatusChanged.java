package ir.bilgisoft.toopeto.xmpp;

public interface OnContactStatusChanged {
	public void onContactStatusChanged(final ir.bilgisoft.toopeto.entities.Contact contact, final boolean online);
}
