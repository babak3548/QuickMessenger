package ir.bilgisoft.toopeto.xmpp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import ir.bilgisoft.toopeto.Config;
import ir.bilgisoft.toopeto.crypto.sasl.SaslMechanism;
import ir.bilgisoft.toopeto.entities.Account;

import ir.bilgisoft.toopeto.entities.Message;
import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.Json;
import ir.bilgisoft.toopeto.json.MessagePacket;

import ir.bilgisoft.toopeto.json.UserPacket;
import ir.bilgisoft.toopeto.services.XmppConnectionService;
import ir.bilgisoft.toopeto.utils.DNSHelper;
import ir.bilgisoft.toopeto.xml.Element;
import ir.bilgisoft.toopeto.xml.Tag;
import ir.bilgisoft.toopeto.xml.TagWriter;
import ir.bilgisoft.toopeto.xml.XmlReader;
import ir.bilgisoft.toopeto.xmpp.jingle.OnJinglePacketReceived;
import ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket;

public class XmppConnection implements Runnable {

    private static final int PACKET_IQ = 0;
    private static final int PACKET_MESSAGE = 1;
    private static final int PACKET_PRESENCE = 2;
    private final Context applicationContext;
    protected Account account;
    private final WakeLock wakeLock;
    private Socket socket;
    private XmlReader tagReader;
    private TagWriter tagWriter;
    private final Features features = new Features(this);
    private boolean shouldBind = true;
    private boolean shouldAuthenticate = true;
    private Element streamFeatures;
    private final HashMap<String, List<String>> disco = new HashMap<>();

    private String streamId = null;
    private int smVersion = 3;
    private final SparseArray<String> messageReceipts = new SparseArray<>();

    private int stanzasReceived = 0;
    private int stanzasSent = 0;
    private long lastPacketReceived = 0;
    private long lastPingSent = 0;
    private long lastConnect = 0;
    private long lastSessionStarted = 0;
    private int attempt = 0;
    private final Map<String, Pair<IqPacket, OnIqPacketReceived>> packetCallbacks = new Hashtable<>();
    private final Map<String, Pair<Json, OnIqPacketReceived>> jsonCallbacks = new Hashtable<>();
    private OnPresencePacketReceived presenceListener = null;
    private OnJinglePacketReceived jingleListener = null;
    private OnIqPacketReceived unregisteredIqListener = null;
    private OnMessagePacketReceived messageListener = null;
    private OnStatusChanged statusListener = null;
    private OnBindListener bindListener = null;
    private final ArrayList<OnAdvancedStreamFeaturesLoaded> advancedStreamFeaturesLoadedListeners = new ArrayList<>();
    private OnMessageAcknowledged acknowledgedListener = null;
    private XmppConnectionService mXmppConnectionService = null;

    private SaslMechanism saslMechanism;

    public XmppConnection(final Account account, final XmppConnectionService service) {
        this.account = account;
        this.wakeLock = service.getPowerManager().newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, account.getJid().toBareJid().toString());
        tagWriter = new TagWriter();
        mXmppConnectionService = service;
        applicationContext = service.getApplicationContext();
    }

    protected void changeStatus(final Account.State nextStatus) {
        if (account.getStatus() != nextStatus) {
            if ((nextStatus == ir.bilgisoft.toopeto.entities.Account.State.OFFLINE)
                    && (account.getStatus() != ir.bilgisoft.toopeto.entities.Account.State.CONNECTING)
                    && (account.getStatus() != Account.State.ONLINE)
                    && (account.getStatus() != Account.State.DISABLED)) {
                return;
            }
            if (nextStatus == Account.State.ONLINE) {
                this.attempt = 0;
            }
            account.setStatus(nextStatus);
            if (statusListener != null) {
                statusListener.onStatusChanged(account);
            }
        }
    }

    protected void changeStatus(final String jsonString) {
        UserPacket userPacket = UserPacket.GetUserPacket(jsonString);
     if (userPacket.userName.equals(account.getJid().getLocalpart())) {
         if (userPacket.type.equals(Enums.UserTypeEnum.registerFailed.toString())) {
             changeStatus(Account.State.REGISTRATION_FAILED);
         } else if (userPacket.type.equals(Enums.UserTypeEnum.registerConflict.toString())) {
             changeStatus(Account.State.REGISTRATION_CONFLICT);
         } else if (userPacket.type.equals(Enums.UserTypeEnum.registerDone.toString())) {
             changeStatus(Account.State.ONLINE);
         } else if (userPacket.type.equals(Enums.UserTypeEnum.presence.toString())) {
             changeStatus(Account.State.ONLINE);
         }
     }
        else
     {
         Log.d("Error :","other user presence lost");
     }
    }

    //ghalbe proje in method mibashad va zaman start avleh yek bar dar therad shuro be ejra mishvad
    protected void connect() {
        Log.d(Config.LOGTAG, account.getJid().toBareJid().toString() + ": connecting");
        features.encryptionEnabled = false;
        lastConnect = SystemClock.elapsedRealtime();
        lastPingSent = SystemClock.elapsedRealtime();
        this.attempt++;
        try {
            shouldAuthenticate = shouldBind = !account.isOptionSet(Account.OPTION_REGISTER);
            tagReader = new XmlReader(wakeLock);
            tagWriter = new TagWriter();
            packetCallbacks.clear();
            this.changeStatus(Account.State.CONNECTING);
            final Bundle result = DNSHelper.getSRVRecord(account.getServer());
            final ArrayList<Parcelable> values = result.getParcelableArrayList("values");
            if ("timeout".equals(result.getString("error"))) {
                throw new IOException("timeout in dns");
            } else if (values != null) {
                int i = 0;
                boolean socketError = true;
                //be tedad  ya i, say mikonad be socket connect ra bargara konad
                while (socketError && values.size() > i) {
                    final Bundle namePort = (Bundle) values.get(i);
                    try {
                        String srvRecordServer;
                        try {
                            srvRecordServer = IDN.toASCII(namePort.getString("name"));
                        } catch (final IllegalArgumentException e) {
                            // TODO: Handle me?`
                            srvRecordServer = "";
                        }
                        final int srvRecordPort = namePort.getInt("port");
                        final String srvIpServer = namePort.getString("ip");
                        final InetSocketAddress addr;
                        if (srvIpServer != null) {
                            addr = new InetSocketAddress(srvIpServer, srvRecordPort);
                            Log.d(Config.LOGTAG, account.getJid().toBareJid().toString()
                                    + ": using values from dns " + srvRecordServer
                                    + "[" + srvIpServer + "]:" + srvRecordPort);
                        } else {
                            addr = new InetSocketAddress(srvRecordServer, srvRecordPort);
                            Log.d(Config.LOGTAG, account.getJid().toBareJid().toString()
                                    + ": using values from dns "
                                    + srvRecordServer + ":" + srvRecordPort);
                        }
                        socket = new Socket();
                        socket.connect(addr, 20000);
                        socketError = false;
                    } catch (final UnknownHostException e) {
                        Log.d(Config.LOGTAG, account.getJid().toBareJid().toString() + ": " + e.getMessage());
                        i++;
                    } catch (final IOException e) {
                        Log.d(Config.LOGTAG, account.getJid().toBareJid().toString() + ": " + e.getMessage());
                        i++;
                    }
                }//end of while connect socket
                if (socketError) {
                    throw new UnknownHostException();
                }
            } else if (result.containsKey("error") && "nosrv".equals(result.getString("error", null))) {
                //socket = new Socket(account.getServer().getDomainpart(), 5222);
                socket = new Socket(account.getServer().getDomainpart(), 3001);
            } else {
                throw new IOException("timeout in dns");
            }
            //osolan agar connection bargarar bood bayad byad beresad be in ghesmat
            final OutputStream out = socket.getOutputStream();
            tagWriter.setOutputStream(out);
            final InputStream in = socket.getInputStream();
            tagReader.setInputStream(in);
            tagWriter.beginDocument();
            // ersal ra shoru mikonad ke felan ghre fall hast
            sendStartStream();
            //bind avlie ra shorou mikoneh vali badan bardashte mishavad
            bindListener.onBind(account);
            String nextTag;
            // khandan yek tag resideh
            //	while ((nextTag = tagReader.readTag()) != null) {
            while (true) {
                nextTag = tagReader.readTag();
                if (nextTag != null) {
                    processStream(nextTag);
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (final UnknownHostException | ConnectException e) {
            Log.d(Config.LOGTAG, account.getJid().toBareJid().toString() + ": error in run UnknownHostException" + e.getMessage());
            this.changeStatus(Account.State.SERVER_NOT_FOUND);
        } catch (final IOException | XmlPullParserException | NoSuchAlgorithmException e) {
            Log.d(Config.LOGTAG, account.getJid().toBareJid().toString() + ": error in run IOException" + e.getMessage());
            this.changeStatus(Account.State.OFFLINE);
        } catch (final Exception e) {
            Log.d(Config.LOGTAG, account.getJid().toBareJid().toString() + ": error in run Others" + e.getMessage());
            this.changeStatus(Account.State.OFFLINE);
        } finally {
            Log.d("reader thread:", "finished");
            if (wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (final RuntimeException ignored) {
                }
            }
        }
    }

    @Override
    public void run() {
        connect();
    }

    //tag jari br hasbe start an, be parser mord hadf tahvil midahad
    private void processStream(final String currentTag) throws XmlPullParserException,
            IOException, NoSuchAlgorithmException {
        //	Tag nextTag = tagReader.readTag();
            /*
    *  ) {
        changeStatus(Account.State.REGISTRATION_FAILED);
    } else if (nextTag.packetName.equals(Enums.PacketNameEnum.user.toString())
            && ) {
        changeStatus(Account.State.REGISTRATION_CONFLICT);
    } else if (nextTag.packetName.equals(Enums.PacketNameEnum.user.toString())
            && ) {
        changeStatus(Account.State.ONLINE);
    } else if (nextTag.packetName.equals(Enums.PacketNameEnum.user.toString())
            && */
        Json currentJson = Json.GetJson(currentTag);
        Json nextTag = currentJson;
        //	while ((nextTag != null) && (!nextTag.isEnd("stream"))) {
        if (nextTag.type.equals(Enums.MessageTypeEnum.ack.toString())) {
            //felan kari nemikoneh
        } else if (nextTag.packetName.equals(Enums.PacketNameEnum.message.toString())
                && nextTag.type.equals(Enums.MessageTypeEnum.error.toString())) {
            processStreamError(nextTag);

        } else if (nextTag.packetName.equals(Enums.PacketNameEnum.user.toString()) &&
                (nextTag.type.equals(Enums.UserTypeEnum.registerFailed.toString())
                        || nextTag.type.equals(Enums.UserTypeEnum.registerConflict.toString())
                        || nextTag.type.equals(Enums.UserTypeEnum.registerDone.toString())
                        || nextTag.type.equals(Enums.UserTypeEnum.presence.toString()))
                ) {
            changeStatus(currentTag);
            //farg nemikonhe packet name chi basheh faghat type un result bashe
        } else if (nextTag.type.equals(Enums.MessageTypeEnum.repeatBind.toString())) {
            sendBindRequest();
        } else if (nextTag.packetName.equals(Enums.PacketNameEnum.listTransfer.toString())
                || nextTag.type.equals(Enums.MessageTypeEnum.result.toString())) {
            processIq(nextTag, currentTag);
        } else if (nextTag.packetName.equals(Enums.PacketNameEnum.message.toString())) {
            //tage grefte shode braye pars shodan ersal migardad
            processMessage(currentTag);
        } else if (nextTag.packetName.equals(Enums.PacketNameEnum.user.toString())) {
            processPresence(currentTag);
        } else {
            Log.d("error critic", "packet receive is lost ");
        }
        if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
            account.setStatus(ir.bilgisoft.toopeto.entities.Account.State.OFFLINE);
            if (statusListener != null) {
                statusListener.onStatusChanged(account);
            }
        }

    }

    //elan hozor beserver
    private void sendInitialPing() {
        MessagePacket ping = new MessagePacket();
        ping.type = Enums.MessageTypeEnum.ping.toString();
        ping.to = Enums.Setting.fromServerToopeto;
        ping.from = account.getUsername();
        ping.id = account.getUuid();
        this.sendUnmodifiedIqPacket(ping, new OnIqPacketReceived() {
            @Override
            public void onIqPacketReceived(Account account, String jsonString) {
                Log.d(Config.LOGTAG, account.getJid().toBareJid().toString()
                        + ": online with resource " + account.getResource());
                changeStatus(ir.bilgisoft.toopeto.entities.Account.State.ONLINE);
            }
        });
    }

    private ir.bilgisoft.toopeto.xml.Element processPacket(final ir.bilgisoft.toopeto.xml.Tag currentTag, final int packetType)
            throws XmlPullParserException, IOException {
        return null;
    }

    private void processIq(Json json, String jsonString) throws XmlPullParserException, IOException {
        //final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = (ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket) processPacket(currentTag, PACKET_IQ);
        if (json.id == null || json.id == "") {
            Log.d("error", "error");
            return; // an iq packet without id is definitely invalid
        } else {
            if (jsonCallbacks.containsKey(json.id)) {
                final Pair<Json, OnIqPacketReceived> packetCallbackDuple = jsonCallbacks.get(json.id);
                packetCallbackDuple.second.onIqPacketReceived(account, jsonString);
            } else// if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET|| packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET)
            {
                Log.d(Config.LOGTAG, "unregistered Iq packet message receive ");
                this.unregisteredIqListener.onIqPacketReceived(account, jsonString);
            }
        }
    }

    private void processMessage(final String jsonString) throws XmlPullParserException, IOException {
        //final ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet =
        //  (ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket) processPacket(currentTag,PACKET_MESSAGE);
        MessagePacket messagePacket = MessagePacket.GetMessagePacket(jsonString);
        this.messageListener.onMessagePacketReceived(account, messagePacket);
    }

    private void processPresence(String jsonString) throws XmlPullParserException, IOException {
        //	ir.bilgisoft.toopeto.xmpp.stanzas.PresencePacket packet = (ir.bilgisoft.toopeto.xmpp.stanzas.PresencePacket) processPacket(currentTag, PACKET_PRESENCE);
        UserPacket userPacket = UserPacket.GetUserPacket(jsonString);
        this.presenceListener.onPresencePacketReceived(account, userPacket);
    }

    private void sendStartTLS() throws IOException {
		/*final ir.bilgisoft.toopeto.xml.Tag startTLS = ir.bilgisoft.toopeto.xml.Tag.empty("starttls");
		startTLS.setAttribute("xmlns", "urn:ietf:params:xml:ns:xmpp-tls");
		tagWriter.writeTag(startTLS); */
    }

    private SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    private boolean enableLegacySSL() {
        return getPreferences().getBoolean("enable_legacy_ssl", false);
    }

    private void switchOverToTls(final ir.bilgisoft.toopeto.xml.Tag currentTag) throws XmlPullParserException, IOException {
        tagReader.readTag();
        try {
            final SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new X509TrustManager[]{this.mXmppConnectionService.getMemorizingTrustManager()}, mXmppConnectionService.getRNG());
            final SSLSocketFactory factory = sc.getSocketFactory();
            final HostnameVerifier verifier = this.mXmppConnectionService.getMemorizingTrustManager().wrapHostnameVerifier(new StrictHostnameVerifier());
            final InetAddress address = socket == null ? null : socket.getInetAddress();

            if (factory == null || address == null || verifier == null) {
                throw new IOException("could not setup ssl");
            }

            final SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, address.getHostAddress(), socket.getPort(), true);

            if (sslSocket == null) {
                throw new IOException("could not initialize ssl socket");
            }

            final String[] supportProtocols;
            if (enableLegacySSL()) {
                supportProtocols = sslSocket.getSupportedProtocols();
            } else {
                final Collection<String> supportedProtocols = new LinkedList<>(
                        Arrays.asList(sslSocket.getSupportedProtocols()));
                supportedProtocols.remove("SSLv3");
                supportProtocols = new String[supportedProtocols.size()];
                supportedProtocols.toArray(supportProtocols);
            }
            sslSocket.setEnabledProtocols(supportProtocols);

            if (!verifier.verify(account.getServer().getDomainpart(), sslSocket.getSession())) {
                Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + ": TLS certificate verification failed");
                disconnect(true);
                changeStatus(ir.bilgisoft.toopeto.entities.Account.State.SECURITY_ERROR);
            }
            tagReader.setInputStream(sslSocket.getInputStream());
            tagWriter.setOutputStream(sslSocket.getOutputStream());
            sendStartStream();
            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + ": TLS connection established");
            features.encryptionEnabled = true;
            processStream(tagReader.readTag());
            sslSocket.close();
        } catch (final NoSuchAlgorithmException | KeyManagementException e1) {
            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + ": TLS certificate verification failed");
            disconnect(true);
            changeStatus(ir.bilgisoft.toopeto.entities.Account.State.SECURITY_ERROR);
        }
    }

    private void processStreamFeatures(final ir.bilgisoft.toopeto.xml.Tag currentTag)
            throws XmlPullParserException, IOException {/*
        this.streamFeatures = tagReader.readElement(currentTag);
		if (this.streamFeatures.hasChild("starttls") && !features.encryptionEnabled) {
			sendStartTLS();
		} else if (this.streamFeatures.hasChild("register")
				&& account.isOptionSet(ir.bilgisoft.toopeto.entities.Account.OPTION_REGISTER)
				&& features.encryptionEnabled) {
			sendRegistryRequest();
		} else if (!this.streamFeatures.hasChild("register")
				&& account.isOptionSet(ir.bilgisoft.toopeto.entities.Account.OPTION_REGISTER)) {
			changeStatus(ir.bilgisoft.toopeto.entities.Account.State.REGISTRATION_NOT_SUPPORTED);
			disconnect(true);
		} else if (this.streamFeatures.hasChild("mechanisms")
				&& shouldAuthenticate && features.encryptionEnabled) {
			final List<String> mechanisms = extractMechanisms(streamFeatures
					.findChild("mechanisms"));
			final ir.bilgisoft.toopeto.xml.Element auth = new ir.bilgisoft.toopeto.xml.Element("auth");
			auth.setAttribute("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl");
			if (mechanisms.contains("SCRAM-SHA-1")) {
				saslMechanism = new ir.bilgisoft.toopeto.crypto.sasl.ScramSha1(tagWriter, account, mXmppConnectionService.getRNG());
			} else if (mechanisms.contains("PLAIN")) {
				saslMechanism = new ir.bilgisoft.toopeto.crypto.sasl.Plain(tagWriter, account);
			} else if (mechanisms.contains("DIGEST-MD5")) {
				saslMechanism = new ir.bilgisoft.toopeto.crypto.sasl.DigestMd5(tagWriter, account, mXmppConnectionService.getRNG());
			}
			final JSONObject keys = account.getKeys();
			try {
				if (keys.has(ir.bilgisoft.toopeto.entities.Account.PINNED_MECHANISM_KEY) &&
						keys.getInt(ir.bilgisoft.toopeto.entities.Account.PINNED_MECHANISM_KEY) > saslMechanism.getPriority() ) {
					Log.e(ir.bilgisoft.toopeto.Config.LOGTAG, "Auth failed. Authentication mechanism " + saslMechanism.getMechanism() +
							" has lower priority (" + String.valueOf(saslMechanism.getPriority()) +
							") than pinned priority (" + keys.getInt(ir.bilgisoft.toopeto.entities.Account.PINNED_MECHANISM_KEY) +
							"). Possible downgrade attack?");
					disconnect(true);
					changeStatus(ir.bilgisoft.toopeto.entities.Account.State.SECURITY_ERROR);
						}
			} catch (final JSONException e) {
				Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "Parse error while checking pinned auth mechanism");
			}
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,account.getJid().toString()+": Authenticating with " + saslMechanism.getMechanism());
			auth.setAttribute("mechanism", saslMechanism.getMechanism());
			if (!saslMechanism.getClientFirstMessage().isEmpty()) {
				auth.setContent(saslMechanism.getClientFirstMessage());
			}
			tagWriter.writeElement(auth);
		} else if (this.streamFeatures.hasChild("sm", "urn:xmpp:sm:"
					+ smVersion)
				&& streamId != null) {
			final ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.ResumePacket resume = new ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.ResumePacket(this.streamId,
					stanzasReceived, smVersion);
			this.tagWriter.writeStanzaAsync(resume);
		} else if (this.streamFeatures.hasChild("bind") && shouldBind) {
			sendBindRequest();
		} else {
			disconnect(true);
			changeStatus(ir.bilgisoft.toopeto.entities.Account.State.INCOMPATIBLE_SERVER);
		}*/
    }

    private List<String> extractMechanisms(final ir.bilgisoft.toopeto.xml.Element stream) {
        final ArrayList<String> mechanisms = new ArrayList<>(stream
                .getChildren().size());
        for (final ir.bilgisoft.toopeto.xml.Element child : stream.getChildren()) {
            mechanisms.add(child.getContent());
        }
        return mechanisms;
    }

    private void sendRegistryRequest() {
        final UserPacket userPacket = new UserPacket();
        sendUnmodifiedIqPacket(userPacket, new OnIqPacketReceived() {
            @Override
            public void onIqPacketReceived(Account account, String jsonPacket) {
                UserPacket userPacketReceived = UserPacket.GetUserPacket(jsonPacket);
                Account.State state;
                if (userPacketReceived.type == Enums.UserTypeEnum.registerDone.toString()) {
                    state = Account.State.REGISTRATION_SUCCESSFUL;
                } else {
                    state = Account.State.REGISTRATION_FAILED;
                }

                changeStatus(state);
                if (userPacketReceived.type == Enums.UserTypeEnum.registerConflict.toString()) {
                    disconnect(true);
                    Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
                                    + ": could not userPacket. instructions are"
                    );
                } else if (userPacketReceived.type == Enums.UserTypeEnum.registerFailed.toString()) {
                    Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "register Failed"
                    );
                }
            }
        });
    }

    //baraye bind avlieh mnasb mibashad
    private void sendBindRequest() {

        final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new IqPacket(IqPacket.TYPE.SET);
        //iq.addChild("bind", "urn:ietf:params:xml:ns:xmpp-bind")
        //	.addChild("resource").setContent(account.getResource());
        MessagePacket bing = new MessagePacket();
        bing.type = Enums.MessageTypeEnum.ping.toString();

        this.sendUnmodifiedIqPacket(bing, new OnIqPacketReceived() {
            @Override
            public void onIqPacketReceived(final Account account, final String packet) {
                try {
                    //  final ir.bilgisoft.toopeto.xml.Element bind = packet.findChild("bind");
                    Json json = Json.GetJson(packet);
                    if (json != null) {
                        /*
                        final ir.bilgisoft.toopeto.xml.Element jid = bind.findChild("jid");

                        if (jid != null && jid.getContent() != null) {
                            try {
                                account.setResource(ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(jid.getContent()).getResourcepart());
                            } catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
                                // TODO: Handle the case where an external JID is technically invalid?
                            }
                            if (streamFeatures.hasChild("sm", "urn:xmpp:sm:3")) {
                                smVersion = 3;
                                final ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.EnablePacket enable = new ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.EnablePacket(smVersion);
                                tagWriter.writeStanzaAsync(enable);
                                stanzasSent = 0;
                                messageReceipts.clear();
                            } else if (streamFeatures.hasChild("sm", "urn:xmpp:sm:2")) {
                                smVersion = 2;
                                final ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.EnablePacket enable = new ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.EnablePacket(smVersion);
                                tagWriter.writeStanzaAsync(enable);
                                stanzasSent = 0;
                                messageReceipts.clear();
                            }
                            features.carbonsEnabled = false;
                            features.blockListRequested = false;
                            disco.clear();
                            sendServiceDiscoveryInfo(account.getServer());
                            sendServiceDiscoveryItems(account.getServer());
                            */
                        if (bindListener != null) {
                            bindListener.onBind(account);
                        }
                        sendInitialPing();
                        //} else {
                        //  disconnect(true);
                        // }
                    } else {
                        disconnect(true);
                    }
                } catch (Exception e) {
                    Log.d("eee: ", "eee");
                }

            }
        });
        /*
		if (this.streamFeatures.hasChild("session")) {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + ": sending deprecated session");
			final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket startSession = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
			startSession.addChild("session","urn:ietf:params:xml:ns:xmpp-session");
			this.sendUnmodifiedIqPacket(startSession, null);
		}*/
    }

    /*
        private void sendServiceDiscoveryInfo(final ir.bilgisoft.toopeto.xmpp.jid.Jid server) {
            if (disco.containsKey(server.toDomainJid().toString())) {
                if (account.getServer().equals(server.toDomainJid())) {
                    enableAdvancedStreamFeatures();
                }
            } else {
                final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
                iq.setTo(server.toDomainJid());
                iq.query("http://jabber.org/protocol/disco#info");
                this.sendUnmodifiedIqPacket(iq, new OnIqPacketReceived() {

                    @Override
                    public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
                        final List<ir.bilgisoft.toopeto.xml.Element> elements = packet.query().getChildren();
                        final List<String> features = new ArrayList<>();
                        for (final ir.bilgisoft.toopeto.xml.Element element : elements) {
                            if (element.getName().equals("identity")) {
                                if ("irc".equals(element.getAttribute("type"))) {
                                    //add fake feature to not confuse irc and real muc
                                    features.add("siacs:no:muc");
                                }
                            } else if (element.getName().equals("feature")) {
                                features.add(element.getAttribute("var"));
                            }
                        }
                        disco.put(server.toDomainJid().toString(), features);

                        if (account.getServer().equals(server.toDomainJid())) {
                            enableAdvancedStreamFeatures();
                            for (final ir.bilgisoft.toopeto.xmpp.OnAdvancedStreamFeaturesLoaded listener : advancedStreamFeaturesLoadedListeners) {
                                listener.onAdvancedStreamFeaturesAvailable(account);
                            }
                        }
                    }
                });
            }
        }

        private void enableAdvancedStreamFeatures() {
            if (getFeatures().carbons() && !features.carbonsEnabled) {
                sendEnableCarbons();
            }
            if (getFeatures().blocking() && !features.blockListRequested) {
                Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "Requesting block list");
                this.sendIqPacket(getIqGenerator().generateGetBlockList(), mXmppConnectionService.getIqParser());
            }
        } */
/*
	private void sendServiceDiscoveryItems(final ir.bilgisoft.toopeto.xmpp.jid.Jid server) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
		iq.setTo(server.toDomainJid());
		iq.query("http://jabber.org/protocol/disco#items");
		this.sendIqPacket(iq, new OnIqPacketReceived() {

			@Override
			public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
				final List<ir.bilgisoft.toopeto.xml.Element> elements = packet.query().getChildren();
				for (final ir.bilgisoft.toopeto.xml.Element element : elements) {
					if (element.getName().equals("item")) {
						final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = element.getAttributeAsJid("jid");
						if (jid != null && !jid.equals(account.getServer())) {
							sendServiceDiscoveryInfo(jid);
						}
					}
				}
			}
		});
	}
    */
    /*
	private void sendEnableCarbons() {

		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
		iq.addChild("enable", "urn:xmpp:carbons:2");
		this.sendIqPacket(iq, new OnIqPacketReceived() {

			@Override
			public void onIqPacketReceived(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
				if (!packet.hasChild("error")) {
					Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
							+ ": successfully enabled carbons");
					features.carbonsEnabled = true;
				} else {
					Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid()
							+ ": error enableing carbons " + packet.toString());
				}
			}
		});
	}
    */
    private void processStreamError(final Json currentTag)
            throws XmlPullParserException, IOException {

        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
                account.getJid().toBareJid() + ":recive error switching resource due to conflict ("
                        + account.getResource() + ")");
        /*
		final ir.bilgisoft.toopeto.xml.Element streamError = tagReader.readElement(currentTag);
		if (streamError != null && streamError.hasChild("conflict")) {
			final String resource = account.getResource().split("\\.")[0];
			account.setResource(resource + "." + nextRandomId());
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
					account.getJid().toBareJid() + ": switching resource due to conflict ("
					+ account.getResource() + ")");
		}*/

    }

    //ersal stream ra shoru mikonad
    private void sendStartStream() throws IOException {
        MessagePacket messagePacket = new MessagePacket();
        //  messagePacket.type= MessageTypeEnum.ping.to
        /*
		final ir.bilgisoft.toopeto.xml.Tag stream = ir.bilgisoft.toopeto.xml.Tag.start("stream:stream");
		stream.setAttribute("from", account.getJid().toBareJid().toString());
		stream.setAttribute("to", account.getServer().toString());
		stream.setAttribute("version", "1.0");
		stream.setAttribute("xml:lang", "en");
		stream.setAttribute("xmlns", "jabber:client");
		stream.setAttribute("xmlns:stream", "http://etherx.jabber.org/streams");
		tagWriter.writeTag(stream);*/
    }

    private String nextRandomId() {
        return new BigInteger(50, mXmppConnectionService.getRNG()).toString(32);
    }

    /**/
    public void sendIqPacket(final Json packet, final OnIqPacketReceived callback) {
        //   packet.setFrom(account.getJid());
        this.sendUnmodifiedIqPacket(packet, callback);
    }

    /*
        private synchronized void sendUnmodifiedIqPacket(final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet, final OnIqPacketReceived callback) {
            if (packet.getId() == null) {
                final String id = nextRandomId();
                packet.setAttribute("id", id);
            }
            if (callback != null) {
                if (packet.getId() == null) {
                    packet.setId(nextRandomId());
                }
                packetCallbacks.put(packet.getId(), new Pair<>(packet, callback));
            }
            this.sendPacket(packet);
        }*/
    private synchronized void sendUnmodifiedIqPacket(final Json json, final OnIqPacketReceived callback) {

        if (callback != null) {
            jsonCallbacks.put(json.id, new Pair<>(json, callback));
        }
        this.sendPacket(json);
    }

    public void sendMessagePacket(final Json message) {
        this.sendPacket(message);
    }

    public void sendPresencePacket(final UserPacket presence) {
        this.sendPacket(presence);
    }

    //ersale pakege
    private synchronized void sendPacket(final Json packet) {
        //final String name = packet.getName();
        //if (name.equals("iq") || name.equals("message") || name.equals("presence")) {
        ++stanzasSent;
        //}
        tagWriter.writeStanzaAsync(packet);

        if (packet instanceof Json && packet.id != null && this.streamId != null) {
            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "request delivery report for stanza " + stanzasSent);
            this.messageReceipts.put(stanzasSent, packet.id);
            //tagWriter.writeStanzaAsync(new ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.RequestPacket(this.smVersion));
        }
    }

    public void sendPing() {
        /*
		if (streamFeatures.hasChild("sm")) {
			tagWriter.writeStanzaAsync(new ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.RequestPacket(smVersion));
		} else {
			final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
			iq.setFrom(account.getJid());
			iq.addChild("ping", "urn:xmpp:ping");
			this.sendIqPacket(iq, null);
		}*/
        //  Ping ping= new Ping();
        MessagePacket ping = new MessagePacket();
        ping.type = Enums.MessageTypeEnum.ping.toString();

        this.sendPacket(ping);
        this.lastPingSent = SystemClock.elapsedRealtime();
    }

    public void setOnMessagePacketReceivedListener(
            final OnMessagePacketReceived listener) {
        this.messageListener = listener;
    }

    public void setOnUnregisteredIqPacketReceivedListener(
            final OnIqPacketReceived listener) {
        this.unregisteredIqListener = listener;
    }

    public void setOnPresencePacketReceivedListener(
            final OnPresencePacketReceived listener) {
        this.presenceListener = listener;
    }

    public void setOnJinglePacketReceivedListener(
            final ir.bilgisoft.toopeto.xmpp.jingle.OnJinglePacketReceived listener) {
        this.jingleListener = listener;
    }

    public void setOnStatusChangedListener(final ir.bilgisoft.toopeto.xmpp.OnStatusChanged listener) {
        this.statusListener = listener;
    }

    public void setOnBindListener(final OnBindListener listener) {
        this.bindListener = listener;
    }

    public void setOnMessageAcknowledgeListener(final ir.bilgisoft.toopeto.xmpp.OnMessageAcknowledged listener) {
        this.acknowledgedListener = listener;
    }

    public void addOnAdvancedStreamFeaturesAvailableListener(final ir.bilgisoft.toopeto.xmpp.OnAdvancedStreamFeaturesLoaded listener) {
        if (!this.advancedStreamFeaturesLoadedListeners.contains(listener)) {
            this.advancedStreamFeaturesLoadedListeners.add(listener);
        }
    }

    public void disconnect(final boolean force) {
        Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid() + ": disconnecting");
        try {
            if (force) {
                socket.close();
                return;
            }
            new Thread(new Runnable() {

                @Override
                public void run() {
                    if (tagWriter.isActive()) {
                        tagWriter.finish();
                        try {
                            while (!tagWriter.finished()) {
                                Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "not yet finished");
                                Thread.sleep(100);
                            }
                            MessagePacket endPacket = new MessagePacket();
                            endPacket.type = Enums.MessageTypeEnum.end.toString();
                            tagWriter.writeTag(endPacket);
                            socket.close();
                        } catch (final IOException e) {
                            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
                                    "io exception during disconnect");
                        } catch (final InterruptedException e) {
                            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "interrupted");
                        }
                    }
                }
            }).start();
        } catch (final IOException e) {
            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "io exception during disconnect");
        }
    }

    public List<String> findDiscoItemsByFeature(final String feature) {
        final List<String> items = new ArrayList<>();
        for (final Entry<String, List<String>> cursor : disco.entrySet()) {
            if (cursor.getValue().contains(feature)) {
                items.add(cursor.getKey());
            }
        }
        return items;
    }

    public String findDiscoItemByFeature(final String feature) {
        final List<String> items = findDiscoItemsByFeature(feature);
        if (items.size() >= 1) {
            return items.get(0);
        }
        return null;
    }

    /*
        public void r() {
            this.tagWriter.writeStanzaAsync(new ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt.RequestPacket(smVersion));
        }
        */
    public String getMucServer() {
        for (final Entry<String, List<String>> cursor : disco.entrySet()) {
            final List<String> value = cursor.getValue();
            if (value.contains("http://jabber.org/protocol/muc") && !value.contains("jabber:iq:gateway") && !value.contains("siacs:no:muc")) {
                return cursor.getKey();
            }
        }
        return null;
    }

    public int getTimeToNextAttempt() {
        final int interval = (int) (25 * Math.pow(1.5, attempt));
        final int secondsSinceLast = (int) ((SystemClock.elapsedRealtime() - this.lastConnect) / 1000);
        return interval - secondsSinceLast;
    }

    public int getAttempt() {
        return this.attempt;
    }

    public Features getFeatures() {
        return this.features;
    }

    public long getLastSessionEstablished() {
        final long diff;
        if (this.lastSessionStarted == 0) {
            diff = SystemClock.elapsedRealtime() - this.lastConnect;
        } else {
            diff = SystemClock.elapsedRealtime() - this.lastSessionStarted;
        }
        return System.currentTimeMillis() - diff;
    }

    public long getLastConnect() {
        return this.lastConnect;
    }

    public long getLastPingSent() {
        return this.lastPingSent;
    }

    public long getLastPacketReceived() {
        return this.lastPacketReceived;
    }

    public void sendActive() {
        MessagePacket activePacket = new MessagePacket();
        activePacket.type = Enums.MessageTypeEnum.ping.toString();
        this.sendPacket(activePacket);
    }

    public void sendInactive() {
        //	this.sendPacket(new ir.bilgisoft.toopeto.xmpp.stanzas.csi.InactivePacket());
        MessagePacket activePacket = new MessagePacket();
        activePacket.type = Enums.MessageTypeEnum.ping.toString();
        this.sendPacket(activePacket);
    }

    public class Features {
        XmppConnection connection;
        private boolean carbonsEnabled = false;
        private boolean encryptionEnabled = false;
        private boolean blockListRequested = false;

        public Features(final XmppConnection connection) {
            this.connection = connection;
        }

        //check mikonad ke chanin vegi ro support mikone ya na
        private boolean hasDiscoFeature(final ir.bilgisoft.toopeto.xmpp.jid.Jid server, final String feature) {
            return connection.disco.containsKey(server.toDomainJid().toString()) &&
                    connection.disco.get(server.toDomainJid().toString()).contains(feature);
        }

        public boolean carbons() {
            return hasDiscoFeature(account.getServer(), "urn:xmpp:carbons:2");
        }

        public boolean blocking() {
            return hasDiscoFeature(account.getServer(), ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING);
        }

        public boolean register() {
            return hasDiscoFeature(account.getServer(), ir.bilgisoft.toopeto.utils.Xmlns.REGISTER);
        }

        public boolean sm() {
            return streamId != null;
        }

        public boolean csi() {
            return connection.streamFeatures != null && connection.streamFeatures.hasChild("csi", "urn:xmpp:csi:0");
        }

        public boolean pubsub() {
            return hasDiscoFeature(account.getServer(),
                    "http://jabber.org/protocol/pubsub#publish");
        }

        public boolean mam() {
            return hasDiscoFeature(account.getServer(), "urn:xmpp:mam:0");
        }

        public boolean advancedStreamFeaturesLoaded() {
            return disco.containsKey(account.getServer().toString());
        }

        public boolean rosterVersioning() {
            return connection.streamFeatures != null && connection.streamFeatures.hasChild("ver");
        }

        public void setBlockListRequested(boolean value) {
            this.blockListRequested = value;
        }
    }

    private ir.bilgisoft.toopeto.generator.IqGenerator getIqGenerator() {
        return mXmppConnectionService.getIqGenerator();
    }
}
