package ir.bilgisoft.toopeto.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.LruCache;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;

import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import de.duenndns.ssl.MemorizingTrustManager;
import ir.bilgisoft.toopeto.R;
import ir.bilgisoft.toopeto.entities.Message;
import ir.bilgisoft.toopeto.entities.MucOptions;
import ir.bilgisoft.toopeto.entities.MucOptions.OnRenameListener;
import ir.bilgisoft.toopeto.Config;
import ir.bilgisoft.toopeto.entities.Account;
import ir.bilgisoft.toopeto.entities.Bookmark;
import ir.bilgisoft.toopeto.entities.Conversation;
import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.Json;
import ir.bilgisoft.toopeto.json.ListTransferPacket;
import ir.bilgisoft.toopeto.json.MessagePacket;
import ir.bilgisoft.toopeto.json.UserPacket;
import ir.bilgisoft.toopeto.ui.UiCallback;
import ir.bilgisoft.toopeto.xmpp.OnMessageAcknowledged;
import ir.bilgisoft.toopeto.xmpp.XmppConnection;
import ir.bilgisoft.toopeto.xmpp.forms.Data;
import ir.bilgisoft.toopeto.xmpp.forms.Field;
import ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException;
import ir.bilgisoft.toopeto.xmpp.jid.Jid;
import ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket;
import ir.bilgisoft.toopeto.xmpp.stanzas.PresencePacket;

import java.util.UUID;

public class XmppConnectionService extends Service implements ir.bilgisoft.toopeto.utils.OnPhoneContactsLoadedListener {

    public static final String ACTION_CLEAR_NOTIFICATION = "clear_notification";
    private static final String ACTION_MERGE_PHONE_CONTACTS = "merge_phone_contacts";
    public static final String ACTION_DISABLE_FOREGROUND = "disable_foreground";


    private final IBinder mBinder = new XmppConnectionBinder();
    public ir.bilgisoft.toopeto.persistance.DatabaseBackend databaseBackend;

    private int totalRoomCount = 0;
    private String uuid = "";
    private String lastSearchValue = "";
    private int lastRequestIndex = -1;
    private int currentRequestIndex = 0;
    private boolean recentlyRequestReceive = true;
    public final List<ir.bilgisoft.toopeto.entities.Bookmark> rooms = new CopyOnWriteArrayList<>();

    private int totalUsersCount = 0;
    private String uuidUsers = "";
    private String lastSearchValueUsers = "";
    private int lastRequestIndexUsers = -1;
    private int currentRequestIndexUsers = 0;
    public final List<ir.bilgisoft.toopeto.entities.User> users = new CopyOnWriteArrayList<>();

    private ir.bilgisoft.toopeto.persistance.FileBackend fileBackend = new ir.bilgisoft.toopeto.persistance.FileBackend(this);
    private MemorizingTrustManager mMemorizingTrustManager;
    private ir.bilgisoft.toopeto.services.NotificationService mNotificationService = new ir.bilgisoft.toopeto.services.NotificationService(
            this);
    private ir.bilgisoft.toopeto.xmpp.OnMessagePacketReceived mMessageParser = new ir.bilgisoft.toopeto.parser.MessageParser(this);
    private ir.bilgisoft.toopeto.xmpp.OnPresencePacketReceived mPresenceParser = new ir.bilgisoft.toopeto.parser.PresenceParser(this);
    private ir.bilgisoft.toopeto.parser.IqParser mIqParser = new ir.bilgisoft.toopeto.parser.IqParser(this);
    private ir.bilgisoft.toopeto.generator.MessageGenerator mMessageGenerator = new ir.bilgisoft.toopeto.generator.MessageGenerator(this);
    private ir.bilgisoft.toopeto.generator.PresenceGenerator mPresenceGenerator = new ir.bilgisoft.toopeto.generator.PresenceGenerator(this);
    private List<ir.bilgisoft.toopeto.entities.Account> accounts;
    private final List<ir.bilgisoft.toopeto.entities.Conversation> conversations = new CopyOnWriteArrayList<>();
    private ir.bilgisoft.toopeto.xmpp.jingle.JingleConnectionManager mJingleConnectionManager = new ir.bilgisoft.toopeto.xmpp.jingle.JingleConnectionManager(
            this);
    private ir.bilgisoft.toopeto.http.HttpConnectionManager mHttpConnectionManager = new ir.bilgisoft.toopeto.http.HttpConnectionManager(
            this);
    private AvatarService mAvatarService = new AvatarService(this);
    private ir.bilgisoft.toopeto.services.MessageArchiveService mMessageArchiveService = new ir.bilgisoft.toopeto.services.MessageArchiveService(this);
    private OnConversationUpdate mOnConversationUpdate = null;
    private Integer convChangedListenerCount = 0;
    private OnAccountUpdate mOnAccountUpdate = null;

    private ir.bilgisoft.toopeto.xmpp.OnReceivedRooms onReceivedRooms = null;
    private ir.bilgisoft.toopeto.xmpp.OnReceivedRooms onReceivedUsers = null;
    private int accountChangedListenerCount = 0;
    private OnRosterUpdate mOnRosterUpdate = null;
    private ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist mOnUpdateBlocklist = null;
    private int updateBlocklistListenerCount = 0;
    private int rosterChangedListenerCount = 0;
    private OnMucRosterUpdate mOnMucRosterUpdate = null;
    private int mucRosterChangedListenerCount = 0;
    private SecureRandom mRandom;


    private OpenPgpServiceConnection pgpServiceConnection;
    private ir.bilgisoft.toopeto.crypto.PgpEngine mPgpEngine = null;
    private WakeLock wakeLock;
    private PowerManager pm;

    //////////////////////////////// my code //////////////////////////////
    // ejad connection va set kardaneh updaterhaye connection
    public ir.bilgisoft.toopeto.xmpp.XmppConnection createConnection(final Account account) {
        final SharedPreferences sharedPref = getPreferences();
        account.setResource(sharedPref.getString("resource", "mobile")
                .toLowerCase(Locale.getDefault()));
        final XmppConnection connection = new XmppConnection(account, this);
        connection.setOnMessagePacketReceivedListener(this.mMessageParser);
        connection.setOnStatusChangedListener(this.statusListener);//felan moshakhas nist
        connection.setOnPresencePacketReceivedListener(this.mPresenceParser);
        connection.setOnUnregisteredIqPacketReceivedListener(this.mIqParser);
        connection.setOnJinglePacketReceivedListener(this.jingleListener);//felan lazm nist
        //zaman shoru connection in method call migardad va yeksari karkayeh avlihe anjam mishavad
        connection.setOnBindListener(this.mOnBindListener);
        connection.setOnMessageAcknowledgeListener(this.mOnMessageAcknowledgedListener);
        //karbord an kamel meshakhs nashode
        connection.addOnAdvancedStreamFeaturesAvailableListener(this.mMessageArchiveService);
        return connection;
    }

    private final OnMessageAcknowledged mOnMessageAcknowledgedListener = new OnMessageAcknowledged() {
        ///zamani ke Acknowledged barayeh yek goftego miresad in method az interface da class connection call migardadd
        @Override
        public void onMessageAcknowledged(Account account, String uuid) {
            for (final Conversation conversation : getConversations()) {
                if (conversation.getAccount() == account) {
                    Message message = conversation.findUnsentMessageWithUuid(uuid);
                    if (message != null) {
                        markMessage(message, Message.STATUS_SEND);
                        if (conversation.setLastMessageTransmitted(System.currentTimeMillis())) {
                            databaseBackend.updateConversation(conversation);
                        }
                    }
                }
            }
        }
    };

    public void joinMuc(ir.bilgisoft.toopeto.entities.Conversation conversation) {
        ir.bilgisoft.toopeto.entities.Account account = conversation.getAccount();
        account.pendingConferenceJoins.remove(conversation);
        account.pendingConferenceLeaves.remove(conversation);
        if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
            final String nick = conversation.getMucOptions().getProposedNick();
            final ir.bilgisoft.toopeto.xmpp.jid.Jid joinJid = conversation.getMucOptions().createJoinJid(nick);
            if (joinJid == null) {
                return; //safety net
            }
            // Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid().toString() + ": joining conversation " + joinJid.toString());
            UserPacket packet = new UserPacket();
            packet.to = conversation.getJid().getLocalpart(); //joinJid.getLocalpart();
            packet.receiver = Enums.ReceiverEnum.room.toString();
            packet.type = Enums.UserTypeEnum.presence.toString();
            //packet.setFrom(conversation.getAccount().getJid());
            //packet.setTo(joinJid);
            //ir.bilgisoft.toopeto.xml.Element x = packet.addChild("x","http://jabber.org/protocol/muc");
            if (conversation.getMucOptions().getPassword() != null) {
                //x.addChild("password").setContent(conversation.getMucOptions().getPassword());
                packet.password = conversation.getMucOptions().getPassword();
            }
            packet.date = String.valueOf(conversation.getLastMessageTransmitted());
            //x.addChild("history").setAttribute("since", ir.bilgisoft.toopeto.generator.PresenceGenerator.getTimestamp(conversation.getLastMessageTransmitted()));
            String sig = account.getPgpSignature();

            sendPresencePacket(account);
            if (!joinJid.equals(conversation.getJid())) {
                conversation.setContactJid(joinJid);
                databaseBackend.updateConversation(conversation);
            }
        } else {
            account.pendingConferenceJoins.add(conversation);
        }
    }

    //
    public void sendMessage(final ir.bilgisoft.toopeto.entities.Message message) {
        final ir.bilgisoft.toopeto.entities.Account account = message.getConversation().getAccount();
        account.deactivateGracePeriod();
        final ir.bilgisoft.toopeto.entities.Conversation conv = message.getConversation();
        MessagePacket packet = null;
        boolean saveInDb = true;
        boolean send = false;
        if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE
                && account.getXmppConnection() != null) {
            if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
                if (message.getCounterpart() != null) {
                    if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR) {
                        if (!conv.hasValidOtrSession()) {
                            conv.startOtrSession(message.getCounterpart().getResourcepart(), true);
                            message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING);
                        } else if (conv.hasValidOtrSession()
                                && conv.getOtrSession().getSessionStatus() == SessionStatus.ENCRYPTED) {
                            mJingleConnectionManager
                                    .createNewConnection(message);
                        }
                    } else {
                        mJingleConnectionManager.createNewConnection(message);
                    }
                } else {
                    if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR) {
                        conv.startOtrIfNeeded();
                    }
                    message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING);
                }
            } else {
                Log.d("error :", "ersal payam encrypt scenario");
            }
            if (!account.getXmppConnection().getFeatures().sm()
                    && conv.getMode() != ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
                message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_SEND);
            }
        } else {
            message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING);
            if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_TEXT) {
                if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED) {
                    String pgpBody = message.getEncryptedBody();
                    String decryptedBody = message.getBody();
                    message.setBody(pgpBody);
                    message.setEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP);
                    databaseBackend.createMessage(message);
                    saveInDb = false;
                    message.setBody(decryptedBody);
                    message.setEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED);
                } else if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR) {
                    if (!conv.hasValidOtrSession()
                            && message.getCounterpart() != null) {
                        conv.startOtrSession(message.getCounterpart().getResourcepart(), false);
                    }
                }
            }

        }
        conv.add(message);
        if (saveInDb) {
            if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE
                    || saveEncryptedMessages()) {
                databaseBackend.createMessage(message);
            }
        }
        if ((send) && (packet != null)) {
            sendMessagePacket(account, packet);
        }
        updateConversationUi();
    }

    public void populateWithOrderedConversations(final List<ir.bilgisoft.toopeto.entities.Conversation> list) {
        populateWithOrderedConversations(list, true);
    }

    //scenario 2 : por kardan list goftegohayeh anjam shodeh
    public void populateWithOrderedConversations(final List<ir.bilgisoft.toopeto.entities.Conversation> list, boolean includeConferences) {
        list.clear();
        if (includeConferences) {
            list.addAll(getConversations());
        } else {
            for (ir.bilgisoft.toopeto.entities.Conversation conversation : getConversations()) {
                if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE) {
                    list.add(conversation);
                }
            }
        }
        Collections.sort(list, new Comparator<ir.bilgisoft.toopeto.entities.Conversation>() {
            @Override
            public int compare(ir.bilgisoft.toopeto.entities.Conversation lhs, ir.bilgisoft.toopeto.entities.Conversation rhs) {
                ir.bilgisoft.toopeto.entities.Message left = lhs.getLatestMessage();
                ir.bilgisoft.toopeto.entities.Message right = rhs.getLatestMessage();
                if (left.getTimeSent() > right.getTimeSent()) {
                    return -1;
                } else if (left.getTimeSent() < right.getTimeSent()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    private final ir.bilgisoft.toopeto.xmpp.OnBindListener mOnBindListener = new ir.bilgisoft.toopeto.xmpp.OnBindListener() {
        @Override
        //in method zaman bind avalie be server call mishavad
        public void onBind(final ir.bilgisoft.toopeto.entities.Account account) {
            account.getRoster().clearPresences();
            account.pendingConferenceJoins.clear();
            account.pendingConferenceLeaves.clear();
            //list contactHaye karbar ra fetch mikonad
            fetchRosterFromServer(account);
            fetchBookmarks(account);//felan lazm nist va khali hast
            sendPresencePacket(account);
            connectMultiModeConversations(account);
            updateConversationUi();
        }
    };
    //////////////end my code ///////////
    private ir.bilgisoft.toopeto.xmpp.OnStatusChanged statusListener = new ir.bilgisoft.toopeto.xmpp.OnStatusChanged() {

        @Override
        public void onStatusChanged(ir.bilgisoft.toopeto.entities.Account account) {
            ir.bilgisoft.toopeto.xmpp.XmppConnection connection = account.getXmppConnection();
            if (mOnAccountUpdate != null) {
                mOnAccountUpdate.onAccountUpdate();
            }
            if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
                for (ir.bilgisoft.toopeto.entities.Conversation conversation : account.pendingConferenceLeaves) {
                    leaveMuc(conversation);
                }
                for (ir.bilgisoft.toopeto.entities.Conversation conversation : account.pendingConferenceJoins) {
                    joinMuc(conversation);
                }
                mMessageArchiveService.executePendingQueries(account);
                mJingleConnectionManager.cancelInTransmission();
                List<ir.bilgisoft.toopeto.entities.Conversation> conversations = getConversations();
                for (ir.bilgisoft.toopeto.entities.Conversation conversation : conversations) {
                    if (conversation.getAccount() == account) {
                        conversation.startOtrIfNeeded();
                        sendUnsentMessages(conversation);
                    }
                }
                if (connection != null && connection.getFeatures().csi()) {
                    if (checkListeners()) {
                        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
                                + " sending csi//inactive");
                        connection.sendInactive();
                    } else {
                        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
                                + " sending csi//active");
                        connection.sendActive();
                    }
                }
                syncDirtyContacts(account);
                scheduleWakeUpCall(ir.bilgisoft.toopeto.Config.PING_MAX_INTERVAL, account.getUuid().hashCode());
            } else if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.OFFLINE) {
                resetSendingToWaiting(account);
                if (!account.isOptionSet(ir.bilgisoft.toopeto.entities.Account.OPTION_DISABLED)) {
                    int timeToReconnect = mRandom.nextInt(50) + 10;
                    scheduleWakeUpCall(timeToReconnect, account.getUuid().hashCode());
                }
            } else if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.REGISTRATION_SUCCESSFUL) {
                databaseBackend.updateAccount(account);
                reconnectAccount(account, true);
            } else if ((account.getStatus() != ir.bilgisoft.toopeto.entities.Account.State.CONNECTING)
                    && (account.getStatus() != ir.bilgisoft.toopeto.entities.Account.State.NO_INTERNET)) {
                if (connection != null) {
                    int next = connection.getTimeToNextAttempt();
                    Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
                            + ": error connecting account. try again in "
                            + next + "s for the "
                            + (connection.getAttempt() + 1) + " time");
                    scheduleWakeUpCall(next, account.getUuid().hashCode());
                }
            }
            getNotificationService().updateErrorNotification();
        }
    };

    public ir.bilgisoft.toopeto.xmpp.OnContactStatusChanged onContactStatusChanged = new ir.bilgisoft.toopeto.xmpp.OnContactStatusChanged() {

        @Override
        public void onContactStatusChanged(ir.bilgisoft.toopeto.entities.Contact contact, boolean online) {
            ir.bilgisoft.toopeto.entities.Conversation conversation = find(getConversations(), contact);
            if (conversation != null) {
                if (online && contact.getPresences().size() > 1) {
                    conversation.endOtrIfNeeded();
                } else {
                    conversation.resetOtrSession();
                }
                if (online && (contact.getPresences().size() == 1)) {
                    sendUnsentMessages(conversation);
                }
            }
        }
    };


    private final FileObserver fileObserver = new FileObserver(
            ir.bilgisoft.toopeto.persistance.FileBackend.getConversationsImageDirectory()) {

        @Override
        public void onEvent(int event, String path) {
            if (event == FileObserver.DELETE) {
                markFileDeleted(path.split("\\.")[0]);
            }
        }
    };
    private final ir.bilgisoft.toopeto.xmpp.jingle.OnJinglePacketReceived jingleListener = new ir.bilgisoft.toopeto.xmpp.jingle.OnJinglePacketReceived() {

        @Override
        public void onJinglePacketReceived(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet) {
            mJingleConnectionManager.deliverPacket(account, packet);
        }
    };

    private ContentObserver contactObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Intent intent = new Intent(getApplicationContext(),
                    XmppConnectionService.class);
            intent.setAction(ACTION_MERGE_PHONE_CONTACTS);
            startService(intent);
        }
    };

    private LruCache<String, Bitmap> mBitmapCache;
    private final ir.bilgisoft.toopeto.generator.IqGenerator mIqGenerator = new ir.bilgisoft.toopeto.generator.IqGenerator(this);
    private Thread mPhoneContactMergerThread;

    public ir.bilgisoft.toopeto.crypto.PgpEngine getPgpEngine() {
        if (pgpServiceConnection.isBound()) {
            if (this.mPgpEngine == null) {
                this.mPgpEngine = new ir.bilgisoft.toopeto.crypto.PgpEngine(new OpenPgpApi(
                        getApplicationContext(),
                        pgpServiceConnection.getService()), this);
            }
            return mPgpEngine;
        } else {
            return null;
        }

    }

    public void setOnReceivedRooms(final ir.bilgisoft.toopeto.xmpp.OnReceivedRooms onReceivedRooms) {
        this.onReceivedRooms = onReceivedRooms;
    }

    public ir.bilgisoft.toopeto.persistance.FileBackend getFileBackend() {
        return this.fileBackend;
    }

    public AvatarService getAvatarService() {
        return this.mAvatarService;
    }

    public void attachFileToConversation(final ir.bilgisoft.toopeto.entities.Conversation conversation,
                                         final Uri uri,
                                         final ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.entities.Message> callback) {
        final ir.bilgisoft.toopeto.entities.Message message;
        if (conversation.getNextEncryption(forceEncryption()) == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
            message = new ir.bilgisoft.toopeto.entities.Message(conversation, "",
                    ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED);
        } else {
            message = new ir.bilgisoft.toopeto.entities.Message(conversation, "",
                    conversation.getNextEncryption(forceEncryption()));
        }
        message.setCounterpart(conversation.getNextCounterpart());
        message.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_FILE);
        message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_OFFERED);
        String path = getFileBackend().getOriginalPath(uri);
        if (path != null) {
            message.setRelativeFilePath(path);
            getFileBackend().updateFileParams(message);
            if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED) {
                getPgpEngine().encrypt(message, callback);
            } else {
                callback.success(message);
            }
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getFileBackend().copyFileToPrivateStorage(message, uri);
                        getFileBackend().updateFileParams(message);
                        if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED) {
                            getPgpEngine().encrypt(message, callback);
                        } else {
                            callback.success(message);
                        }
                    } catch (ir.bilgisoft.toopeto.persistance.FileBackend.FileCopyException e) {
                        callback.error(e.getResId(), message);
                    }
                }
            }).start();

        }
    }

    public void attachImageToConversation(final ir.bilgisoft.toopeto.entities.Conversation conversation,
                                          final Uri uri, final ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.entities.Message> callback) {
        final ir.bilgisoft.toopeto.entities.Message message;
        if (conversation.getNextEncryption(forceEncryption()) == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
            message = new ir.bilgisoft.toopeto.entities.Message(conversation, "",
                    ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED);
        } else {
            message = new ir.bilgisoft.toopeto.entities.Message(conversation, "",
                    conversation.getNextEncryption(forceEncryption()));
        }
        message.setCounterpart(conversation.getNextCounterpart());
        message.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE);
        message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_OFFERED);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    getFileBackend().copyImageToPrivateStorage(message, uri);
                    if (conversation.getNextEncryption(forceEncryption()) == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
                        getPgpEngine().encrypt(message, callback);
                    } else {
                        callback.success(message);
                    }
                } catch (final ir.bilgisoft.toopeto.persistance.FileBackend.FileCopyException e) {
                    callback.error(e.getResId(), message);
                }
            }
        }).start();
    }

    public ir.bilgisoft.toopeto.entities.Conversation find(ir.bilgisoft.toopeto.entities.Bookmark bookmark) {
        return find(bookmark.getAccount(), bookmark.getJid().getLocalpart());
    }

    public ir.bilgisoft.toopeto.entities.Conversation find(final Account account, final String jid) {
        return find(getConversations(), account, jid);
    }

    //
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent == null ? null : intent.getAction();
        if (action != null) {
            if (action.equals(ACTION_MERGE_PHONE_CONTACTS)) {
                ir.bilgisoft.toopeto.utils.PhoneHelper.loadPhoneContacts(getApplicationContext(), new ArrayList<Bundle>(), this);
                return START_STICKY;
            } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
                logoutAndSave();
                return START_NOT_STICKY;
            } else if (action.equals(ACTION_CLEAR_NOTIFICATION)) {
                mNotificationService.clear();
            } else if (action.equals(ACTION_DISABLE_FOREGROUND)) {
                getPreferences().edit().putBoolean("keep_foreground_service", false).commit();
                toggleForegroundService();
            }
        }
        this.wakeLock.acquire();

        for (ir.bilgisoft.toopeto.entities.Account account : accounts) {
            if (!account.isOptionSet(ir.bilgisoft.toopeto.entities.Account.OPTION_DISABLED)) {
                if (!hasInternetConnection()) {
                    account.setStatus(ir.bilgisoft.toopeto.entities.Account.State.NO_INTERNET);
                    if (statusListener != null) {
                        statusListener.onStatusChanged(account);
                    }
                } else {
                    if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.NO_INTERNET) {
                        account.setStatus(ir.bilgisoft.toopeto.entities.Account.State.OFFLINE);
                        if (statusListener != null) {
                            statusListener.onStatusChanged(account);
                        }
                    }
                    if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
                        long lastReceived = account.getXmppConnection().getLastPacketReceived();
                        long lastSent = account.getXmppConnection().getLastPingSent();
                        long pingInterval = "ui".equals(action) ? ir.bilgisoft.toopeto.Config.PING_MIN_INTERVAL * 1000 : ir.bilgisoft.toopeto.Config.PING_MAX_INTERVAL * 1000;
                        long secondsToNextPing = ((lastReceived + pingInterval) - SystemClock.elapsedRealtime()) / 1000;
                        if (lastSent > lastReceived && (lastSent + ir.bilgisoft.toopeto.Config.PING_TIMEOUT * 1000) < SystemClock.elapsedRealtime()) {
                            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + ": ping timeout");
                            this.reconnectAccount(account, true);
                        } else if (secondsToNextPing <= 0) {
                            account.getXmppConnection().sendPing();
                            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + " send ping");
                            this.scheduleWakeUpCall(ir.bilgisoft.toopeto.Config.PING_TIMEOUT, account.getUuid().hashCode());
                        } else {
                            this.scheduleWakeUpCall((int) secondsToNextPing, account.getUuid().hashCode());
                        }
                    } else if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.OFFLINE) {
                        if (account.getXmppConnection() == null) {
                            account.setXmppConnection(this.createConnection(account));
                        }
                        new Thread(account.getXmppConnection()).start();
                    } else if ((account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.CONNECTING)
                            && ((SystemClock.elapsedRealtime() - account
                            .getXmppConnection().getLastConnect()) / 1000 >= ir.bilgisoft.toopeto.Config.CONNECT_TIMEOUT)) {
                        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid() + ": time out during connect reconnecting");
                        reconnectAccount(account, true);
                    } else {
                        if (account.getXmppConnection().getTimeToNextAttempt() <= 0) {
                            reconnectAccount(account, true);
                        }
                    }

                }
                if (mOnAccountUpdate != null) {
                    mOnAccountUpdate.onAccountUpdate();
                }
            }
        }
        /*PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
			removeStaleListeners();
			}*/
        if (wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (final RuntimeException ignored) {
            }
        }
        return START_STICKY;
    }

    public boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @SuppressLint("TrulyRandom")
    @Override
    public void onCreate() {
        ir.bilgisoft.toopeto.utils.ExceptionHelper.init(getApplicationContext());
        ir.bilgisoft.toopeto.utils.PRNGFixes.apply();
        this.mRandom = new SecureRandom();
        this.mMemorizingTrustManager = new MemorizingTrustManager(
                getApplicationContext());

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        this.mBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(final String key, final Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        this.databaseBackend = ir.bilgisoft.toopeto.persistance.DatabaseBackend.getInstance(getApplicationContext());
        //list account ha ra daryaft mikonad
        this.accounts = databaseBackend.getAccounts();

        for (final ir.bilgisoft.toopeto.entities.Account account : this.accounts) {
            account.initOtrEngine(this);
            this.databaseBackend.readRoster(account.getRoster());
        }
        initConversations();
        ir.bilgisoft.toopeto.utils.PhoneHelper.loadPhoneContacts(getApplicationContext(), new ArrayList<Bundle>(), this);

        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactObserver);
        this.fileObserver.startWatching();
        this.pgpServiceConnection = new OpenPgpServiceConnection(getApplicationContext(), "org.sufficientlysecure.keychain");
        this.pgpServiceConnection.bindToService();

        this.pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XmppConnectionService");
        toggleForegroundService();
    }

    public void toggleForegroundService() {
        if (getPreferences().getBoolean("keep_foreground_service", false)) {
            startForeground(ir.bilgisoft.toopeto.services.NotificationService.FOREGROUND_NOTIFICATION_ID, this.mNotificationService.createForegroundNotification());
        } else {
            stopForeground(true);
        }
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (!getPreferences().getBoolean("keep_foreground_service", false)) {
            this.logoutAndSave();
        }
    }
    @Override
    public void onDestroy(){
        short x=1+2;
    }
    private void logoutAndSave() {
        for (final ir.bilgisoft.toopeto.entities.Account account : accounts) {
            databaseBackend.writeRoster(account.getRoster());
            if (account.getXmppConnection() != null) {
                disconnect(account, false);
            }
        }
        Context context = getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ir.bilgisoft.toopeto.services.EventReceiver.class);
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, intent, 0));
        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "good bye");
        stopSelf();
    }

    protected void scheduleWakeUpCall(int seconds, int requestCode) {
        final long timeToWake = SystemClock.elapsedRealtime() + seconds * 1000;

        Context context = getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ir.bilgisoft.toopeto.services.EventReceiver.class);
        intent.setAction("ping");
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, requestCode, intent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeToWake, alarmIntent);
    }


    private void sendUnsentMessages(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
        conversation.findWaitingMessages(new ir.bilgisoft.toopeto.entities.Conversation.OnMessageFound() {

            @Override
            public void onMessageFound(ir.bilgisoft.toopeto.entities.Message message) {
                resendMessage(message);
            }
        });
    }

    private void resendMessage(final ir.bilgisoft.toopeto.entities.Message message) {
        ir.bilgisoft.toopeto.entities.Account account = message.getConversation().getAccount();
        MessagePacket packet = null;
        if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR) {
            ir.bilgisoft.toopeto.entities.Presences presences = message.getConversation().getContact()
                    .getPresences();
            if (!message.getConversation().hasValidOtrSession()) {
                if ((message.getCounterpart() != null)
                        && (presences.has(message.getCounterpart().getResourcepart()))) {
                    message.getConversation().startOtrSession(message.getCounterpart().getResourcepart(), true);
                } else {
                    if (presences.size() == 1) {
                        String presence = presences.asStringArray()[0];
                        message.getConversation().startOtrSession(presence, true);
                    }
                }
            } else {
                if (message.getConversation().getOtrSession()
                        .getSessionStatus() == SessionStatus.ENCRYPTED) {
                    try {
                        message.setCounterpart(ir.bilgisoft.toopeto.xmpp.jid.Jid.fromSessionID(message.getConversation().getOtrSession().getSessionID()));
                        if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_TEXT) {
                            packet = mMessageGenerator.generateOtrChat(message, true);
                        } else if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
                            mJingleConnectionManager.createNewConnection(message);
                        }
                    } catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException ignored) {

                    }
                }
            }
        } else if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_TEXT) {
            if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE) {
                packet = mMessageGenerator.generateChat(message, true);
            } else if ((message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED)
                    || (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP)) {
                //packet = mMessageGenerator.generatePgpChat(message, true);
                packet = null;
                Log.d("error :", "errsal encrypt nadarim");
            }
        } else if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
            ir.bilgisoft.toopeto.entities.Contact contact = message.getConversation().getContact();
            ir.bilgisoft.toopeto.entities.Presences presences = contact.getPresences();
            if ((message.getCounterpart() != null)
                    && (presences.has(message.getCounterpart().getResourcepart()))) {
                markMessage(message, ir.bilgisoft.toopeto.entities.Message.STATUS_OFFERED);
                mJingleConnectionManager.createNewConnection(message);
            } else {
                if (presences.size() == 1) {
                    String presence = presences.asStringArray()[0];
                    try {
                        message.setCounterpart(ir.bilgisoft.toopeto.xmpp.jid.Jid.fromParts(contact.getJid().getLocalpart(), contact.getJid().getDomainpart(), presence));
                    } catch (ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
                        return;
                    }
                    markMessage(message, ir.bilgisoft.toopeto.entities.Message.STATUS_OFFERED);
                    mJingleConnectionManager.createNewConnection(message);
                }
            }
        }
        if (packet != null) {
            if (!account.getXmppConnection().getFeatures().sm()
                    && message.getConversation().getMode() != ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
                markMessage(message, ir.bilgisoft.toopeto.entities.Message.STATUS_SEND);
            } else {
                markMessage(message, ir.bilgisoft.toopeto.entities.Message.STATUS_UNSEND);
            }
            sendMessagePacket(account, packet);
        }
    }

    public void fetchRosterFromServer(final ir.bilgisoft.toopeto.entities.Account account) {
        /*
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iqPacket = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
		if (!"".equals(account.getRosterVersion())) {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
					+ ": fetching roster version " + account.getRosterVersion());
		} else {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + ": fetching roster");
		}
		iqPacket.query(ir.bilgisoft.toopeto.utils.Xmlns.ROSTER).setAttribute("ver",
				account.getRosterVersion());
        */
        ListTransferPacket listTransferPacket = new ListTransferPacket();
        listTransferPacket.type = Enums.ListTransferEnum.getContact.toString();

        account.getXmppConnection().sendIqPacket(listTransferPacket, mIqParser);
    }

    //farge bookmark va contact dar add shodan tavasot user ya add shodan dar zamaneh aghaz sohbat mibashad
    public void fetchBookmarks(final ir.bilgisoft.toopeto.entities.Account account) {
       /* comment bardashte shava va daryaft list dostan anjam shavad */
        //final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iqPacket = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
        //final ir.bilgisoft.toopeto.xml.Element query = iqPacket.query("jabber:iq:private");
        //query.addChild("storage", "storage:bookmarks");
        ListTransferPacket listTransferPacket = new ListTransferPacket();
        listTransferPacket.type = Enums.ListTransferEnum.getContact.toString();
        final ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived callback = new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
            /*
                        public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
                            final ir.bilgisoft.toopeto.xml.Element query = packet.query();
                            final List<ir.bilgisoft.toopeto.entities.Bookmark> bookmarks = new CopyOnWriteArrayList<>();
                            final ir.bilgisoft.toopeto.xml.Element storage = query.findChild("storage",
                                    "storage:bookmarks");
                            if (storage != null) {
                                for (final ir.bilgisoft.toopeto.xml.Element item : storage.getChildren()) {
                                    if (item.getName().equals("conference")) {
                                        final ir.bilgisoft.toopeto.entities.Bookmark bookmark = ir.bilgisoft.toopeto.entities.Bookmark.parse(item, account);
                                        bookmarks.add(bookmark);
                                        ir.bilgisoft.toopeto.entities.Conversation conversation = find(bookmark);
                                        if (conversation != null) {
                                            conversation.setBookmark(bookmark);
                                        } else if (bookmark.autojoin() && bookmark.getJid() != null) {
                                            conversation = findOrCreateConversation(
                                                    account, bookmark.getJid(), true);
                                            conversation.setBookmark(bookmark);
                                            joinMuc(conversation);
                                        }
                                    }
                                }
                            }
                            account.setBookmarks(bookmarks);
                        }
            */
            @Override
            public void onIqPacketReceived(Account account, String jsonString) {/*
                for (final ir.bilgisoft.toopeto.xml.Element item : storage.getChildren()) {
                    if (item.getName().equals("conference")) {
                        final ir.bilgisoft.toopeto.entities.Bookmark bookmark = ir.bilgisoft.toopeto.entities.Bookmark.parse(item, account);
                        bookmarks.add(bookmark);
                        ir.bilgisoft.toopeto.entities.Conversation conversation = find(bookmark);
                        if (conversation != null) {
                            conversation.setBookmark(bookmark);
                        } else if (bookmark.autojoin() && bookmark.getJid() != null) {
                            conversation = findOrCreateConversation(
                                    account, bookmark.getJid(), true);
                            conversation.setBookmark(bookmark);
                            joinMuc(conversation);
                        }
                    }
                }*/
            }
        };
        //	sendIqPacket(account, iqPacket, callback);

    }

    public void pushBookmarks(ir.bilgisoft.toopeto.entities.Account account) {
        ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iqPacket = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
        ir.bilgisoft.toopeto.xml.Element query = iqPacket.query("jabber:iq:private");
        ir.bilgisoft.toopeto.xml.Element storage = query.addChild("storage", "storage:bookmarks");
        for (ir.bilgisoft.toopeto.entities.Bookmark bookmark : account.getBookmarks()) {
            storage.addChild(bookmark);
        }
        //  sendIqPacket(account, iqPacket, null);
    }

    private boolean ifChangeSearchValueClearRooms(String searchValue) {
        if (!lastSearchValue.equals(searchValue)) {
            rooms.clear();
            lastSearchValue = searchValue;
            currentRequestIndex = 0;
            onReceivedRooms.onReceivedRooms();
            return true;
        } else {
            return false;
        }
    }

    private boolean ifChangeRequestIndex() {
        if (lastRequestIndex != currentRequestIndex) {
            lastRequestIndex = currentRequestIndex;
            return true;
        } else {
            return false;
        }
    }

    private boolean ifChangeSearchValueClearUsers(String searchValue) {
        if (!lastSearchValueUsers.equals(searchValue)) {
            users.clear();
            lastSearchValueUsers = searchValue;
            currentRequestIndexUsers = 0;
            //   onReceivedUsers.onReceivedRooms();
            return true;
        } else {
            return false;
        }
    }

    private boolean ifChangeRequestIndexUsers() {
        if (lastRequestIndexUsers != currentRequestIndexUsers) {
            lastRequestIndexUsers = currentRequestIndex;
            return true;
        } else {
            return false;
        }
    }

    public void fetchRooms(final ir.bilgisoft.toopeto.entities.Account account, final String currentSearchValue) {
        //daryaft list roomha
        currentRequestIndex = rooms.size();
        if (ifChangeSearchValueClearRooms(currentSearchValue) | ifChangeRequestIndex()) {
            ListTransferPacket iqListTransferPacket = new ListTransferPacket();
            iqListTransferPacket.type = Enums.ListTransferEnum.getRooms.toString();
            uuid = iqListTransferPacket.id;
            iqListTransferPacket.currentNumber = rooms.size();
            final ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived callback = new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
                @Override
                public void onIqPacketReceived(final Account account, final String jsonString) {
                    try {
                        ListTransferPacket listTransferPacket = ListTransferPacket.GetListTransferPacket(jsonString);
                        if (uuid.equals(listTransferPacket.id)) {
                            totalRoomCount = listTransferPacket.count;
                            for (final String room : listTransferPacket.list) {
                                final Bookmark bookmark = new Bookmark(account, Jid.fromParts(room));
                                rooms.add(bookmark);
                            }
                            Log.d("count :", "count xmppConnection.rooms" + Integer.toString(rooms.size()));
                            onReceivedRooms.onReceivedRooms();
                        }
                    } catch (Exception e) {
                        Log.d("error new :", "in handle onReceivedRooms");
                    }
                }
            };
            sendIqPacket(account, iqListTransferPacket, callback);
            //onReceivedRooms.onReceivedRooms();
        }


    }

    public void fetchUsers(final ir.bilgisoft.toopeto.entities.Account account, final String currentSearchValue) {
      /* daryaft list karbaran Online
        currentRequestIndexUsers = users.size();
        if (ifChangeSearchValueClearUsers(currentSearchValue) | ifChangeRequestIndexUsers() )
        {
            uuidUsers= UUID.randomUUID().toString();
            final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iqPacket =mIqGenerator.generateSearchUsers(currentSearchValue
                    ,currentRequestIndexUsers,account.getServer().toString());
            iqPacket.setAttribute("id", uuidUsers);
            final ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived callback = new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
                @Override
                public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
                    try {
                        if(uuidUsers.equals(packet.getAttribute("id"))){
                            final ir.bilgisoft.toopeto.xml.Element query = packet.query();
                            final ir.bilgisoft.toopeto.xml.Element xPacket = query.findChild("x");

                            final ir.bilgisoft.toopeto.xml.Element setElm = query.findChild("set");

                            if (xPacket != null && setElm != null) {
                                final ir.bilgisoft.toopeto.xml.Element countElm = setElm.findChild("count");
                                totalUsersCount = Integer.parseInt(countElm.getContent());
                                for (final ir.bilgisoft.toopeto.xml.Element item : xPacket.getChildren()) {
                                    if (item.getName().equals("item")) {
                                        final ir.bilgisoft.toopeto.entities.User user = ir.bilgisoft.toopeto.entities.User.parse(mIqParser.searchItemToUser(item), account);
                                        //  if (!rooms.contains(bookmark)) {
                                        users.add(user);
                                        //  }
                                    }
                                }
                            }
                            Log.d("count :","count xmppConnection.rooms"+Integer.toString( users.size()));
                          //  onReceivedUsers.onReceivedRooms();

                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("error new :","in handle onReceivedRooms");
                    }
                }
            };
            sendIqPacket(account, iqPacket, callback);

        }
        */
    }

    public void onPhoneContactsLoaded(final List<Bundle> phoneContacts) {
        if (mPhoneContactMergerThread != null) {
            mPhoneContactMergerThread.interrupt();
        }
        mPhoneContactMergerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "start merging phone contacts with roster");
                for (ir.bilgisoft.toopeto.entities.Account account : accounts) {
                    account.getRoster().clearSystemAccounts();
                    for (Bundle phoneContact : phoneContacts) {
                        if (Thread.interrupted()) {
                            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "interrupted merging phone contacts");
                            return;
                        }
                        ir.bilgisoft.toopeto.xmpp.jid.Jid jid;
                        try {
                            jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(phoneContact.getString("jid"));
                        } catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
                            continue;
                        }
                        final ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(jid.getLocalpart());
                        String systemAccount = phoneContact.getInt("phoneid")
                                + "#"
                                + phoneContact.getString("lookup");
                        contact.setSystemAccount(systemAccount);
                        contact.setPhotoUri(phoneContact.getString("photouri"));
                        getAvatarService().clear(contact);
                        contact.setSystemName(phoneContact.getString("displayname"));
                    }
                }
                Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "finished merging phone contacts");
                updateAccountUi();
            }
        });
        mPhoneContactMergerThread.start();
    }

    //list goftegoharo fetch mikoneh
    private void initConversations() {
        synchronized (this.conversations) {
            final Map<String, ir.bilgisoft.toopeto.entities.Account> accountLookupTable = new Hashtable<>();
            for (ir.bilgisoft.toopeto.entities.Account account : this.accounts) {
                accountLookupTable.put(account.getUuid(), account);
            }
            this.conversations.addAll(databaseBackend.getConversations(ir.bilgisoft.toopeto.entities.Conversation.STATUS_AVAILABLE));
            for (ir.bilgisoft.toopeto.entities.Conversation conversation : this.conversations) {
                ir.bilgisoft.toopeto.entities.Account account = accountLookupTable.get(conversation.getAccountUuid());
                conversation.setAccount(account);
                conversation.addAll(0, databaseBackend.getMessages(conversation, ir.bilgisoft.toopeto.Config.PAGE_SIZE));
                checkDeletedFiles(conversation);
            }
        }
    }

    public List<ir.bilgisoft.toopeto.entities.Conversation> getConversations() {
        return this.conversations;
    }

    private void checkDeletedFiles(ir.bilgisoft.toopeto.entities.Conversation conversation) {
        conversation.findMessagesWithFiles(new ir.bilgisoft.toopeto.entities.Conversation.OnMessageFound() {

            @Override
            public void onMessageFound(ir.bilgisoft.toopeto.entities.Message message) {
                if (!getFileBackend().isFileAvailable(message)) {
                    message.setDownloadable(new ir.bilgisoft.toopeto.entities.DownloadablePlaceholder(ir.bilgisoft.toopeto.entities.Downloadable.STATUS_DELETED));
                }
            }
        });
    }

    private void markFileDeleted(String uuid) {
        for (ir.bilgisoft.toopeto.entities.Conversation conversation : getConversations()) {
            ir.bilgisoft.toopeto.entities.Message message = conversation.findMessageWithFileAndUuid(uuid);
            if (message != null) {
                if (!getFileBackend().isFileAvailable(message)) {
                    message.setDownloadable(new ir.bilgisoft.toopeto.entities.DownloadablePlaceholder(ir.bilgisoft.toopeto.entities.Downloadable.STATUS_DELETED));
                    updateConversationUi();
                }
                return;
            }
        }
    }


    public void loadMoreMessages(final ir.bilgisoft.toopeto.entities.Conversation conversation, final long timestamp, final OnMoreMessagesLoaded callback) {
        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "load more messages for " + conversation.getName() + " prior to " + ir.bilgisoft.toopeto.generator.MessageGenerator.getTimestamp(timestamp));
        if (XmppConnectionService.this.getMessageArchiveService().queryInProgress(conversation, callback)) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ir.bilgisoft.toopeto.entities.Account account = conversation.getAccount();
                List<ir.bilgisoft.toopeto.entities.Message> messages = databaseBackend.getMessages(conversation, 50, timestamp);
                if (messages.size() > 0) {
                    conversation.addAll(0, messages);
                    callback.onMoreMessagesLoaded(messages.size(), conversation);
                } else if (conversation.hasMessagesLeftOnServer()
                        && account.isOnlineAndConnected()
                        && account.getXmppConnection().getFeatures().mam()) {
                    ir.bilgisoft.toopeto.services.MessageArchiveService.Query query = getMessageArchiveService().query(conversation, 0, timestamp - 1);
                    if (query != null) {
                        query.setCallback(callback);
                    }
                    callback.informUser(R.string.fetching_history_from_server);
                }
            }
        }).start();
    }

    public interface OnMoreMessagesLoaded {
        public void onMoreMessagesLoaded(int count, ir.bilgisoft.toopeto.entities.Conversation conversation);

        public void informUser(int r);
    }

    public List<ir.bilgisoft.toopeto.entities.Account> getAccounts() {
        return this.accounts;
    }

    public ir.bilgisoft.toopeto.entities.Conversation find(final Iterable<ir.bilgisoft.toopeto.entities.Conversation> haystack, final ir.bilgisoft.toopeto.entities.Contact contact) {
        for (final ir.bilgisoft.toopeto.entities.Conversation conversation : haystack) {
            if (conversation.getContact() == contact) {
                return conversation;
            }
        }
        return null;
    }

    public Conversation find(final Iterable<Conversation> haystack, final Account account, final String jid) {
        if (jid == null) {
            return null;
        }
        for (final Conversation conversation : haystack) {
            if ((account == null || conversation.getAccount() == account)
                    && (conversation.getJid().toBareJid().equals(jid))) {
                return conversation;
            }
        }
        return null;
    }

    public ir.bilgisoft.toopeto.entities.Conversation findOrCreateConversation(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.jid.Jid jid, final boolean muc) {
        return this.findOrCreateConversation(account, jid, muc, null);
    }

    public ir.bilgisoft.toopeto.entities.Conversation findOrCreateConversation(final Account account, final Jid jid, final boolean muc,
                                                                               final MessageArchiveService.Query query) {
        synchronized (this.conversations) {
            ir.bilgisoft.toopeto.entities.Conversation conversation = find(account, jid.getLocalpart());
            if (conversation != null) {
                return conversation;
            }
            conversation = databaseBackend.findConversation(account, jid);
            if (conversation != null) {
                conversation.setStatus(ir.bilgisoft.toopeto.entities.Conversation.STATUS_AVAILABLE);
                conversation.setAccount(account);
                if (muc) {
                    conversation.setMode(ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI);
                } else {
                    conversation.setMode(ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE);
                }
                conversation.addAll(0, databaseBackend.getMessages(conversation, ir.bilgisoft.toopeto.Config.PAGE_SIZE));
                this.databaseBackend.updateConversation(conversation);
            } else {
                String conversationName;
                ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(jid.getLocalpart());
                if (contact != null) {
                    conversationName = contact.getDisplayName();
                } else {
                    conversationName = jid.getLocalpart();
                }
                if (muc) {
                    conversation = new ir.bilgisoft.toopeto.entities.Conversation(conversationName, account, jid,
                            ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI);
                } else {
                    conversation = new ir.bilgisoft.toopeto.entities.Conversation(conversationName, account, jid,
                            ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE);
                }
                this.databaseBackend.createConversation(conversation);
            }
            if (account.getXmppConnection() != null && account.getXmppConnection().getFeatures().mam()) {
                if (query == null) {
                    this.mMessageArchiveService.query(conversation);
                } else {
                    if (query.getConversation() == null) {
                        //this.mMessageArchiveService.query(conversation, query.getStart());
                        this.mMessageArchiveService.query(conversation, 10);
                    }
                }
            }
            this.conversations.add(conversation);
            updateConversationUi();
            return conversation;
        }
    }

    public void archiveConversation(ir.bilgisoft.toopeto.entities.Conversation conversation) {
        synchronized (this.conversations) {
            if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
                if (conversation.getAccount().getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
                    ir.bilgisoft.toopeto.entities.Bookmark bookmark = conversation.getBookmark();
                    if (bookmark != null && bookmark.autojoin()) {
                        bookmark.setAutojoin(false);
                        pushBookmarks(bookmark.getAccount());
                    }
                }
                leaveMuc(conversation);
            } else {
                conversation.endOtrIfNeeded();
            }
            this.databaseBackend.updateConversation(conversation);
            this.conversations.remove(conversation);
            updateConversationUi();
        }
    }

    //scenario 3 : ejad va vasl shodan be account
    public void createAccount(final ir.bilgisoft.toopeto.entities.Account account) {
        account.initOtrEngine(this);
        databaseBackend.createAccount(account);
        this.accounts.add(account);
        this.reconnectAccount(account, false);
        updateAccountUi();
    }

    public void updateAccount(final ir.bilgisoft.toopeto.entities.Account account) {
        this.statusListener.onStatusChanged(account);
        databaseBackend.updateAccount(account);
        reconnectAccount(account, false);
        updateAccountUi();
        getNotificationService().updateErrorNotification();
    }

    public void updateAccountPasswordOnServer(final ir.bilgisoft.toopeto.entities.Account account, final String newPassword, final OnAccountPasswordChanged callback) {
        /*tagyer pass
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = getIqGenerator().generateSetPassword(account, newPassword);
		sendIqPacket(account, iq, new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
				if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
					account.setPassword(newPassword);
					databaseBackend.updateAccount(account);
					callback.onPasswordChangeSucceeded();
				} else {
					callback.onPasswordChangeFailed();
				}
			}
		});
        */
    }

    public interface OnAccountPasswordChanged {
        public void onPasswordChangeSucceeded();

        public void onPasswordChangeFailed();
    }

    public void deleteAccount(final ir.bilgisoft.toopeto.entities.Account account) {
        synchronized (this.conversations) {
            for (final ir.bilgisoft.toopeto.entities.Conversation conversation : conversations) {
                if (conversation.getAccount() == account) {
                    if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
                        leaveMuc(conversation);
                    } else if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE) {
                        conversation.endOtrIfNeeded();
                    }
                    conversations.remove(conversation);
                }
            }
            if (account.getXmppConnection() != null) {
                this.disconnect(account, true);
            }
            databaseBackend.deleteAccount(account);
            this.accounts.remove(account);
            updateAccountUi();
            getNotificationService().updateErrorNotification();
        }
    }

    public void setOnConversationListChangedListener(OnConversationUpdate listener) {
        synchronized (this) {
            if (checkListeners()) {
                switchToForeground();
            }
            this.mOnConversationUpdate = listener;
            this.mNotificationService.setIsInForeground(true);
            if (this.convChangedListenerCount < 2) {
                this.convChangedListenerCount++;
            }
        }
    }

    public void removeOnConversationListChangedListener() {
        synchronized (this) {
            this.convChangedListenerCount--;
            if (this.convChangedListenerCount <= 0) {
                this.convChangedListenerCount = 0;
                this.mOnConversationUpdate = null;
                this.mNotificationService.setIsInForeground(false);
                if (checkListeners()) {
                    switchToBackground();
                }
            }
        }
    }

    public void setOnAccountListChangedListener(OnAccountUpdate listener) {
        synchronized (this) {
            if (checkListeners()) {
                switchToForeground();
            }
            this.mOnAccountUpdate = listener;
            if (this.accountChangedListenerCount < 2) {
                this.accountChangedListenerCount++;
            }
        }
    }

    public void removeOnAccountListChangedListener() {
        synchronized (this) {
            this.accountChangedListenerCount--;
            if (this.accountChangedListenerCount <= 0) {
                this.mOnAccountUpdate = null;
                this.accountChangedListenerCount = 0;
                if (checkListeners()) {
                    switchToBackground();
                }
            }
        }
    }

    public void setOnRosterUpdateListener(final OnRosterUpdate listener) {
        synchronized (this) {
            if (checkListeners()) {
                switchToForeground();
            }
            this.mOnRosterUpdate = listener;
            if (this.rosterChangedListenerCount < 2) {
                this.rosterChangedListenerCount++;
            }
        }
    }

    public void removeOnRosterUpdateListener() {
        synchronized (this) {
            this.rosterChangedListenerCount--;
            if (this.rosterChangedListenerCount <= 0) {
                this.rosterChangedListenerCount = 0;
                this.mOnRosterUpdate = null;
                if (checkListeners()) {
                    switchToBackground();
                }
            }
        }
    }

    public void setOnUpdateBlocklistListener(final ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist listener) {
        synchronized (this) {
            if (checkListeners()) {
                switchToForeground();
            }
            this.mOnUpdateBlocklist = listener;
            if (this.updateBlocklistListenerCount < 2) {
                this.updateBlocklistListenerCount++;
            }
        }
    }

    public void removeOnUpdateBlocklistListener() {
        synchronized (this) {
            this.updateBlocklistListenerCount--;
            if (this.updateBlocklistListenerCount <= 0) {
                this.updateBlocklistListenerCount = 0;
                this.mOnUpdateBlocklist = null;
                if (checkListeners()) {
                    switchToBackground();
                }
            }
        }
    }

    public void setOnMucRosterUpdateListener(OnMucRosterUpdate listener) {
        synchronized (this) {
            if (checkListeners()) {
                switchToForeground();
            }
            this.mOnMucRosterUpdate = listener;
            if (this.mucRosterChangedListenerCount < 2) {
                this.mucRosterChangedListenerCount++;
            }
        }
    }

    public void removeOnMucRosterUpdateListener() {
        synchronized (this) {
            this.mucRosterChangedListenerCount--;
            if (this.mucRosterChangedListenerCount <= 0) {
                this.mucRosterChangedListenerCount = 0;
                this.mOnMucRosterUpdate = null;
                if (checkListeners()) {
                    switchToBackground();
                }
            }
        }
    }

    private boolean checkListeners() {
        return (this.mOnAccountUpdate == null
                && this.mOnConversationUpdate == null
                && this.mOnRosterUpdate == null
                && this.mOnUpdateBlocklist == null);
    }

    private void switchToForeground() {
        for (ir.bilgisoft.toopeto.entities.Account account : getAccounts()) {
            if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
                ir.bilgisoft.toopeto.xmpp.XmppConnection connection = account.getXmppConnection();
                if (connection != null && connection.getFeatures().csi()) {
                    connection.sendActive();
                }
            }
        }
        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "app switched into foreground");
    }

    private void switchToBackground() {
        for (ir.bilgisoft.toopeto.entities.Account account : getAccounts()) {
            if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
                ir.bilgisoft.toopeto.xmpp.XmppConnection connection = account.getXmppConnection();
                if (connection != null && connection.getFeatures().csi()) {
                    connection.sendInactive();
                }
            }
        }
        this.mNotificationService.setIsInForeground(false);
        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "app switched into background");
    }

    private void connectMultiModeConversations(ir.bilgisoft.toopeto.entities.Account account) {
        List<ir.bilgisoft.toopeto.entities.Conversation> conversations = getConversations();
        for (ir.bilgisoft.toopeto.entities.Conversation conversation : conversations) {
            if ((conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI)
                    && (conversation.getAccount() == account)) {
                conversation.resetMucOptions();
                joinMuc(conversation);
            }
        }
    }


    public void providePasswordForMuc(ir.bilgisoft.toopeto.entities.Conversation conversation, String password) {
        if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
            conversation.getMucOptions().setPassword(password);
            if (conversation.getBookmark() != null) {
                conversation.getBookmark().setAutojoin(true);
                pushBookmarks(conversation.getAccount());
            }
            databaseBackend.updateConversation(conversation);
            joinMuc(conversation);
        }
    }

    //lazm nist
    public void renameInMuc(final ir.bilgisoft.toopeto.entities.Conversation conversation, final String nick, final ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.entities.Conversation> callback) {
	/*
		final ir.bilgisoft.toopeto.entities.MucOptions options = conversation.getMucOptions();
		final ir.bilgisoft.toopeto.xmpp.jid.Jid joinJid = options.createJoinJid(nick);
		if (options.online()) {
			ir.bilgisoft.toopeto.entities.Account account = conversation.getAccount();
			options.setOnRenameListener(new OnRenameListener() {

				@Override
				public void onSuccess() {
					conversation.setContactJid(joinJid);
					databaseBackend.updateConversation(conversation);
					ir.bilgisoft.toopeto.entities.Bookmark bookmark = conversation.getBookmark();
					if (bookmark != null) {
						bookmark.setNick(nick);
						pushBookmarks(bookmark.getAccount());
					}
					callback.success(conversation);
				}

				@Override
				public void onFailure() {
					callback.error(R.string.nick_in_use, conversation);
				}
			});

			UserPacket packet = new UserPacket();
            packet.type
			//packet.setTo(joinJid);
			//packet.setFrom(conversation.getAccount().getJid());

			String sig = account.getPgpSignature();
			if (sig != null) {
				packet.addChild("status").setContent("online");
				packet.addChild("x", "jabber:x:signed").setContent(sig);
			}
			sendPresencePacket(account, packet);
		} else {
			conversation.setContactJid(joinJid);
			databaseBackend.updateConversation(conversation);
			if (conversation.getAccount().getStatus() == Account.State.ONLINE) {
				Bookmark bookmark = conversation.getBookmark();
				if (bookmark != null) {
					bookmark.setNick(nick);
					pushBookmarks(bookmark.getAccount());
				}
				joinMuc(conversation);
			}
		}
        */
    }

    public void leaveMuc(Conversation conversation) {
        Account account = conversation.getAccount();
        account.pendingConferenceJoins.remove(conversation);
        account.pendingConferenceLeaves.remove(conversation);
        if (account.getStatus() == Account.State.ONLINE) {
            UserPacket packet = new UserPacket();
            packet.type = Enums.UserTypeEnum.Leave.toString();
            packet.receiver = Enums.ReceiverEnum.room.toString();
            //packet.setTo(conversation.getJid());
            //packet.setFrom(conversation.getAccount().getJid());
            //packet.setAttribute("type", "unavailable");
            sendPresencePacket(conversation.getAccount());
            conversation.getMucOptions().setOffline();
            conversation.deregisterWithBookmark();
            Log.d(Config.LOGTAG, conversation.getAccount().getJid().toBareJid()
                    + ": leaving muc " + conversation.getJid());
        } else {
            account.pendingConferenceLeaves.add(conversation);
        }
    }

    private String findConferenceServer(final Account account) {
        String server;
        if (account.getXmppConnection() != null) {
            server = account.getXmppConnection().getMucServer();
            if (server != null) {
                return server;
            }
        }
        for (Account other : getAccounts()) {
            if (other != account && other.getXmppConnection() != null) {
                server = other.getXmppConnection().getMucServer();
                if (server != null) {
                    return server;
                }
            }
        }
        return null;
    }

    public void createAdhocConference(final Account account, final Iterable<Jid> jids, final UiCallback<Conversation> callback) {
        Log.d(Config.LOGTAG, account.getJid().toBareJid().toString() + ": creating adhoc conference with " + jids.toString());
        if (account.getStatus() == Account.State.ONLINE) {
            try {
                String server = findConferenceServer(account);
                if (server == null) {
                    if (callback != null) {
                        callback.error(R.string.no_conference_server_found, null);
                    }
                    return;
                }
                String name = new BigInteger(75, getRNG()).toString(32);
                Jid jid = Jid.fromParts(name, server, null);
                final Conversation conversation = findOrCreateConversation(account, jid, true);
                joinMuc(conversation);
                Bundle options = new Bundle();
                options.putString("muc#roomconfig_persistentroom", "1");
                options.putString("muc#roomconfig_membersonly", "1");
                options.putString("muc#roomconfig_publicroom", "0");
                options.putString("muc#roomconfig_whois", "anyone");
                pushConferenceConfiguration(conversation, options, new OnConferenceOptionsPushed() {
                    @Override
                    public void onPushSucceeded() {
                        for (Jid invite : jids) {
                            invite(conversation, invite);
                        }
                        if (callback != null) {
                            callback.success(conversation);
                        }
                    }

                    @Override
                    public void onPushFailed() {
                        if (callback != null) {
                            callback.error(R.string.conference_creation_failed, conversation);
                        }
                    }
                });

            } catch (InvalidJidException e) {
                if (callback != null) {
                    callback.error(R.string.conference_creation_failed, null);
                }
            }
        } else {
            if (callback != null) {
                callback.error(R.string.not_connected_try_again, null);
            }
        }
    }

    public void pushConferenceConfiguration(final Conversation conversation, final Bundle options, final OnConferenceOptionsPushed callback) {
        /*
		IqPacket request = new IqPacket(IqPacket.TYPE.GET);
		request.setTo(conversation.getJid().toBareJid());
		request.query("http://jabber.org/protocol/muc#owner");
		sendIqPacket(conversation.getAccount(),request,new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() != IqPacket.TYPE.ERROR) {
					Data data = Data.parse(packet.query().findChild("x", "jabber:x:data"));
					for (Field field : data.getFields()) {
						if (options.containsKey(field.getName())) {
							field.setValue(options.getString(field.getName()));
						}
					}
					data.submit();
					ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket set = new IqPacket(IqPacket.TYPE.SET);
					set.setTo(conversation.getJid().toBareJid());
					set.query("http://jabber.org/protocol/muc#owner").addChild(data);
					sendIqPacket(account, set, new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
						@Override
						public void onIqPacketReceived(Account account, IqPacket packet) {
							if (packet.getType() == IqPacket.TYPE.RESULT) {
								if (callback != null) {
									callback.onPushSucceeded();
								}
							} else {
								if (callback != null) {
									callback.onPushFailed();
								}
							}
						}
					});
				} else {
					if (callback != null) {
						callback.onPushFailed();
					}
				}
			}
		});
        */
    }

    public void disconnect(Account account, boolean force) {
        if ((account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE)
                || (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.DISABLED)) {
            if (!force) {
                List<ir.bilgisoft.toopeto.entities.Conversation> conversations = getConversations();
                for (ir.bilgisoft.toopeto.entities.Conversation conversation : conversations) {
                    if (conversation.getAccount() == account) {
                        if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
                            leaveMuc(conversation);
                        } else {
                            if (conversation.endOtrIfNeeded()) {
                                Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
                                        + ": ended otr session with "
                                        + conversation.getJid());
                            }
                        }
                    }
                }
            }
            account.getXmppConnection().disconnect(force);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void updateMessage(ir.bilgisoft.toopeto.entities.Message message) {
        databaseBackend.updateMessage(message);
        updateConversationUi();
    }

    protected void syncDirtyContacts(ir.bilgisoft.toopeto.entities.Account account) {
        for (ir.bilgisoft.toopeto.entities.Contact contact : account.getRoster().getContacts()) {
            if (contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_PUSH)) {
                pushContactToServer(contact);
            }
            if (contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_DELETE)) {
                deleteContactOnServer(contact);
            }
        }
    }

    public void createContact(ir.bilgisoft.toopeto.entities.Contact contact) {
        SharedPreferences sharedPref = getPreferences();
        boolean autoGrant = sharedPref.getBoolean("grant_new_contacts", true);
        if (autoGrant) {
            contact.setOption(ir.bilgisoft.toopeto.entities.Contact.Options.PREEMPTIVE_GRANT);
            contact.setOption(ir.bilgisoft.toopeto.entities.Contact.Options.ASKING);
        }
        pushContactToServer(contact);
    }

    public void onOtrSessionEstablished(ir.bilgisoft.toopeto.entities.Conversation conversation) {
        final ir.bilgisoft.toopeto.entities.Account account = conversation.getAccount();
        final Session otrSession = conversation.getOtrSession();
        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
                account.getJid().toBareJid() + " otr session established with "
                        + conversation.getJid() + "/"
                        + otrSession.getSessionID().getUserID());
        conversation.findUnsentMessagesWithOtrEncryption(new ir.bilgisoft.toopeto.entities.Conversation.OnMessageFound() {

            @Override
            public void onMessageFound(ir.bilgisoft.toopeto.entities.Message message) {
                SessionID id = otrSession.getSessionID();
                try {
                    message.setCounterpart(ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(id.getAccountID() + "/" + id.getUserID()));
                } catch (ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
                    return;
                }
                if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_TEXT) {
                    MessagePacket outPacket = mMessageGenerator.generateOtrChat(message, true);
                    if (outPacket != null) {
                        message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_SEND);
                        databaseBackend.updateMessage(message);
                        sendMessagePacket(account, outPacket);
                    }
                } else if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
                    mJingleConnectionManager.createNewConnection(message);
                }
                updateConversationUi();
            }
        });
    }

    public boolean renewSymmetricKey(ir.bilgisoft.toopeto.entities.Conversation conversation) {
        Log.d("error :", "lazm niisit dar scenario bashad");
        return false;
        /*
		ir.bilgisoft.toopeto.entities.Account account = conversation.getAccount();
		byte[] symmetricKey = new byte[32];
		this.mRandom.nextBytes(symmetricKey);
		Session otrSession = conversation.getOtrSession();
		if (otrSession != null) {
			MessagePacket packet = new MessagePacket();
		//	packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_CHAT);
		//	packet.setFrom(account.getJid());
			//packet.addChild("private", "urn:xmpp:carbons:2");
			//packet.addChild("no-copy", "urn:xmpp:hints");
			//packet.setAttribute("to", otrSession.getSessionID().getAccountID() + "/"
					//+ otrSession.getSessionID().getUserID());
			try {
				packet.setBody(otrSession
						.transformSending(ir.bilgisoft.toopeto.utils.CryptoHelper.FILETRANSFER
							+ ir.bilgisoft.toopeto.utils.CryptoHelper.bytesToHex(symmetricKey)));
				sendMessagePacket(account, packet);
				conversation.setSymmetricKey(symmetricKey);
				return true;
			} catch (OtrException e) {
				return false;
			}
		}
		return false;
        */
    }

    public void pushContactToServer(final ir.bilgisoft.toopeto.entities.Contact contact) {
        contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_DELETE);
        contact.setOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_PUSH);
        final ir.bilgisoft.toopeto.entities.Account account = contact.getAccount();
        if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
            final boolean ask = contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.ASKING);
            final boolean sendUpdates = contact
                    .getOption(ir.bilgisoft.toopeto.entities.Contact.Options.PENDING_SUBSCRIPTION_REQUEST)
                    && contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.PREEMPTIVE_GRANT);
            final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
            iq.query(ir.bilgisoft.toopeto.utils.Xmlns.ROSTER).addChild(contact.asElement());
            //account.getXmppConnection().sendIqPacket(iq, null);
            if (sendUpdates) {
                sendPresencePacket(account);
            }
            if (ask) {
                sendPresencePacket(account);
            }
        }
    }

    public void publishAvatar(final ir.bilgisoft.toopeto.entities.Account account,
                              final Uri image,
                              final ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.xmpp.pep.Avatar> callback) {
        /*
		final Bitmap.CompressFormat format = ir.bilgisoft.toopeto.Config.AVATAR_FORMAT;
		final int size = ir.bilgisoft.toopeto.Config.AVATAR_SIZE;
		final ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar = getFileBackend()
			.getPepAvatar(image, size, format);
		if (avatar != null) {
			avatar.height = size;
			avatar.width = size;
			if (format.equals(Bitmap.CompressFormat.WEBP)) {
				avatar.type = "image/webp";
			} else if (format.equals(Bitmap.CompressFormat.JPEG)) {
				avatar.type = "image/jpeg";
			} else if (format.equals(Bitmap.CompressFormat.PNG)) {
				avatar.type = "image/png";
			}
			if (!getFileBackend().save(avatar)) {
				callback.error(R.string.error_saving_avatar, avatar);
				return;
			}
			final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = this.mIqGenerator.publishAvatar(avatar);
            packet.setTo(account.getServer());
          //  packet.setAttribute("to",account.getJid().getDomainpart());
			this.sendIqPacket(account, packet, new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {

				@Override
				public void onIqPacketReceived(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket result) {
					if (result.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
						final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = XmppConnectionService.this.mIqGenerator
							.publishAvatarMetadata(avatar);
                        packet.setTo(account.getServer());
						sendIqPacket(account, packet, new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {

							@Override
							public void onIqPacketReceived(ir.bilgisoft.toopeto.entities.Account account,
									ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket result) {
								if (result.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
									if (account.setAvatar(avatar.getFilename())) {
										databaseBackend.updateAccount(account);
									}
									callback.success(avatar);
								} else {
									callback.error(
											R.string.error_publish_avatar_server_reject,
											avatar);
								}
							}
						});
					} else {
						callback.error(
								R.string.error_publish_avatar_server_reject,
								avatar);
					}
				}
			});
		} else {
			callback.error(R.string.error_publish_avatar_converting, null);
		}
        */
    }

    public void fetchAvatar(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar) {
        fetchAvatar(account, avatar, null);
    }

    public void fetchAvatar(ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar,
                            final ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.xmpp.pep.Avatar> callback) {
        /*
		ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = this.mIqGenerator.retrieveAvatar(avatar);
		sendIqPacket(account, packet, new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {

			@Override
			public void onIqPacketReceived(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket result) {
				final String ERROR = account.getJid().toBareJid()
					+ ": fetching avatar for " + avatar.owner + " failed ";
				if (result.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
					avatar.image = mIqParser.avatarData(result);
					if (avatar.image != null) {
						if (getFileBackend().save(avatar)) {
							if (account.getJid().toBareJid().equals(avatar.owner)) {
								if (account.setAvatar(avatar.getFilename())) {
									databaseBackend.updateAccount(account);
								}
								getAvatarService().clear(account);
								updateConversationUi();
								updateAccountUi();
							} else {
								ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster()
									.getContact(avatar.owner);
								contact.setAvatar(avatar.getFilename());
								getAvatarService().clear(contact);
								updateConversationUi();
								updateRosterUi();
							}
							if (callback != null) {
								callback.success(avatar);
							}
							Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
									+ ": succesfully fetched avatar for "
									+ avatar.owner);
							return;
						}
					} else {

						Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, ERROR + "(parsing error)");
					}
				} else {
					ir.bilgisoft.toopeto.xml.Element error = result.findChild("error");
					if (error == null) {
						Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, ERROR + "(server error)");
					} else {
						Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, ERROR + error.toString());
					}
				}
				if (callback != null) {
					callback.error(0, null);
				}

			}
		});
        */
    }

    public void checkForAvatar(ir.bilgisoft.toopeto.entities.Account account,
                               final ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.xmpp.pep.Avatar> callback) {
        /*
		ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = this.mIqGenerator.retrieveAvatarMetaData(null);
		this.sendIqPacket(account, packet, new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {

			@Override
			public void onIqPacketReceived(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
				if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
					ir.bilgisoft.toopeto.xml.Element pubsub = packet.findChild("pubsub",
							"http://jabber.org/protocol/pubsub");
					if (pubsub != null) {
						ir.bilgisoft.toopeto.xml.Element items = pubsub.findChild("items");
						if (items != null) {
							ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar = ir.bilgisoft.toopeto.xmpp.pep.Avatar.parseMetadata(items);
							if (avatar != null) {
								avatar.owner = account.getJid().toBareJid();
								if (fileBackend.isAvatarCached(avatar)) {
									if (account.setAvatar(avatar.getFilename())) {
										databaseBackend.updateAccount(account);
									}
									getAvatarService().clear(account);
									callback.success(avatar);
								} else {
									fetchAvatar(account, avatar, callback);
								}
								return;
							}
						}
					}
				}
				callback.error(0, null);
			}
		});
        */
    }

    public void deleteContactOnServer(ir.bilgisoft.toopeto.entities.Contact contact) {
        contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.PREEMPTIVE_GRANT);
        contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_PUSH);
        contact.setOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_DELETE);
        ir.bilgisoft.toopeto.entities.Account account = contact.getAccount();
        if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
            ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
            ir.bilgisoft.toopeto.xml.Element item = iq.query(ir.bilgisoft.toopeto.utils.Xmlns.ROSTER).addChild("item");
            item.setAttribute("jid", contact.getJid().toString());
            item.setAttribute("subscription", "remove");
            //	account.getXmppConnection().sendIqPacket(iq, null);
        }
    }

    public void updateConversation(ir.bilgisoft.toopeto.entities.Conversation conversation) {
        this.databaseBackend.updateConversation(conversation);
    }

    //start barname va send o receive az in ghesmat shoru mishvad
    //scenario 1
    public void reconnectAccount(final ir.bilgisoft.toopeto.entities.Account account, final boolean force) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("reconnect", "reconnect fire");
                //agar account connection dasht barye disconnect ersal migardad
                if (account.getXmppConnection() != null) {
                    disconnect(account, force);
                }
                //agar account disable bood  dobareh connetion ejad migardad
                if (!account.isOptionSet(ir.bilgisoft.toopeto.entities.Account.OPTION_DISABLED)) {
                    if (account.getXmppConnection() == null) {
                        account.setXmppConnection(createConnection(account));
                    }
                    //shoru send recive barnameh
                    Thread thread = new Thread(account.getXmppConnection());
                    thread.start();
                    scheduleWakeUpCall(ir.bilgisoft.toopeto.Config.CONNECT_TIMEOUT, account.getUuid().hashCode());
                } else {
                    account.getRoster().clearPresences();
                    account.setXmppConnection(null);
                }
            }
        }).start();
    }

    public void invite(ir.bilgisoft.toopeto.entities.Conversation conversation, ir.bilgisoft.toopeto.xmpp.jid.Jid contact) {
        MessagePacket packet = mMessageGenerator.invite(conversation, contact);
        sendMessagePacket(conversation.getAccount(), packet);
    }

    public void resetSendingToWaiting(ir.bilgisoft.toopeto.entities.Account account) {
        for (ir.bilgisoft.toopeto.entities.Conversation conversation : getConversations()) {
            if (conversation.getAccount() == account) {
                conversation.findUnsentTextMessages(new ir.bilgisoft.toopeto.entities.Conversation.OnMessageFound() {

                    @Override
                    public void onMessageFound(ir.bilgisoft.toopeto.entities.Message message) {
                        markMessage(message, ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING);
                    }
                });
            }
        }
    }

    public boolean markMessage(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.jid.Jid recipient, final String uuid,
                               final int status) {
        if (uuid == null) {
            return false;
        } else {
            for (ir.bilgisoft.toopeto.entities.Conversation conversation : getConversations()) {
                if (conversation.getJid().equals(recipient)
                        && conversation.getAccount().equals(account)) {
                    return markMessage(conversation, uuid, status);
                }
            }
            return false;
        }
    }

    public boolean markMessage(ir.bilgisoft.toopeto.entities.Conversation conversation, String uuid,
                               int status) {
        if (uuid == null) {
            return false;
        } else {
            ir.bilgisoft.toopeto.entities.Message message = conversation.findSentMessageWithUuid(uuid);
            if (message != null) {
                markMessage(message, status);
                return true;
            } else {
                return false;
            }
        }
    }

    public void markMessage(ir.bilgisoft.toopeto.entities.Message message, int status) {
        if (status == ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_FAILED
                && (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_RECEIVED || message
                .getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_DISPLAYED)) {
            return;
        }
        message.setStatus(status);
        databaseBackend.updateMessage(message);
        updateConversationUi();
    }

    public SharedPreferences getPreferences() {
        return PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
    }

    public boolean forceEncryption() {
        return getPreferences().getBoolean("force_encryption", false);
    }

    public boolean confirmMessages() {
        return getPreferences().getBoolean("confirm_messages", true);
    }

    public boolean saveEncryptedMessages() {
        return !getPreferences().getBoolean("dont_save_encrypted", false);
    }

    public boolean indicateReceived() {
        return getPreferences().getBoolean("indicate_received", false);
    }

    public void updateConversationUi() {
        if (mOnConversationUpdate != null) {
            mOnConversationUpdate.onConversationUpdate();
        }
    }

    public void updateAccountUi() {
        if (mOnAccountUpdate != null) {
            mOnAccountUpdate.onAccountUpdate();
        }
    }

    public void updateRosterUi() {
        if (mOnRosterUpdate != null) {
            mOnRosterUpdate.onRosterUpdate();
        }
    }

    public void updateBlocklistUi(final ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist.Status status) {
        if (mOnUpdateBlocklist != null) {
            mOnUpdateBlocklist.OnUpdateBlocklist(status);
        }
    }

    public void updateMucRosterUi() {
        if (mOnMucRosterUpdate != null) {
            mOnMucRosterUpdate.onMucRosterUpdate();
        }
    }

    public ir.bilgisoft.toopeto.entities.Account findAccountByJid(final ir.bilgisoft.toopeto.xmpp.jid.Jid accountJid) {
        for (ir.bilgisoft.toopeto.entities.Account account : this.accounts) {
            if (account.getJid().toBareJid().equals(accountJid.toBareJid())) {
                return account;
            }
        }
        return null;
    }

    public ir.bilgisoft.toopeto.entities.Conversation findConversationByUuid(String uuid) {
        for (ir.bilgisoft.toopeto.entities.Conversation conversation : getConversations()) {
            if (conversation.getUuid().equals(uuid)) {
                return conversation;
            }
        }
        return null;
    }

    public void markRead(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
        mNotificationService.clear(conversation);
        conversation.markRead();
    }

    public void sendReadMarker(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
        final ir.bilgisoft.toopeto.entities.Message markable = conversation.getLatestMarkableMessage();
        this.markRead(conversation);
        if (confirmMessages() && markable != null && markable.getRemoteMsgId() != null) {
            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, conversation.getAccount().getJid().toBareJid() + ": sending read marker to " + markable.getCounterpart().toString());
            Account account = conversation.getAccount();
            final ir.bilgisoft.toopeto.xmpp.jid.Jid to = markable.getCounterpart();
            MessagePacket packet = mMessageGenerator.confirm(account, to, markable.getRemoteMsgId());
            this.sendMessagePacket(conversation.getAccount(), packet);
        }
        updateConversationUi();
    }

    public SecureRandom getRNG() {
        return this.mRandom;
    }

    public MemorizingTrustManager getMemorizingTrustManager() {
        return this.mMemorizingTrustManager;
    }

    public PowerManager getPowerManager() {
        return this.pm;
    }

    public LruCache<String, Bitmap> getBitmapCache() {
        return this.mBitmapCache;
    }

    public void syncRosterToDisk(final ir.bilgisoft.toopeto.entities.Account account) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                databaseBackend.writeRoster(account.getRoster());
            }
        }).start();

    }

    public List<String> getKnownHosts() {
        final List<String> hosts = new ArrayList<>();
        for (final ir.bilgisoft.toopeto.entities.Account account : getAccounts()) {
            if (!hosts.contains(account.getServer().toString())) {
                hosts.add(account.getServer().toString());
            }
            for (final ir.bilgisoft.toopeto.entities.Contact contact : account.getRoster().getContacts()) {
                if (contact.showInRoster()) {
                    final String server = contact.getServer().toString();
                    if (server != null && !hosts.contains(server)) {
                        hosts.add(server);
                    }
                }
            }
        }
        return hosts;
    }

    //list serverhaye mucServer ra bar mighrdand ==>> bayad bishtar baraci shavad
    public List<String> getKnownConferenceHosts() {
        final ArrayList<String> mucServers = new ArrayList<>();
        for (final ir.bilgisoft.toopeto.entities.Account account : accounts) {
            if (account.getXmppConnection() != null) {
                final String server = account.getXmppConnection().getMucServer();
                if (server != null && !mucServers.contains(server)) {
                    mucServers.add(server);
                }
            }
        }
        return mucServers;
    }

    public void sendMessagePacket(ir.bilgisoft.toopeto.entities.Account account, MessagePacket packet) {
        ir.bilgisoft.toopeto.xmpp.XmppConnection connection = account.getXmppConnection();
        if (connection != null) {
            //connection.sendMessagePacket(packet);
        }
    }

    public void sendPresencePacket(ir.bilgisoft.toopeto.entities.Account account) {
        ir.bilgisoft.toopeto.xmpp.XmppConnection connection = account.getXmppConnection();
        if (connection != null) {
            connection.sendPresencePacket( mPresenceGenerator.sendPresence(account));
        }
    }

    public void sendIqPacket(final Account account, final Json packet, final ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived callback) {
        final ir.bilgisoft.toopeto.xmpp.XmppConnection connection = account.getXmppConnection();
        if (connection != null) {
            connection.sendIqPacket(packet, callback);
        }
    }

    public ir.bilgisoft.toopeto.generator.MessageGenerator getMessageGenerator() {
        return this.mMessageGenerator;
    }

    public ir.bilgisoft.toopeto.generator.PresenceGenerator getPresenceGenerator() {
        return this.mPresenceGenerator;
    }

    public ir.bilgisoft.toopeto.generator.IqGenerator getIqGenerator() {
        return this.mIqGenerator;
    }

    public ir.bilgisoft.toopeto.parser.IqParser getIqParser() {
        return this.mIqParser;
    }

    public ir.bilgisoft.toopeto.xmpp.jingle.JingleConnectionManager getJingleConnectionManager() {
        return this.mJingleConnectionManager;
    }

    public ir.bilgisoft.toopeto.services.MessageArchiveService getMessageArchiveService() {
        return this.mMessageArchiveService;
    }

    public List<ir.bilgisoft.toopeto.entities.Contact> findContacts(ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
        ArrayList<ir.bilgisoft.toopeto.entities.Contact> contacts = new ArrayList<>();
        for (ir.bilgisoft.toopeto.entities.Account account : getAccounts()) {
            if (!account.isOptionSet(ir.bilgisoft.toopeto.entities.Account.OPTION_DISABLED)) {
                ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContactFromRoster(jid);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
        }
        return contacts;
    }

    public ir.bilgisoft.toopeto.services.NotificationService getNotificationService() {
        return this.mNotificationService;
    }

    public ir.bilgisoft.toopeto.http.HttpConnectionManager getHttpConnectionManager() {
        return this.mHttpConnectionManager;
    }

    public void resendFailedMessages(final ir.bilgisoft.toopeto.entities.Message message) {
        final Collection<ir.bilgisoft.toopeto.entities.Message> messages = new ArrayList<>();
        ir.bilgisoft.toopeto.entities.Message current = message;
        while (current.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_FAILED) {
            messages.add(current);
            if (current.mergeable(current.next())) {
                current = current.next();
            } else {
                break;
            }
        }
        for (final ir.bilgisoft.toopeto.entities.Message msg : messages) {
            markMessage(msg, ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING);
            this.resendMessage(msg);
        }
    }

    public void clearConversationHistory(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
        conversation.clearMessages();
        conversation.setHasMessagesLeftOnServer(false); //avoid messages getting loaded through mam
        new Thread(new Runnable() {
            @Override
            public void run() {
                databaseBackend.deleteMessagesInConversation(conversation);
            }
        }).start();
    }

    public interface OnConversationUpdate {
        public void onConversationUpdate();
    }

    public interface OnAccountUpdate {
        public void onAccountUpdate();
    }

    public interface OnRosterUpdate {
        public void onRosterUpdate();
    }

    public interface OnMucRosterUpdate {
        public void onMucRosterUpdate();
    }

    private interface OnConferenceOptionsPushed {
        public void onPushSucceeded();

        public void onPushFailed();
    }

    public class XmppConnectionBinder extends Binder {
        public XmppConnectionService getService() {
            return XmppConnectionService.this;
        }
    }

    public void sendBlockRequest(final ir.bilgisoft.toopeto.entities.Blockable blockable) {
        /*
		if (blockable != null && blockable.getBlockedJid() != null) {
			final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = blockable.getBlockedJid();
			this.sendIqPacket(blockable.getAccount(), getIqGenerator().generateSetBlockRequest(jid), new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {

				@Override
				public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
					if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
						account.getBlocklist().add(jid);
						updateBlocklistUi(ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist.Status.BLOCKED);
					}
				}
			});
		}
        */
    }

    public void sendUnblockRequest(final ir.bilgisoft.toopeto.entities.Blockable blockable) {
        /*
		if (blockable != null && blockable.getJid() != null) {
			final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = blockable.getBlockedJid();
			this.sendIqPacket(blockable.getAccount(), getIqGenerator().generateSetUnblockRequest(jid), new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
				@Override
				public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
					if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
						account.getBlocklist().remove(jid);
						updateBlocklistUi(ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist.Status.UNBLOCKED);
					}
				}
			});
		}
        */
    }
}
