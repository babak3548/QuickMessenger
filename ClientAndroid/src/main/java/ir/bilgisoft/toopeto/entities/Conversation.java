package ir.bilgisoft.toopeto.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.SystemClock;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.interfaces.DSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ir.bilgisoft.toopeto.json.Enums;

public class Conversation extends ir.bilgisoft.toopeto.entities.AbstractEntity implements ir.bilgisoft.toopeto.entities.Blockable {
	public static final String TABLENAME = "conversations";

	public static final int STATUS_AVAILABLE = 0;
	public static final int STATUS_ARCHIVED = 1;
	public static final int STATUS_DELETED = 2;

	public static final int MODE_MULTI = 1;
	public static final int MODE_SINGLE = 0;

	public static final String NAME = "name";
	public static final String ACCOUNT = "accountUuid";
	public static final String CONTACT = "contactUuid";
	public static final String CONTACTJID = "contactJid";
	public static final String STATUS = "status";
	public static final String CREATED = "created";
	public static final String MODE = "mode";
	public static final String ATTRIBUTES = "attributes";

	public static final String ATTRIBUTE_NEXT_ENCRYPTION = "next_encryption";
	public static final String ATTRIBUTE_MUC_PASSWORD = "muc_password";
	public static final String ATTRIBUTE_MUTED_TILL = "muted_till";
	public static final String ATTRIBUTE_LAST_MESSAGE_TRANSMITTED = "last_message_transmitted";

	private String name;
	private String contactUuid;
	private String accountUuid;
	private ir.bilgisoft.toopeto.xmpp.jid.Jid contactJid;
	private int status;
	private long created;
	private int mode;

	private JSONObject attributes = new JSONObject();

	private ir.bilgisoft.toopeto.xmpp.jid.Jid nextCounterpart;

	protected final ArrayList<ir.bilgisoft.toopeto.entities.Message> messages = new ArrayList<>();
	protected ir.bilgisoft.toopeto.entities.Account account = null;

	private transient SessionImpl otrSession;

	private transient String otrFingerprint = null;
	private Smp mSmp = new Smp();

	private String nextMessage;

	private transient ir.bilgisoft.toopeto.entities.MucOptions mucOptions = null;

	private byte[] symmetricKey;

	private ir.bilgisoft.toopeto.entities.Bookmark bookmark;

	private boolean messagesLeftOnServer = true;

	public boolean hasMessagesLeftOnServer() {
		return messagesLeftOnServer;
	}

	public void setHasMessagesLeftOnServer(boolean value) {
		this.messagesLeftOnServer = value;
	}

	public ir.bilgisoft.toopeto.entities.Message findUnsentMessageWithUuid(String uuid) {
		synchronized(this.messages) {
			for (final ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				final int s = message.getStatus();
				if ((s == ir.bilgisoft.toopeto.entities.Message.STATUS_UNSEND || s == ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING) && message.getUuid().equals(uuid)) {
					return message;
				}
			}
		}
		return null;
	}

	public void findWaitingMessages(OnMessageFound onMessageFound) {
		synchronized (this.messages) {
			for(ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				if (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING) {
					onMessageFound.onMessageFound(message);
				}
			}
		}
	}

	public void findMessagesWithFiles(OnMessageFound onMessageFound) {
		synchronized (this.messages) {
			for (ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				if ((message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE)
						&& message.getEncryption() != ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
					onMessageFound.onMessageFound(message);
						}
			}
		}
	}

	public ir.bilgisoft.toopeto.entities.Message findMessageWithFileAndUuid(String uuid) {
		synchronized (this.messages) {
			for (ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE
						&& message.getEncryption() != ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP
						&& message.getUuid().equals(uuid)) {
					return message;
						}
			}
		}
		return null;
	}

	public void clearMessages() {
		synchronized (this.messages) {
			this.messages.clear();
		}
	}

	public void trim() {
		synchronized (this.messages) {
			final int size = messages.size();
			final int maxsize = ir.bilgisoft.toopeto.Config.PAGE_SIZE * ir.bilgisoft.toopeto.Config.MAX_NUM_PAGES;
			if (size > maxsize) {
				this.messages.subList(0, size - maxsize).clear();
			}
		}
	}

	public void findUnsentMessagesWithOtrEncryption(OnMessageFound onMessageFound) {
		synchronized (this.messages) {
			for (ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				if ((message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_UNSEND || message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING)
						&& (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR)) {
					onMessageFound.onMessageFound(message);
						}
			}
		}
	}

	public void findUnsentTextMessages(OnMessageFound onMessageFound) {
		synchronized (this.messages) {
			for (ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				if (message.getType() != ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE
						&& message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_UNSEND) {
					onMessageFound.onMessageFound(message);
						}
			}
		}
	}

	public ir.bilgisoft.toopeto.entities.Message findSentMessageWithUuid(String uuid) {
		synchronized (this.messages) {
			for (ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				if (uuid.equals(message.getUuid())
						|| (message.getStatus() >= ir.bilgisoft.toopeto.entities.Message.STATUS_SEND && uuid
							.equals(message.getRemoteMsgId()))) {
					return message;
							}
			}
		}
		return null;
	}

	public void populateWithMessages(final List<ir.bilgisoft.toopeto.entities.Message> messages) {
		synchronized (this.messages) {
			messages.clear();
			messages.addAll(this.messages);
		}
	}

	@Override
	public boolean isBlocked() {
		return getContact().isBlocked();
	}

	@Override
	public boolean isDomainBlocked() {
		return getContact().isDomainBlocked();
	}

	@Override
	public ir.bilgisoft.toopeto.xmpp.jid.Jid getBlockedJid() {
		return getContact().getBlockedJid();
	}


	public interface OnMessageFound {
		public void onMessageFound(final ir.bilgisoft.toopeto.entities.Message message);
	}

	public Conversation(final String name, final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.jid.Jid contactJid,
			final int mode) {
		this(java.util.UUID.randomUUID().toString(), name, null, account
				.getUuid(), contactJid, System.currentTimeMillis(),
				STATUS_AVAILABLE, mode, "");
		this.account = account;
	}

	public Conversation(final String uuid, final String name, final String contactUuid,
			final String accountUuid, final ir.bilgisoft.toopeto.xmpp.jid.Jid contactJid, final long created, final int status,
			final int mode, final String attributes) {
		this.uuid = uuid;
		this.name = name;
		this.contactUuid = contactUuid;
		this.accountUuid = accountUuid;
		this.contactJid = contactJid;
		this.created = created;
		this.status = status;
		this.mode = mode;
		try {
			this.attributes = new JSONObject(attributes == null ? "" : attributes);
		} catch (JSONException e) {
			this.attributes = new JSONObject();
		}
	}

	public boolean isRead() {
		return (this.messages.size() == 0) || this.messages.get(this.messages.size() - 1).isRead();
	}

	public void markRead() {
		for (int i = this.messages.size() - 1; i >= 0; --i) {
			if (messages.get(i).isRead()) {
				break;
			}
			this.messages.get(i).markRead();
		}
	}

	public ir.bilgisoft.toopeto.entities.Message getLatestMarkableMessage() {
		for (int i = this.messages.size() - 1; i >= 0; --i) {
			if (this.messages.get(i).getStatus() <= ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED
					&& this.messages.get(i).markable) {
				if (this.messages.get(i).isRead()) {
					return null;
				} else {
					return this.messages.get(i);
				}
					}
		}
		return null;
	}

	public ir.bilgisoft.toopeto.entities.Message getLatestMessage() {
		if (this.messages.size() == 0) {
			ir.bilgisoft.toopeto.entities.Message message = new ir.bilgisoft.toopeto.entities.Message(this, "", ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE);
			message.setTime(getCreated());
			return message;
		} else {
			ir.bilgisoft.toopeto.entities.Message message = this.messages.get(this.messages.size() - 1);
			message.setConversation(this);
			return message;
		}
	}

	public String getName() {
		if (getMode() == MODE_MULTI) {
			if (getMucOptions().getSubject() != null) {
				return getMucOptions().getSubject();
			} else if (bookmark != null && bookmark.getName() != null) {
				return bookmark.getName();
			} else {
				String generatedName = getMucOptions().createNameFromParticipants();
				if (generatedName != null) {
					return generatedName;
				} else {
					return getJid().getLocalpart();
				}
			}
		} else {
			return this.getContact().getDisplayName();
		}
	}

	public String getAccountUuid() {
		return this.accountUuid;
	}

	public ir.bilgisoft.toopeto.entities.Account getAccount() {
		return this.account;
	}

	public ir.bilgisoft.toopeto.entities.Contact getContact() {
		return this.account.getRoster().getContact(this.contactJid.getLocalpart());
	}

	public void setAccount(final ir.bilgisoft.toopeto.entities.Account account) {
		this.account = account;
	}

	@Override
	public ir.bilgisoft.toopeto.xmpp.jid.Jid getJid() {
		return this.contactJid;
	}

	public int getStatus() {
		return this.status;
	}

	public long getCreated() {
		return this.created;
	}

	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(UUID, uuid);
		values.put(NAME, name);
		values.put(CONTACT, contactUuid);
		values.put(ACCOUNT, accountUuid);
		values.put(CONTACTJID, contactJid.toString());
		values.put(CREATED, created);
		values.put(STATUS, status);
		values.put(MODE, mode);
		values.put(ATTRIBUTES, attributes.toString());
		return values;
	}

	public static Conversation fromCursor(Cursor cursor) {
		ir.bilgisoft.toopeto.xmpp.jid.Jid jid;
		try {
			jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(cursor.getString(cursor.getColumnIndex(CONTACTJID)));
		} catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
			// Borked DB..
			jid = null;
		}
		return new Conversation(cursor.getString(cursor.getColumnIndex(UUID)),
				cursor.getString(cursor.getColumnIndex(NAME)),
				cursor.getString(cursor.getColumnIndex(CONTACT)),
				cursor.getString(cursor.getColumnIndex(ACCOUNT)),
				jid,
				cursor.getLong(cursor.getColumnIndex(CREATED)),
				cursor.getInt(cursor.getColumnIndex(STATUS)),
				cursor.getInt(cursor.getColumnIndex(MODE)),
				cursor.getString(cursor.getColumnIndex(ATTRIBUTES)));
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getMode() {
		return this.mode;
	}
    public Enums.ReceiverEnum getModeEnum() {
        if (Conversation.MODE_SINGLE == getMode())return  Enums.ReceiverEnum.user;
        else return  Enums.ReceiverEnum.room;
    }
	public void setMode(int mode) {
		this.mode = mode;
	}

	public SessionImpl startOtrSession(String presence, boolean sendStart) {
		if (this.otrSession != null) {
			return this.otrSession;
		} else {
			final SessionID sessionId = new SessionID(this.getJid().toBareJid().toString(),
					presence,
					"xmpp");
			this.otrSession = new SessionImpl(sessionId, getAccount().getOtrEngine());
			try {
				if (sendStart) {
					this.otrSession.startSession();
					return this.otrSession;
				}
				return this.otrSession;
			} catch (OtrException e) {
				return null;
			}
		}

	}

	public SessionImpl getOtrSession() {
		return this.otrSession;
	}

	public void resetOtrSession() {
		this.otrFingerprint = null;
		this.otrSession = null;
		this.mSmp.hint = null;
		this.mSmp.secret = null;
		this.mSmp.status = Smp.STATUS_NONE;
	}

	public Smp smp() {
		return mSmp;
	}

	public void startOtrIfNeeded() {
		if (this.otrSession != null
				&& this.otrSession.getSessionStatus() != SessionStatus.ENCRYPTED) {
			try {
				this.otrSession.startSession();
			} catch (OtrException e) {
				this.resetOtrSession();
			}
				}
	}

	public boolean endOtrIfNeeded() {
		if (this.otrSession != null) {
			if (this.otrSession.getSessionStatus() == SessionStatus.ENCRYPTED) {
				try {
					this.otrSession.endSession();
					this.resetOtrSession();
					return true;
				} catch (OtrException e) {
					this.resetOtrSession();
					return false;
				}
			} else {
				this.resetOtrSession();
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean hasValidOtrSession() {
		return this.otrSession != null;
	}

	public synchronized String getOtrFingerprint() {
		if (this.otrFingerprint == null) {
			try {
				if (getOtrSession() == null || getOtrSession().getSessionStatus() != SessionStatus.ENCRYPTED) {
					return null;
				}
				DSAPublicKey remotePubKey = (DSAPublicKey) getOtrSession().getRemotePublicKey();
				this.otrFingerprint = getAccount().getOtrEngine().getFingerprint(remotePubKey);
			} catch (final OtrCryptoException | UnsupportedOperationException ignored) {
				return null;
			}
		}
		return this.otrFingerprint;
	}

	public boolean verifyOtrFingerprint() {
		final String fingerprint = getOtrFingerprint();
		if (fingerprint != null) {
			getContact().addOtrFingerprint(fingerprint);
			return true;
		} else {
			return false;
		}
	}

	public boolean isOtrFingerprintVerified() {
		return getContact().getOtrFingerprints().contains(getOtrFingerprint());
	}

	public synchronized ir.bilgisoft.toopeto.entities.MucOptions getMucOptions() {
		if (this.mucOptions == null) {
			this.mucOptions = new ir.bilgisoft.toopeto.entities.MucOptions(this);
		}
		return this.mucOptions;
	}

	public void resetMucOptions() {
		this.mucOptions = null;
	}

	public void setContactJid(final ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
		this.contactJid = jid;
	}

	public void setNextCounterpart(ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
		this.nextCounterpart = jid;
	}

	public ir.bilgisoft.toopeto.xmpp.jid.Jid getNextCounterpart() {
		return this.nextCounterpart;
	}

	public int getLatestEncryption() {
		int latestEncryption = this.getLatestMessage().getEncryption();
		if ((latestEncryption == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED)
				|| (latestEncryption == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTION_FAILED)) {
			return ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP;
		} else {
			return latestEncryption;
		}
	}

	public int getNextEncryption(boolean force) {
		int next = this.getIntAttribute(ATTRIBUTE_NEXT_ENCRYPTION, -1);
		if (next == -1) {
			int latest = this.getLatestEncryption();
			if (latest == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE) {
				if (force && getMode() == MODE_SINGLE) {
					return ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR;
				} else if (getContact().getPresences().size() == 1) {
					if (getContact().getOtrFingerprints().size() >= 1) {
						return ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR;
					} else {
						return latest;
					}
				} else {
					return latest;
				}
			} else {
				return latest;
			}
		}
		if (next == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE && force
				&& getMode() == MODE_SINGLE) {
			return ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR;
		} else {
			return next;
		}
	}

	public void setNextEncryption(int encryption) {
		this.setAttribute(ATTRIBUTE_NEXT_ENCRYPTION, String.valueOf(encryption));
	}

	public String getNextMessage() {
		if (this.nextMessage == null) {
			return "";
		} else {
			return this.nextMessage;
		}
	}

	public boolean smpRequested() {
		return smp().status == Smp.STATUS_CONTACT_REQUESTED;
	}

	public void setNextMessage(String message) {
		this.nextMessage = message;
	}

	public void setSymmetricKey(byte[] key) {
		this.symmetricKey = key;
	}

	public byte[] getSymmetricKey() {
		return this.symmetricKey;
	}

	public void setBookmark(ir.bilgisoft.toopeto.entities.Bookmark bookmark) {
		this.bookmark = bookmark;
		this.bookmark.setConversation(this);
	}

	public void deregisterWithBookmark() {
		if (this.bookmark != null) {
			this.bookmark.setConversation(null);
		}
	}

	public ir.bilgisoft.toopeto.entities.Bookmark getBookmark() {
		return this.bookmark;
	}

	public boolean hasDuplicateMessage(ir.bilgisoft.toopeto.entities.Message message) {
		synchronized (this.messages) {
			for (int i = this.messages.size() - 1; i >= 0; --i) {
				if (this.messages.get(i).equals(message)) {
					return true;
				}
			}
		}
		return false;
	}

	public ir.bilgisoft.toopeto.entities.Message findSentMessageWithBody(String body) {
		synchronized (this.messages) {
			for (int i = this.messages.size() - 1; i >= 0; --i) {
				ir.bilgisoft.toopeto.entities.Message message = this.messages.get(i);
				if ((message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_UNSEND || message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_SEND) && message.getBody() != null && message.getBody().equals(body)) {
					return message;
				}
			}
			return null;
		}
	}

	public boolean setLastMessageTransmitted(long value) {
		long before = getLastMessageTransmitted();
		if (value - before > 1000) {
			this.setAttribute(ATTRIBUTE_LAST_MESSAGE_TRANSMITTED, String.valueOf(value));
			return true;
		} else {
			return false;
		}
	}

	public long getLastMessageTransmitted() {
		long timestamp = getLongAttribute(ATTRIBUTE_LAST_MESSAGE_TRANSMITTED,0);
		if (timestamp == 0) {
			synchronized (this.messages) {
				for(int i = this.messages.size() - 1; i >= 0; --i) {
					ir.bilgisoft.toopeto.entities.Message message = this.messages.get(i);
					if (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED) {
						return message.getTimeSent();
					}
				}
			}
		}
		return timestamp;
	}

	public void setMutedTill(long value) {
		this.setAttribute(ATTRIBUTE_MUTED_TILL, String.valueOf(value));
	}

	public boolean isMuted() {
		return SystemClock.elapsedRealtime() < this.getLongAttribute(
				ATTRIBUTE_MUTED_TILL, 0);
	}

	public boolean setAttribute(String key, String value) {
		try {
			this.attributes.put(key, value);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}

	public String getAttribute(String key) {
		try {
			return this.attributes.getString(key);
		} catch (JSONException e) {
			return null;
		}
	}

	public int getIntAttribute(String key, int defaultValue) {
		String value = this.getAttribute(key);
		if (value == null) {
			return defaultValue;
		} else {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
	}

	public long getLongAttribute(String key, long defaultValue) {
		String value = this.getAttribute(key);
		if (value == null) {
			return defaultValue;
		} else {
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
	}

	public void add(ir.bilgisoft.toopeto.entities.Message message) {
		message.setConversation(this);
		synchronized (this.messages) {
			this.messages.add(message);
		}
	}

	public void addAll(int index, List<ir.bilgisoft.toopeto.entities.Message> messages) {
		synchronized (this.messages) {
			this.messages.addAll(index, messages);
		}
	}

	public void sort() {
		synchronized (this.messages) {
			Collections.sort(this.messages, new Comparator<ir.bilgisoft.toopeto.entities.Message>() {
				@Override
				public int compare(ir.bilgisoft.toopeto.entities.Message left, ir.bilgisoft.toopeto.entities.Message right) {
					if (left.getTimeSent() < right.getTimeSent()) {
						return -1;
					} else if (left.getTimeSent() > right.getTimeSent()) {
						return 1;
					} else {
						return 0;
					}
				}
			});
			for(ir.bilgisoft.toopeto.entities.Message message : this.messages) {
				message.untie();
			}
		}
	}

	public class Smp {
		public static final int STATUS_NONE = 0;
		public static final int STATUS_CONTACT_REQUESTED = 1;
		public static final int STATUS_WE_REQUESTED = 2;
		public static final int STATUS_FAILED = 3;
		public static final int STATUS_VERIFIED = 4;

		public String secret = null;
		public String hint = null;
		public int status = 0;
	}
}
