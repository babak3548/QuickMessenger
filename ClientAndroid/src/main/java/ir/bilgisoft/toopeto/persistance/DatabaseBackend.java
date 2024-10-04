package ir.bilgisoft.toopeto.persistance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseBackend extends SQLiteOpenHelper {

	private static DatabaseBackend instance = null;

	private static final String DATABASE_NAME = "history";
	private static final int DATABASE_VERSION = 13;

	private static String CREATE_CONTATCS_STATEMENT = "create table "
			+ ir.bilgisoft.toopeto.entities.Contact.TABLENAME + "(" + ir.bilgisoft.toopeto.entities.Contact.ACCOUNT + " TEXT, "
			+ ir.bilgisoft.toopeto.entities.Contact.SERVERNAME + " TEXT, " + ir.bilgisoft.toopeto.entities.Contact.SYSTEMNAME + " TEXT,"
			+ ir.bilgisoft.toopeto.entities.Contact.JID + " TEXT," + ir.bilgisoft.toopeto.entities.Contact.KEYS + " TEXT,"
			+ ir.bilgisoft.toopeto.entities.Contact.PHOTOURI + " TEXT," + ir.bilgisoft.toopeto.entities.Contact.OPTIONS + " NUMBER,"
			+ ir.bilgisoft.toopeto.entities.Contact.SYSTEMACCOUNT + " NUMBER, " + ir.bilgisoft.toopeto.entities.Contact.AVATAR + " TEXT, "
            + ir.bilgisoft.toopeto.entities.Contact.LAST_PRESENCE + " TEXT, " + ir.bilgisoft.toopeto.entities.Contact.LAST_TIME + " NUMBER, "
			+ ir.bilgisoft.toopeto.entities.Contact.GROUPS + " TEXT, FOREIGN KEY(" + ir.bilgisoft.toopeto.entities.Contact.ACCOUNT + ") REFERENCES "
			+ ir.bilgisoft.toopeto.entities.Account.TABLENAME + "(" + ir.bilgisoft.toopeto.entities.Account.UUID
			+ ") ON DELETE CASCADE, UNIQUE(" + ir.bilgisoft.toopeto.entities.Contact.ACCOUNT + ", "
			+ ir.bilgisoft.toopeto.entities.Contact.JID + ") ON CONFLICT REPLACE);";

	private DatabaseBackend(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("PRAGMA foreign_keys=ON;");
		db.execSQL("create table " + ir.bilgisoft.toopeto.entities.Account.TABLENAME + "(" + ir.bilgisoft.toopeto.entities.Account.UUID
				+ " TEXT PRIMARY KEY," + ir.bilgisoft.toopeto.entities.Account.USERNAME + " TEXT,"
				+ ir.bilgisoft.toopeto.entities.Account.SERVER + " TEXT," + ir.bilgisoft.toopeto.entities.Account.PASSWORD + " TEXT,"
				+ ir.bilgisoft.toopeto.entities.Account.ROSTERVERSION + " TEXT," + ir.bilgisoft.toopeto.entities.Account.OPTIONS
				+ " NUMBER, " + ir.bilgisoft.toopeto.entities.Account.AVATAR + " TEXT, " + ir.bilgisoft.toopeto.entities.Account.KEYS
				+ " TEXT)");
		db.execSQL("create table " + ir.bilgisoft.toopeto.entities.Conversation.TABLENAME + " ("
				+ ir.bilgisoft.toopeto.entities.Conversation.UUID + " TEXT PRIMARY KEY, " + ir.bilgisoft.toopeto.entities.Conversation.NAME
				+ " TEXT, " + ir.bilgisoft.toopeto.entities.Conversation.CONTACT + " TEXT, "
				+ ir.bilgisoft.toopeto.entities.Conversation.ACCOUNT + " TEXT, " + ir.bilgisoft.toopeto.entities.Conversation.CONTACTJID
				+ " TEXT, " + ir.bilgisoft.toopeto.entities.Conversation.CREATED + " NUMBER, "
				+ ir.bilgisoft.toopeto.entities.Conversation.STATUS + " NUMBER, " + ir.bilgisoft.toopeto.entities.Conversation.MODE
				+ " NUMBER, " + ir.bilgisoft.toopeto.entities.Conversation.ATTRIBUTES + " TEXT, FOREIGN KEY("
				+ ir.bilgisoft.toopeto.entities.Conversation.ACCOUNT + ") REFERENCES " + ir.bilgisoft.toopeto.entities.Account.TABLENAME
				+ "(" + ir.bilgisoft.toopeto.entities.Account.UUID + ") ON DELETE CASCADE);");
		db.execSQL("create table " + ir.bilgisoft.toopeto.entities.Message.TABLENAME + "( " + ir.bilgisoft.toopeto.entities.Message.UUID
				+ " TEXT PRIMARY KEY, " + ir.bilgisoft.toopeto.entities.Message.CONVERSATION + " TEXT, "
				+ ir.bilgisoft.toopeto.entities.Message.TIME_SENT + " NUMBER, " + ir.bilgisoft.toopeto.entities.Message.COUNTERPART
				+ " TEXT, " + ir.bilgisoft.toopeto.entities.Message.TRUE_COUNTERPART + " TEXT,"
				+ ir.bilgisoft.toopeto.entities.Message.BODY + " TEXT, " + ir.bilgisoft.toopeto.entities.Message.ENCRYPTION + " NUMBER, "
				+ ir.bilgisoft.toopeto.entities.Message.STATUS + " NUMBER," + ir.bilgisoft.toopeto.entities.Message.TYPE + " NUMBER, "
				+ ir.bilgisoft.toopeto.entities.Message.RELATIVE_FILE_PATH + " TEXT, "
				+ ir.bilgisoft.toopeto.entities.Message.SERVER_MSG_ID + " TEXT, "
				+ ir.bilgisoft.toopeto.entities.Message.REMOTE_MSG_ID + " TEXT, FOREIGN KEY("
				+ ir.bilgisoft.toopeto.entities.Message.CONVERSATION + ") REFERENCES "
				+ ir.bilgisoft.toopeto.entities.Conversation.TABLENAME + "(" + ir.bilgisoft.toopeto.entities.Conversation.UUID
				+ ") ON DELETE CASCADE);");

		db.execSQL(CREATE_CONTATCS_STATEMENT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2 && newVersion >= 2) {
			db.execSQL("update " + ir.bilgisoft.toopeto.entities.Account.TABLENAME + " set "
					+ ir.bilgisoft.toopeto.entities.Account.OPTIONS + " = " + ir.bilgisoft.toopeto.entities.Account.OPTIONS + " | 8");
		}
		if (oldVersion < 3 && newVersion >= 3) {
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Message.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Message.TYPE + " NUMBER");
		}
		if (oldVersion < 5 && newVersion >= 5) {
			db.execSQL("DROP TABLE " + ir.bilgisoft.toopeto.entities.Contact.TABLENAME);
			db.execSQL(CREATE_CONTATCS_STATEMENT);
			db.execSQL("UPDATE " + ir.bilgisoft.toopeto.entities.Account.TABLENAME + " SET "
					+ ir.bilgisoft.toopeto.entities.Account.ROSTERVERSION + " = NULL");
		}
		if (oldVersion < 6 && newVersion >= 6) {
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Message.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Message.TRUE_COUNTERPART + " TEXT");
		}
		if (oldVersion < 7 && newVersion >= 7) {
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Message.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Message.REMOTE_MSG_ID + " TEXT");
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Contact.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Contact.AVATAR + " TEXT");
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Account.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Account.AVATAR + " TEXT");
		}
		if (oldVersion < 8 && newVersion >= 8) {
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Conversation.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Conversation.ATTRIBUTES + " TEXT");
		}
        if (oldVersion < 9 && newVersion >= 9) {
            db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Contact.TABLENAME + " ADD COLUMN "
                    + ir.bilgisoft.toopeto.entities.Contact.LAST_TIME + " NUMBER");
            db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Contact.TABLENAME + " ADD COLUMN "
                    + ir.bilgisoft.toopeto.entities.Contact.LAST_PRESENCE + " TEXT");
        }
		if (oldVersion < 10 && newVersion >= 10) {
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Message.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Message.RELATIVE_FILE_PATH + " TEXT");
		}
		if (oldVersion < 11 && newVersion >= 11) {
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Contact.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Contact.GROUPS + " TEXT");
			db.execSQL("delete from "+ ir.bilgisoft.toopeto.entities.Contact.TABLENAME);
			db.execSQL("update "+ ir.bilgisoft.toopeto.entities.Account.TABLENAME+" set "+ ir.bilgisoft.toopeto.entities.Account.ROSTERVERSION+" = NULL");
		}
		if (oldVersion < 12 && newVersion >= 12) {
			db.execSQL("ALTER TABLE " + ir.bilgisoft.toopeto.entities.Message.TABLENAME + " ADD COLUMN "
					+ ir.bilgisoft.toopeto.entities.Message.SERVER_MSG_ID + " TEXT");
		}
		if (oldVersion < 13 && newVersion >= 13) {
			db.execSQL("delete from "+ ir.bilgisoft.toopeto.entities.Contact.TABLENAME);
			db.execSQL("update "+ ir.bilgisoft.toopeto.entities.Account.TABLENAME+" set "+ ir.bilgisoft.toopeto.entities.Account.ROSTERVERSION+" = NULL");
		}
	}

	public static synchronized DatabaseBackend getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseBackend(context);
		}
		return instance;
	}

	public void createConversation(ir.bilgisoft.toopeto.entities.Conversation conversation) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(ir.bilgisoft.toopeto.entities.Conversation.TABLENAME, null, conversation.getContentValues());
	}

	public void createMessage(ir.bilgisoft.toopeto.entities.Message message) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(ir.bilgisoft.toopeto.entities.Message.TABLENAME, null, message.getContentValues());
	}

	public void createAccount(ir.bilgisoft.toopeto.entities.Account account) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(ir.bilgisoft.toopeto.entities.Account.TABLENAME, null, account.getContentValues());
	}

	public void createContact(ir.bilgisoft.toopeto.entities.Contact contact) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(ir.bilgisoft.toopeto.entities.Contact.TABLENAME, null, contact.getContentValues());
	}

	public int getConversationCount() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("select count(uuid) as count from "
				+ ir.bilgisoft.toopeto.entities.Conversation.TABLENAME + " where " + ir.bilgisoft.toopeto.entities.Conversation.STATUS
				+ "=" + ir.bilgisoft.toopeto.entities.Conversation.STATUS_AVAILABLE, null);
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();
		return count;
	}

	public CopyOnWriteArrayList<ir.bilgisoft.toopeto.entities.Conversation> getConversations(int status) {
		CopyOnWriteArrayList<ir.bilgisoft.toopeto.entities.Conversation> list = new CopyOnWriteArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { Integer.toString(status) };
		Cursor cursor = db.rawQuery("select * from " + ir.bilgisoft.toopeto.entities.Conversation.TABLENAME
				+ " where " + ir.bilgisoft.toopeto.entities.Conversation.STATUS + " = ? order by "
				+ ir.bilgisoft.toopeto.entities.Conversation.CREATED + " desc", selectionArgs);
		while (cursor.moveToNext()) {
			list.add(ir.bilgisoft.toopeto.entities.Conversation.fromCursor(cursor));
		}
		cursor.close();
		return list;
	}

	public ArrayList<ir.bilgisoft.toopeto.entities.Message> getMessages(ir.bilgisoft.toopeto.entities.Conversation conversations, int limit) {
		return getMessages(conversations, limit, -1);
	}

	public ArrayList<ir.bilgisoft.toopeto.entities.Message> getMessages(ir.bilgisoft.toopeto.entities.Conversation conversation, int limit,
			long timestamp) {
		ArrayList<ir.bilgisoft.toopeto.entities.Message> list = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor;
		if (timestamp == -1) {
			String[] selectionArgs = { conversation.getUuid() };
			cursor = db.query(ir.bilgisoft.toopeto.entities.Message.TABLENAME, null, ir.bilgisoft.toopeto.entities.Message.CONVERSATION
					+ "=?", selectionArgs, null, null, ir.bilgisoft.toopeto.entities.Message.TIME_SENT
					+ " DESC", String.valueOf(limit));
		} else {
			String[] selectionArgs = { conversation.getUuid(),
					Long.toString(timestamp) };
			cursor = db.query(ir.bilgisoft.toopeto.entities.Message.TABLENAME, null, ir.bilgisoft.toopeto.entities.Message.CONVERSATION
					+ "=? and " + ir.bilgisoft.toopeto.entities.Message.TIME_SENT + "<?", selectionArgs,
					null, null, ir.bilgisoft.toopeto.entities.Message.TIME_SENT + " DESC",
					String.valueOf(limit));
		}
		if (cursor.getCount() > 0) {
			cursor.moveToLast();
			do {
				ir.bilgisoft.toopeto.entities.Message message = ir.bilgisoft.toopeto.entities.Message.fromCursor(cursor);
				message.setConversation(conversation);
				list.add(message);
			} while (cursor.moveToPrevious());
		}
		cursor.close();
		return list;
	}

	public ir.bilgisoft.toopeto.entities.Conversation findConversation(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.jid.Jid contactJid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { account.getUuid(), contactJid.toBareJid().toString() + "%" };
		Cursor cursor = db.query(ir.bilgisoft.toopeto.entities.Conversation.TABLENAME, null,
				ir.bilgisoft.toopeto.entities.Conversation.ACCOUNT + "=? AND " + ir.bilgisoft.toopeto.entities.Conversation.CONTACTJID
						+ " like ?", selectionArgs, null, null, null);
		if (cursor.getCount() == 0)
			return null;
		cursor.moveToFirst();
		ir.bilgisoft.toopeto.entities.Conversation conversation = ir.bilgisoft.toopeto.entities.Conversation.fromCursor(cursor);
		cursor.close();
		return conversation;
	}

	public void updateConversation(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
		final SQLiteDatabase db = this.getWritableDatabase();
		final String[] args = { conversation.getUuid() };
		db.update(ir.bilgisoft.toopeto.entities.Conversation.TABLENAME, conversation.getContentValues(),
				ir.bilgisoft.toopeto.entities.Conversation.UUID + "=?", args);
	}

	public List<ir.bilgisoft.toopeto.entities.Account> getAccounts() {
		List<ir.bilgisoft.toopeto.entities.Account> list = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(ir.bilgisoft.toopeto.entities.Account.TABLENAME, null, null, null, null,
				null, null);
		while (cursor.moveToNext()) {
			list.add(ir.bilgisoft.toopeto.entities.Account.fromCursor(cursor));
		}
		cursor.close();
		return list;
	}

	public void updateAccount(ir.bilgisoft.toopeto.entities.Account account) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { account.getUuid() };
		db.update(ir.bilgisoft.toopeto.entities.Account.TABLENAME, account.getContentValues(), ir.bilgisoft.toopeto.entities.Account.UUID
				+ "=?", args);
	}

	public void deleteAccount(ir.bilgisoft.toopeto.entities.Account account) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { account.getUuid() };
		db.delete(ir.bilgisoft.toopeto.entities.Account.TABLENAME, ir.bilgisoft.toopeto.entities.Account.UUID + "=?", args);
	}

	public boolean hasEnabledAccounts() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("select count(" + ir.bilgisoft.toopeto.entities.Account.UUID + ")  from "
				+ ir.bilgisoft.toopeto.entities.Account.TABLENAME + " where not options & (1 <<1)", null);
		try {
			cursor.moveToFirst();
			int count = cursor.getInt(0);
			cursor.close();
			return (count > 0);
		} catch (SQLiteCantOpenDatabaseException e) {
			return true; // better safe than sorry
		}
	}

	@Override
	public SQLiteDatabase getWritableDatabase() {
		SQLiteDatabase db = super.getWritableDatabase();
		db.execSQL("PRAGMA foreign_keys=ON;");
		return db;
	}

	public void updateMessage(ir.bilgisoft.toopeto.entities.Message message) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { message.getUuid() };
		db.update(ir.bilgisoft.toopeto.entities.Message.TABLENAME, message.getContentValues(), ir.bilgisoft.toopeto.entities.Message.UUID
				+ "=?", args);
	}

	public void readRoster(ir.bilgisoft.toopeto.entities.Roster roster) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor;
		String args[] = { roster.getAccount().getUuid() };
		cursor = db.query(ir.bilgisoft.toopeto.entities.Contact.TABLENAME, null, ir.bilgisoft.toopeto.entities.Contact.ACCOUNT + "=?",
				args, null, null, null);
		while (cursor.moveToNext()) {
			roster.initContact(ir.bilgisoft.toopeto.entities.Contact.fromCursor(cursor));
		}
		cursor.close();
	}

	public void writeRoster(final ir.bilgisoft.toopeto.entities.Roster roster) {
		final ir.bilgisoft.toopeto.entities.Account account = roster.getAccount();
		final SQLiteDatabase db = this.getWritableDatabase();
		for (ir.bilgisoft.toopeto.entities.Contact contact : roster.getContacts()) {
			if (contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.IN_ROSTER)) {
				db.insert(ir.bilgisoft.toopeto.entities.Contact.TABLENAME, null, contact.getContentValues());
			} else {
				String where = ir.bilgisoft.toopeto.entities.Contact.ACCOUNT + "=? AND " + ir.bilgisoft.toopeto.entities.Contact.JID + "=?";
				String[] whereArgs = { account.getUuid(), contact.getJid().toString() };
				db.delete(ir.bilgisoft.toopeto.entities.Contact.TABLENAME, where, whereArgs);
			}
		}
		account.setRosterVersion(roster.getVersion());
		updateAccount(account);
	}

	public void deleteMessage(ir.bilgisoft.toopeto.entities.Message message) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { message.getUuid() };
		db.delete(ir.bilgisoft.toopeto.entities.Message.TABLENAME, ir.bilgisoft.toopeto.entities.Message.UUID + "=?", args);
	}

	public void deleteMessagesInConversation(ir.bilgisoft.toopeto.entities.Conversation conversation) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { conversation.getUuid() };
		db.delete(ir.bilgisoft.toopeto.entities.Message.TABLENAME, ir.bilgisoft.toopeto.entities.Message.CONVERSATION + "=?", args);
	}

	public ir.bilgisoft.toopeto.entities.Conversation findConversationByUuid(String conversationUuid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { conversationUuid };
		Cursor cursor = db.query(ir.bilgisoft.toopeto.entities.Conversation.TABLENAME, null,
				ir.bilgisoft.toopeto.entities.Conversation.UUID + "=?", selectionArgs, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		ir.bilgisoft.toopeto.entities.Conversation conversation = ir.bilgisoft.toopeto.entities.Conversation.fromCursor(cursor);
		cursor.close();
		return conversation;
	}

	public ir.bilgisoft.toopeto.entities.Message findMessageByUuid(String messageUuid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { messageUuid };
		Cursor cursor = db.query(ir.bilgisoft.toopeto.entities.Message.TABLENAME, null, ir.bilgisoft.toopeto.entities.Message.UUID + "=?",
				selectionArgs, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		ir.bilgisoft.toopeto.entities.Message message = ir.bilgisoft.toopeto.entities.Message.fromCursor(cursor);
		cursor.close();
		return message;
	}

	public ir.bilgisoft.toopeto.entities.Account findAccountByUuid(String accountUuid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { accountUuid };
		Cursor cursor = db.query(ir.bilgisoft.toopeto.entities.Account.TABLENAME, null, ir.bilgisoft.toopeto.entities.Account.UUID + "=?",
				selectionArgs, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		ir.bilgisoft.toopeto.entities.Account account = ir.bilgisoft.toopeto.entities.Account.fromCursor(cursor);
		cursor.close();
		return account;
	}

	public List<ir.bilgisoft.toopeto.entities.Message> getImageMessages(ir.bilgisoft.toopeto.entities.Conversation conversation) {
		ArrayList<ir.bilgisoft.toopeto.entities.Message> list = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor;
			String[] selectionArgs = { conversation.getUuid(), String.valueOf(ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE) };
			cursor = db.query(ir.bilgisoft.toopeto.entities.Message.TABLENAME, null, ir.bilgisoft.toopeto.entities.Message.CONVERSATION
					+ "=? AND "+ ir.bilgisoft.toopeto.entities.Message.TYPE+"=?", selectionArgs, null, null,null);
		if (cursor.getCount() > 0) {
			cursor.moveToLast();
			do {
				ir.bilgisoft.toopeto.entities.Message message = ir.bilgisoft.toopeto.entities.Message.fromCursor(cursor);
				message.setConversation(conversation);
				list.add(message);
			} while (cursor.moveToPrevious());
		}
		cursor.close();
		return list;
	}
}
