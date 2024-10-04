package ir.bilgisoft.toopeto.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.SystemClock;

import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import ir.bilgisoft.toopeto.R;

public class Account extends ir.bilgisoft.toopeto.entities.AbstractEntity {

	public static final String TABLENAME = "accounts";
	public static final String USERNAME = "username";
	public static final String SERVER = "server";
	public static final String PASSWORD = "password";
	public static final String OPTIONS = "options";
	public static final String ROSTERVERSION = "rosterversion";
	public static final String KEYS = "keys";
	public static final String AVATAR = "avatar";

	public static final String PINNED_MECHANISM_KEY = "pinned_mechanism";

	public static final int OPTION_USETLS = 0;
	public static final int OPTION_DISABLED = 1;
	public static final int OPTION_REGISTER = 2;
	public static final int OPTION_USECOMPRESSION = 3;

	public static enum State {
		DISABLED,
		OFFLINE,
		CONNECTING,
		ONLINE,
		NO_INTERNET,
		UNAUTHORIZED(true),
		SERVER_NOT_FOUND(true),
		REGISTRATION_FAILED(true),
		REGISTRATION_CONFLICT(true),
		REGISTRATION_SUCCESSFUL,
		REGISTRATION_NOT_SUPPORTED(true),
		SECURITY_ERROR(true),
		INCOMPATIBLE_SERVER(true);

		private final boolean isError;

		public boolean isError() {
			return this.isError;
		}

		private State(final boolean isError) {
			this.isError = isError;
		}

		private State() {
			this(false);
		}

		public int getReadableId() {
			switch (this) {
				case DISABLED:
					return R.string.account_status_disabled;
				case ONLINE:
					return R.string.account_status_online;
				case CONNECTING:
					return R.string.account_status_connecting;
				case OFFLINE:
					return R.string.account_status_offline;
				case UNAUTHORIZED:
					return R.string.account_status_unauthorized;
				case SERVER_NOT_FOUND:
					return R.string.account_status_not_found;
				case NO_INTERNET:
					return R.string.account_status_no_internet;
				case REGISTRATION_FAILED:
					return R.string.account_status_regis_fail;
				case REGISTRATION_CONFLICT:
					return R.string.account_status_regis_conflict;
				case REGISTRATION_SUCCESSFUL:
					return R.string.account_status_regis_success;
				case REGISTRATION_NOT_SUPPORTED:
					return R.string.account_status_regis_not_sup;
				case SECURITY_ERROR:
					return R.string.account_status_security_error;
				case INCOMPATIBLE_SERVER:
					return R.string.account_status_incompatible_server;
				default:
					return R.string.account_status_unknown;
			}
		}
	}

	public List<ir.bilgisoft.toopeto.entities.Conversation> pendingConferenceJoins = new CopyOnWriteArrayList<>();
	public List<ir.bilgisoft.toopeto.entities.Conversation> pendingConferenceLeaves = new CopyOnWriteArrayList<>();
	public static ir.bilgisoft.toopeto.xmpp.jid.Jid jid;
	protected String password;
	protected int options = 0;
	protected String rosterVersion;
	protected State status = State.OFFLINE;
	protected JSONObject keys = new JSONObject();
	protected String avatar;
	protected boolean online = false;
	private ir.bilgisoft.toopeto.crypto.OtrEngine otrEngine = null;
	private ir.bilgisoft.toopeto.xmpp.XmppConnection xmppConnection = null;
	private long mEndGracePeriod = 0L;
	private String otrFingerprint;
	private final ir.bilgisoft.toopeto.entities.Roster roster = new ir.bilgisoft.toopeto.entities.Roster(this);
	private List<ir.bilgisoft.toopeto.entities.Bookmark> bookmarks = new CopyOnWriteArrayList<>();
    private List<ir.bilgisoft.toopeto.entities.Bookmark> rooms = new CopyOnWriteArrayList<>();
	private final Collection<ir.bilgisoft.toopeto.xmpp.jid.Jid> blocklist = new CopyOnWriteArraySet<>();

	public Account() {
		this.uuid = "0";
	}

	public Account(final ir.bilgisoft.toopeto.xmpp.jid.Jid jid, final String password) {
		this(java.util.UUID.randomUUID().toString(), jid,
				password, 0, null, "", null);
	}

	public Account(final String uuid, final ir.bilgisoft.toopeto.xmpp.jid.Jid jid,
			final String password, final int options, final String rosterVersion, final String keys,
			final String avatar) {
		this.uuid = uuid;
		this.jid = jid;
		if (jid.isBareJid()) {
			this.setResource("mobile");
		}
		this.password = password;
		this.options = options;
		this.rosterVersion = rosterVersion;
		try {
			this.keys = new JSONObject(keys);
		} catch (final JSONException ignored) {

		}
		this.avatar = avatar;
	}

	public static Account fromCursor(final Cursor cursor) {
		ir.bilgisoft.toopeto.xmpp.jid.Jid jid = null;
		try {
			jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromParts(cursor.getString(cursor.getColumnIndex(USERNAME)),
                    cursor.getString(cursor.getColumnIndex(SERVER)), "mobile");
		} catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException ignored) {
		}
		return new Account(cursor.getString(cursor.getColumnIndex(UUID)),
				jid,
				cursor.getString(cursor.getColumnIndex(PASSWORD)),
				cursor.getInt(cursor.getColumnIndex(OPTIONS)),
				cursor.getString(cursor.getColumnIndex(ROSTERVERSION)),
				cursor.getString(cursor.getColumnIndex(KEYS)),
				cursor.getString(cursor.getColumnIndex(AVATAR)));
	}

	public boolean isOptionSet(final int option) {
		return ((options & (1 << option)) != 0);
	}

	public void setOption(final int option, final boolean value) {
		if (value) {
			this.options |= 1 << option;
		} else {
			this.options &= ~(1 << option);
		}
	}

	public String getUsername() {
		return jid.getLocalpart();
	}

	public void setUsername(final String username) throws ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException {
		jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromParts(username, jid.getDomainpart(), jid.getResourcepart());
	}

	public ir.bilgisoft.toopeto.xmpp.jid.Jid getServer() {
		return jid.toDomainJid();
	}

	public void setServer(final String server) throws ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException {
		jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromParts(jid.getLocalpart(), server, jid.getResourcepart());
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public State getStatus() {
		if (isOptionSet(OPTION_DISABLED)) {
			return State.DISABLED;
		} else {
			return this.status;
		}
	}

	public void setStatus(final State status) {
		this.status = status;
	}

	public boolean errorStatus() {
		return getStatus().isError();
	}

	public boolean hasErrorStatus() {
		return getXmppConnection() != null && getStatus().isError() && getXmppConnection().getAttempt() >= 2;
	}

	public String getResource() {
		return jid.getResourcepart();
	}

	public void setResource(final String resource) {
		try {
			jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromParts(jid.getLocalpart(), jid.getDomainpart(), resource);
		} catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException ignored) {
		}
	}

	public ir.bilgisoft.toopeto.xmpp.jid.Jid getJid() {
		return jid;
	}

	public JSONObject getKeys() {
		return keys;
	}

	public boolean setKey(final String keyName, final String keyValue) {
		try {
			this.keys.put(keyName, keyValue);
			return true;
		} catch (final JSONException e) {
			return false;
		}
	}

	@Override
	public ContentValues getContentValues() {
		final ContentValues values = new ContentValues();
		values.put(UUID, uuid);
		values.put(USERNAME, jid.getLocalpart());
		values.put(SERVER, jid.getDomainpart());
		values.put(PASSWORD, password);
		values.put(OPTIONS, options);
		values.put(KEYS, this.keys.toString());
		values.put(ROSTERVERSION, rosterVersion);
		values.put(AVATAR, avatar);
		return values;
	}

	public void initOtrEngine(final ir.bilgisoft.toopeto.services.XmppConnectionService context) {
		this.otrEngine = new ir.bilgisoft.toopeto.crypto.OtrEngine(context, this);
	}

	public ir.bilgisoft.toopeto.crypto.OtrEngine getOtrEngine() {
		return this.otrEngine;
	}

	public ir.bilgisoft.toopeto.xmpp.XmppConnection getXmppConnection() {
		return this.xmppConnection;
	}

	public void setXmppConnection(final ir.bilgisoft.toopeto.xmpp.XmppConnection connection) {
		this.xmppConnection = connection;
	}

	public String getOtrFingerprint() {
		if (this.otrFingerprint == null) {
			try {
				if (this.otrEngine == null) {
					return null;
				}
				final PublicKey publicKey = this.otrEngine.getPublicKey();
				if (publicKey == null || !(publicKey instanceof DSAPublicKey)) {
					return null;
				}
				this.otrFingerprint = new OtrCryptoEngineImpl().getFingerprint(publicKey);
				return this.otrFingerprint;
			} catch (final OtrCryptoException ignored) {
				return null;
			}
		} else {
			return this.otrFingerprint;
		}
	}

	public String getRosterVersion() {
		if (this.rosterVersion == null) {
			return "";
		} else {
			return this.rosterVersion;
		}
	}

	public void setRosterVersion(final String version) {
		this.rosterVersion = version;
	}

	public int countPresences() {
		return this.getRoster().getContact(this.getJid().getLocalpart()).getPresences().size();
	}

	public String getPgpSignature() {
		if (keys.has("pgp_signature")) {
			try {
				return keys.getString("pgp_signature");
			} catch (final JSONException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	public ir.bilgisoft.toopeto.entities.Roster getRoster() {
		return this.roster;
	}

    public List<ir.bilgisoft.toopeto.entities.Bookmark> getBookmarks() {
        return this.bookmarks;
    }

    public List<ir.bilgisoft.toopeto.entities.Bookmark> getRooms() {
        return this.rooms;
    }


    public void setBookmarks(final List<ir.bilgisoft.toopeto.entities.Bookmark> bookmarks) {
		this.bookmarks = bookmarks;
	}

    public void setRooms(final List<ir.bilgisoft.toopeto.entities.Bookmark> bookmarks) {
        this.rooms = bookmarks;
    }

	public boolean hasBookmarkFor(final ir.bilgisoft.toopeto.xmpp.jid.Jid conferenceJid) {
		for (final ir.bilgisoft.toopeto.entities.Bookmark bookmark : this.bookmarks) {
			final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = bookmark.getJid();
			if (jid != null && jid.equals(conferenceJid.toBareJid())) {
				return true;
			}
		}
		return false;
	}

	public boolean setAvatar(final String filename) {
		if (this.avatar != null && this.avatar.equals(filename)) {
			return false;
		} else {
			this.avatar = filename;
			return true;
		}
	}

	public String getAvatar() {
		return this.avatar;
	}

	public void activateGracePeriod() {
		this.mEndGracePeriod = SystemClock.elapsedRealtime()
			+ (ir.bilgisoft.toopeto.Config.CARBON_GRACE_PERIOD * 1000);
	}

	public void deactivateGracePeriod() {
		this.mEndGracePeriod = 0L;
	}

	public boolean inGracePeriod() {
		return SystemClock.elapsedRealtime() < this.mEndGracePeriod;
	}

	public String getShareableUri() {
		final String fingerprint = this.getOtrFingerprint();
		if (fingerprint != null) {
			return "xmpp:" + this.getJid().toBareJid().toString() + "?otr-fingerprint="+fingerprint;
		} else {
			return "xmpp:" + this.getJid().toBareJid().toString();
		}
	}

	public boolean isBlocked(final ir.bilgisoft.toopeto.entities.ListItem contact) {
		final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = contact.getJid();
		return jid != null && (blocklist.contains(jid.toBareJid()) || blocklist.contains(jid.toDomainJid()));
	}

	public boolean isBlocked(final ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
		return jid != null && blocklist.contains(jid.toBareJid());
	}

	public Collection<ir.bilgisoft.toopeto.xmpp.jid.Jid> getBlocklist() {
		return this.blocklist;
	}

	public void clearBlocklist() {
		getBlocklist().clear();
	}

	public boolean isOnlineAndConnected() {
		return this.getStatus() == State.ONLINE && this.getXmppConnection() != null;
	}
}
