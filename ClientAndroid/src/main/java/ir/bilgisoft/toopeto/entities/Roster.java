package ir.bilgisoft.toopeto.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Roster {
	final ir.bilgisoft.toopeto.entities.Account account;
	final HashMap<String, ir.bilgisoft.toopeto.entities.Contact> contacts = new HashMap<>();
    ///////////////my code//////////////////
	private String version = null;

	public Roster(ir.bilgisoft.toopeto.entities.Account account) {
		this.account = account;
	}

	public ir.bilgisoft.toopeto.entities.Contact getContactFromRoster(ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
		if (jid == null) {
			return null;
		}
		synchronized (this.contacts) {
			ir.bilgisoft.toopeto.entities.Contact contact = contacts.get(jid.toBareJid().toString());
			if (contact != null && contact.showInRoster()) {
				return contact;
			} else {
				return null;
			}
		}
	}

//	public ir.bilgisoft.toopeto.entities.Contact getContact(final ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
	public Contact getContact(final String jid) {
		synchronized (this.contacts) {
			final String bareJid = jid;
			if (contacts.containsKey(bareJid)) {
				return contacts.get(bareJid);
			} else {
				Contact contact = new Contact(bareJid);
				contact.setAccount(account);
				contacts.put(bareJid, contact);
				return contact;
			}
		}
	}
//pak kardane hozor kol contactha9999
	public void clearPresences() {
		for (ir.bilgisoft.toopeto.entities.Contact contact : getContacts()) {
			contact.clearPresences();
		}
	}

	public void markAllAsNotInRoster() {
		for (ir.bilgisoft.toopeto.entities.Contact contact : getContacts()) {
			contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.IN_ROSTER);
		}
	}

	public void clearSystemAccounts() {
		for (ir.bilgisoft.toopeto.entities.Contact contact : getContacts()) {
			contact.setPhotoUri(null);
			contact.setSystemName(null);
			contact.setSystemAccount(null);
		}
	}

	public List<ir.bilgisoft.toopeto.entities.Contact> getContacts() {
		synchronized (this.contacts) {
			return new ArrayList<>(this.contacts.values());
		}
	}

	public void initContact(final ir.bilgisoft.toopeto.entities.Contact contact) {
		contact.setAccount(account);
		contact.setOption(ir.bilgisoft.toopeto.entities.Contact.Options.IN_ROSTER);
		synchronized (this.contacts) {
			contacts.put(contact.getJid().toBareJid().toString(), contact);
		}
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return this.version;
	}

	public ir.bilgisoft.toopeto.entities.Account getAccount() {
		return this.account;
	}
    //////////////////////end my code///////////////////////////
}
