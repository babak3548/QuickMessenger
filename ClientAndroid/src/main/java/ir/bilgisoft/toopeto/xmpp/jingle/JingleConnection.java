package ir.bilgisoft.toopeto.xmpp.jingle;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import ir.bilgisoft.toopeto.Config;
import ir.bilgisoft.toopeto.entities.Account;
import ir.bilgisoft.toopeto.entities.Downloadable;
import ir.bilgisoft.toopeto.entities.DownloadablePlaceholder;
import ir.bilgisoft.toopeto.entities.Message;
import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.Json;
import ir.bilgisoft.toopeto.json.MessagePacket;
import ir.bilgisoft.toopeto.xmpp.jid.Jid;
import ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content;
import ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket;
import ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Reason;
import ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket;

public class JingleConnection implements ir.bilgisoft.toopeto.entities.Downloadable {

	private JingleConnectionManager mJingleConnectionManager;
	private ir.bilgisoft.toopeto.services.XmppConnectionService mXmppConnectionService;

	protected static final int JINGLE_STATUS_INITIATED = 0;
	protected static final int JINGLE_STATUS_ACCEPTED = 1;
	protected static final int JINGLE_STATUS_TERMINATED = 2;
	protected static final int JINGLE_STATUS_CANCELED = 3;
	protected static final int JINGLE_STATUS_FINISHED = 4;
	protected static final int JINGLE_STATUS_TRANSMITTING = 5;
	protected static final int JINGLE_STATUS_FAILED = 99;

	private int ibbBlockSize = 4096;

	private int mJingleStatus = -1;
	private int mStatus = ir.bilgisoft.toopeto.entities.Downloadable.STATUS_UNKNOWN;
	private ir.bilgisoft.toopeto.entities.Message message;
	private String sessionId;
	private ir.bilgisoft.toopeto.entities.Account account;
	private ir.bilgisoft.toopeto.xmpp.jid.Jid initiator;
	private ir.bilgisoft.toopeto.xmpp.jid.Jid responder;
	private List<ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate> candidates = new ArrayList<>();
	private ConcurrentHashMap<String, ir.bilgisoft.toopeto.xmpp.jingle.JingleSocks5Transport> connections = new ConcurrentHashMap<>();

	private String transportId;
	private ir.bilgisoft.toopeto.xml.Element fileOffer;
	private ir.bilgisoft.toopeto.entities.DownloadableFile file = null;

	private String contentName;
	private String contentCreator;

	private int mProgress = 0;
	private long mLastGuiRefresh = 0;

	private boolean receivedCandidate = false;
	private boolean sentCandidate = false;

	private boolean acceptedAutomatically = false;

	private ir.bilgisoft.toopeto.xmpp.jingle.JingleTransport transport = null;

	private ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived responseListener = new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
		@Override
		public void onIqPacketReceived(ir.bilgisoft.toopeto.entities.Account account, String packet) {
            MessagePacket messagePacket=   MessagePacket.GetMessagePacket(packet);
			if (messagePacket.type == Enums.MessageTypeEnum.error.toString()) {
				fail();
			}
		}
	};

	final ir.bilgisoft.toopeto.xmpp.jingle.OnFileTransmissionStatusChanged onFileTransmissionSatusChanged = new ir.bilgisoft.toopeto.xmpp.jingle.OnFileTransmissionStatusChanged() {

		@Override
		public void onFileTransmitted(ir.bilgisoft.toopeto.entities.DownloadableFile file) {
			if (responder.equals(account.getJid())) {
				sendSuccess();
				if (acceptedAutomatically) {
					message.markUnread();
					JingleConnection.this.mXmppConnectionService
							.getNotificationService().push(message);
				}
				mXmppConnectionService.getFileBackend().updateFileParams(message);
				mXmppConnectionService.databaseBackend.createMessage(message);
				mXmppConnectionService.markMessage(message,
						ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED);
			} else {
				if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP || message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED) {
					file.delete();
				}
			}
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
					"sucessfully transmitted file:" + file.getAbsolutePath());
			if (message.getEncryption() != ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
				Intent intent = new Intent(
						Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				intent.setData(Uri.fromFile(file));
				mXmppConnectionService.sendBroadcast(intent);
			}
		}

		@Override
		public void onFileTransferAborted() {
			JingleConnection.this.sendCancel();
			JingleConnection.this.fail();
		}
	};

	private OnProxyActivated onProxyActivated = new OnProxyActivated() {

		@Override
		public void success() {
			if (initiator.equals(account.getJid())) {
				Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "we were initiating. sending file");
				transport.send(file, onFileTransmissionSatusChanged);
			} else {
				transport.receive(file, onFileTransmissionSatusChanged);
				Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "we were responding. receiving file");
			}
		}

		@Override
		public void failed() {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "proxy activation failed");
		}
	};

	public JingleConnection(JingleConnectionManager mJingleConnectionManager) {
		this.mJingleConnectionManager = mJingleConnectionManager;
		this.mXmppConnectionService = mJingleConnectionManager
				.getXmppConnectionService();
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public ir.bilgisoft.toopeto.entities.Account getAccount() {
		return this.account;
	}

	public ir.bilgisoft.toopeto.xmpp.jid.Jid getCounterPart() {
		return this.message.getCounterpart();
	}

	public void deliverPacket(ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet) {
		boolean returnResult = true;
		if (packet.isAction("session-terminate")) {
			ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Reason reason = packet.getReason();
			if (reason != null) {
				if (reason.hasChild("cancel")) {
					this.fail();
				} else if (reason.hasChild("success")) {
					this.receiveSuccess();
				} else {
					this.fail();
				}
			} else {
				this.fail();
			}
		} else if (packet.isAction("session-accept")) {
			returnResult = receiveAccept(packet);
		} else if (packet.isAction("transport-info")) {
			returnResult = receiveTransportInfo(packet);
		} else if (packet.isAction("transport-replace")) {
			if (packet.getJingleContent().hasIbbTransport()) {
				returnResult = this.receiveFallbackToIbb(packet);
			} else {
				returnResult = false;
				Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "trying to fallback to something unknown"
						+ packet.toString());
			}
		} else if (packet.isAction("transport-accept")) {
			returnResult = this.receiveTransportAccept(packet);
		} else {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "packet arrived in connection. action was "
					+ packet.getAction());
			returnResult = false;
		}
		ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket response;
		if (returnResult) {
			response = packet.generateResponse(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT);

		} else {
			response = packet.generateResponse(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.ERROR);
		}
		//account.getXmppConnection().sendIqPacket(response, null);
	}

	public void init(ir.bilgisoft.toopeto.entities.Message message) {
		this.contentCreator = "initiator";
		this.contentName = this.mJingleConnectionManager.nextRandomId();
		this.message = message;
		this.message.setDownloadable(this);
		this.mStatus = ir.bilgisoft.toopeto.entities.Downloadable.STATUS_UPLOADING;
		this.account = message.getConversation().getAccount();
		this.initiator = this.account.getJid();
		this.responder = this.message.getCounterpart();
		this.sessionId = this.mJingleConnectionManager.nextRandomId();
		if (this.candidates.size() > 0) {
			this.sendInitRequest();
		} else {
			this.mJingleConnectionManager.getPrimaryCandidate(account,
					new ir.bilgisoft.toopeto.xmpp.jingle.OnPrimaryCandidateFound() {

						@Override
						public void onPrimaryCandidateFound(boolean success,
								final ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate candidate) {
							if (success) {
								final ir.bilgisoft.toopeto.xmpp.jingle.JingleSocks5Transport socksConnection = new ir.bilgisoft.toopeto.xmpp.jingle.JingleSocks5Transport(
										JingleConnection.this, candidate);
								connections.put(candidate.getCid(),
										socksConnection);
								socksConnection
										.connect(new ir.bilgisoft.toopeto.xmpp.jingle.OnTransportConnected() {

											@Override
											public void failed() {
												Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
														"connection to our own primary candidete failed");
												sendInitRequest();
											}

											@Override
											public void established() {
												Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
														"succesfully connected to our own primary candidate");
												mergeCandidate(candidate);
												sendInitRequest();
											}
										});
								mergeCandidate(candidate);
							} else {
								Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
										"no primary candidate of our own was found");
								sendInitRequest();
							}
						}
					});
		}

	}

	public void init(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet) {
        /*
		this.mJingleStatus = JINGLE_STATUS_INITIATED;
		ir.bilgisoft.toopeto.entities.Conversation conversation = this.mXmppConnectionService
				.findOrCreateConversation(account,
						packet.getFrom().toBareJid(), false);
		this.message = new ir.bilgisoft.toopeto.entities.Message(conversation, "", ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE);
		this.message.setStatus(ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED);
		this.mStatus = ir.bilgisoft.toopeto.entities.Downloadable.STATUS_OFFER;
		this.message.setDownloadable(this);
        final ir.bilgisoft.toopeto.xmpp.jid.Jid from = packet.getFrom();
		this.message.setCounterpart(from);
		this.account = account;
		this.initiator = packet.getFrom();
		this.responder = this.account.getJid();
		this.sessionId = packet.getSessionId();
		ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content content = packet.getJingleContent();
		this.contentCreator = content.getAttribute("creator");
		this.contentName = content.getAttribute("name");
		this.transportId = content.getTransportId();
		this.mergeCandidates(ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate.parse(content.socks5transport()
                .getChildren()));
		this.fileOffer = packet.getJingleContent().getFileOffer();
		if (fileOffer != null) {
			ir.bilgisoft.toopeto.xml.Element fileSize = fileOffer.findChild("size");
			ir.bilgisoft.toopeto.xml.Element fileNameElement = fileOffer.findChild("name");
			if (fileNameElement != null) {
				String[] filename = fileNameElement.getContent()
						.toLowerCase(Locale.US).toLowerCase().split("\\.");
				if (Arrays.asList(VALID_IMAGE_EXTENSIONS).contains(
						filename[filename.length - 1])) {
					message.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE);
				} else if (Arrays.asList(VALID_CRYPTO_EXTENSIONS).contains(
						filename[filename.length - 1])) {
					if (filename.length == 3) {
						if (Arrays.asList(VALID_IMAGE_EXTENSIONS).contains(
								filename[filename.length - 2])) {
							message.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE);
						} else {
							message.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_FILE);
						}
						if (filename[filename.length - 1].equals("otr")) {
							message.setEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR);
						} else {
							message.setEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP);
						}
					}
				} else {
					message.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_FILE);
				}
				if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
					String suffix = "";
					if (!fileNameElement.getContent().isEmpty()) {
						String parts[] = fileNameElement.getContent().split("/");
						suffix = parts[parts.length - 1];
						if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR  && suffix.endsWith(".otr")) {
							suffix = suffix.substring(0,suffix.length() - 4);
						} else if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP && (suffix.endsWith(".pgp") || suffix.endsWith(".gpg"))) {
							suffix = suffix.substring(0,suffix.length() - 4);
						}
					}
					message.setRelativeFilePath(message.getUuid()+"_"+suffix);
				}
				long size = Long.parseLong(fileSize.getContent());
				message.setBody(Long.toString(size));
				conversation.add(message);
				mXmppConnectionService.updateConversationUi();
				if (size <= this.mJingleConnectionManager
						.getAutoAcceptFileSize()) {
					Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "auto accepting file from "
							+ packet.getFrom());
					this.acceptedAutomatically = true;
					this.sendAccept();
				} else {
					message.markUnread();
					Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
							"not auto accepting new file offer with size: "
									+ size
									+ " allowed size:"
									+ this.mJingleConnectionManager
											.getAutoAcceptFileSize());
					this.mXmppConnectionService.getNotificationService()
							.push(message);
				}
				this.file = this.mXmppConnectionService.getFileBackend()
						.getFile(message, false);
				if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR) {
					byte[] key = conversation.getSymmetricKey();
					if (key == null) {
						this.sendCancel();
						this.fail();
						return;
					} else {
						this.file.setKey(key);
					}
				}
				this.file.setExpectedSize(size);
			} else {
				this.sendCancel();
				this.fail();
			}
		} else {
			this.sendCancel();
			this.fail();
		}
        */
	}

	private void sendInitRequest() {
		this.mXmppConnectionService.markMessage(this.message, ir.bilgisoft.toopeto.entities.Message.STATUS_OFFERED);
		ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet = this.bootstrapPacket("session-initiate");
		ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content content = new ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content(this.contentCreator, this.contentName);
		if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
			content.setTransportId(this.transportId);
			this.file = this.mXmppConnectionService.getFileBackend().getFile(
					message, false);
			if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR) {
				ir.bilgisoft.toopeto.entities.Conversation conversation = this.message.getConversation();
				this.mXmppConnectionService.renewSymmetricKey(conversation);
				content.setFileOffer(this.file, true);
				this.file.setKey(conversation.getSymmetricKey());
			} else {
				content.setFileOffer(this.file, false);
			}
			this.transportId = this.mJingleConnectionManager.nextRandomId();
			content.setTransportId(this.transportId);
			content.socks5transport().setChildren(getCandidatesAsElements());
			packet.setContent(content);
			this.sendJinglePacket(packet);
			this.mJingleStatus = JINGLE_STATUS_INITIATED;
		}
	}

	private List<ir.bilgisoft.toopeto.xml.Element> getCandidatesAsElements() {
		List<ir.bilgisoft.toopeto.xml.Element> elements = new ArrayList<>();
		for (ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate c : this.candidates) {
			elements.add(c.toElement());
		}
		return elements;
	}

	private void sendAccept() {
		mJingleStatus = JINGLE_STATUS_ACCEPTED;
		this.mStatus = ir.bilgisoft.toopeto.entities.Downloadable.STATUS_DOWNLOADING;
		mXmppConnectionService.updateConversationUi();
		this.mJingleConnectionManager.getPrimaryCandidate(this.account,
				new ir.bilgisoft.toopeto.xmpp.jingle.OnPrimaryCandidateFound() {

					@Override
					public void onPrimaryCandidateFound(boolean success,
							final ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate candidate) {
						final ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet = bootstrapPacket("session-accept");
						final ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content content = new ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content(contentCreator,
								contentName);
						content.setFileOffer(fileOffer);
						content.setTransportId(transportId);
						if ((success) && (!equalCandidateExists(candidate))) {
							final ir.bilgisoft.toopeto.xmpp.jingle.JingleSocks5Transport socksConnection = new ir.bilgisoft.toopeto.xmpp.jingle.JingleSocks5Transport(
									JingleConnection.this, candidate);
							connections.put(candidate.getCid(), socksConnection);
							socksConnection.connect(new ir.bilgisoft.toopeto.xmpp.jingle.OnTransportConnected() {

								@Override
								public void failed() {
									Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
											"connection to our own primary candidate failed");
									content.socks5transport().setChildren(
											getCandidatesAsElements());
									packet.setContent(content);
									sendJinglePacket(packet);
									connectNextCandidate();
								}

								@Override
								public void established() {
									Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
											"connected to primary candidate");
									mergeCandidate(candidate);
									content.socks5transport().setChildren(
											getCandidatesAsElements());
									packet.setContent(content);
									sendJinglePacket(packet);
									connectNextCandidate();
								}
							});
						} else {
							Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
									"did not find a primary candidate for ourself");
							content.socks5transport().setChildren(
									getCandidatesAsElements());
							packet.setContent(content);
							sendJinglePacket(packet);
							connectNextCandidate();
						}
					}
				});

	}

	private ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket bootstrapPacket(String action) {
		ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet = new ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket();
		packet.setAction(action);
		packet.setFrom(account.getJid());
		packet.setTo(this.message.getCounterpart());
		packet.setSessionId(this.sessionId);
		packet.setInitiator(this.initiator);
		return packet;
	}

	private void sendJinglePacket(ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet) {
		// Log.d(Config.LOGTAG,packet.toString());
	//	account.getXmppConnection().sendIqPacket(packet, responseListener);
	}

	private boolean receiveAccept(ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet) {
		ir.bilgisoft.toopeto.xmpp.jingle.stanzas.Content content = packet.getJingleContent();
		mergeCandidates(JingleCandidate.parse(content.socks5transport()
                .getChildren()));
		this.mJingleStatus = JINGLE_STATUS_ACCEPTED;
		mXmppConnectionService.markMessage(message, Message.STATUS_UNSEND);
		this.connectNextCandidate();
		return true;
	}

	private boolean receiveTransportInfo(JinglePacket packet) {
		Content content = packet.getJingleContent();
		if (content.hasSocks5Transport()) {
			if (content.socks5transport().hasChild("activated")) {
				if ((this.transport != null)
						&& (this.transport instanceof JingleSocks5Transport)) {
					onProxyActivated.success();
				} else {
					String cid = content.socks5transport()
							.findChild("activated").getAttribute("cid");
					Log.d(Config.LOGTAG, "received proxy activated (" + cid
							+ ")prior to choosing our own transport");
					JingleSocks5Transport connection = this.connections
							.get(cid);
					if (connection != null) {
						connection.setActivated(true);
					} else {
						Log.d(Config.LOGTAG, "activated connection not found");
						this.sendCancel();
						this.fail();
					}
				}
				return true;
			} else if (content.socks5transport().hasChild("proxy-error")) {
				onProxyActivated.failed();
				return true;
			} else if (content.socks5transport().hasChild("candidate-error")) {
				Log.d(Config.LOGTAG, "received candidate error");
				this.receivedCandidate = true;
				if ((mJingleStatus == JINGLE_STATUS_ACCEPTED)
						&& (this.sentCandidate)) {
					this.connect();
				}
				return true;
			} else if (content.socks5transport().hasChild("candidate-used")) {
				String cid = content.socks5transport()
						.findChild("candidate-used").getAttribute("cid");
				if (cid != null) {
					Log.d(Config.LOGTAG, "candidate used by counterpart:" + cid);
					JingleCandidate candidate = getCandidate(cid);
					candidate.flagAsUsedByCounterpart();
					this.receivedCandidate = true;
					if ((mJingleStatus == JINGLE_STATUS_ACCEPTED)
							&& (this.sentCandidate)) {
						this.connect();
					} else {
						Log.d(Config.LOGTAG,
								"ignoring because file is already in transmission or we havent sent our candidate yet");
					}
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	private void connect() {
		final JingleSocks5Transport connection = chooseConnection();
		this.transport = connection;
		if (connection == null) {
			Log.d(Config.LOGTAG, "could not find suitable candidate");
			this.disconnectSocks5Connections();
			if (this.initiator.equals(account.getJid())) {
				this.sendFallbackToIbb();
			}
		} else {
			this.mJingleStatus = JINGLE_STATUS_TRANSMITTING;
			if (connection.needsActivation()) {
				if (connection.getCandidate().isOurs()) {
					Log.d(Config.LOGTAG, "candidate "
							+ connection.getCandidate().getCid()
							+ " was our proxy. going to activate");
					IqPacket activation = new IqPacket(IqPacket.TYPE.SET);
					activation.setTo(connection.getCandidate().getJid());
					activation.query("http://jabber.org/protocol/bytestreams")
							.setAttribute("sid", this.getSessionId());
					activation.query().addChild("activate")
							.setContent(this.getCounterPart().toString());
/*
                    					this.account.getXmppConnection().sendIqPacket(activation,
							new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {

								@Override
								public void onIqPacketReceived(Account account,
										IqPacket packet) {
									if (packet.getType() == IqPacket.TYPE.ERROR) {
										onProxyActivated.failed();
									} else {
										onProxyActivated.success();
										sendProxyActivated(connection
												.getCandidate().getCid());
									}
								}
							});*/
				} else {
					Log.d(Config.LOGTAG,
							"candidate "
									+ connection.getCandidate().getCid()
									+ " was a proxy. waiting for other party to activate");
				}
			} else {
				if (initiator.equals(account.getJid())) {
					Log.d(Config.LOGTAG, "we were initiating. sending file");
					connection.send(file, onFileTransmissionSatusChanged);
				} else {
					Log.d(Config.LOGTAG, "we were responding. receiving file");
					connection.receive(file, onFileTransmissionSatusChanged);
				}
			}
		}
	}

	private JingleSocks5Transport chooseConnection() {
		JingleSocks5Transport connection = null;
		for (Entry<String, JingleSocks5Transport> cursor : connections
				.entrySet()) {
			JingleSocks5Transport currentConnection = cursor.getValue();
			// Log.d(Config.LOGTAG,"comparing candidate: "+currentConnection.getCandidate().toString());
			if (currentConnection.isEstablished()
					&& (currentConnection.getCandidate().isUsedByCounterpart() || (!currentConnection
							.getCandidate().isOurs()))) {
				// Log.d(Config.LOGTAG,"is usable");
				if (connection == null) {
					connection = currentConnection;
				} else {
					if (connection.getCandidate().getPriority() < currentConnection
							.getCandidate().getPriority()) {
						connection = currentConnection;
					} else if (connection.getCandidate().getPriority() == currentConnection
							.getCandidate().getPriority()) {
						// Log.d(Config.LOGTAG,"found two candidates with same priority");
						if (initiator.equals(account.getJid())) {
							if (currentConnection.getCandidate().isOurs()) {
								connection = currentConnection;
							}
						} else {
							if (!currentConnection.getCandidate().isOurs()) {
								connection = currentConnection;
							}
						}
					}
				}
			}
		}
		return connection;
	}

	private void sendSuccess() {
		JinglePacket packet = bootstrapPacket("session-terminate");
		Reason reason = new Reason();
		reason.addChild("success");
		packet.setReason(reason);
		this.sendJinglePacket(packet);
		this.disconnectSocks5Connections();
		this.mJingleStatus = JINGLE_STATUS_FINISHED;
		this.message.setStatus(Message.STATUS_RECEIVED);
		this.message.setDownloadable(null);
		this.mXmppConnectionService.updateMessage(message);
		this.mJingleConnectionManager.finishConnection(this);
	}

	private void sendFallbackToIbb() {
		Log.d(Config.LOGTAG, "sending fallback to ibb");
		JinglePacket packet = this.bootstrapPacket("transport-replace");
		Content content = new Content(this.contentCreator, this.contentName);
		this.transportId = this.mJingleConnectionManager.nextRandomId();
		content.setTransportId(this.transportId);
		content.ibbTransport().setAttribute("block-size",
				Integer.toString(this.ibbBlockSize));
		packet.setContent(content);
		this.sendJinglePacket(packet);
	}

	private boolean receiveFallbackToIbb(JinglePacket packet) {
		Log.d(Config.LOGTAG, "receiving fallack to ibb");
		String receivedBlockSize = packet.getJingleContent().ibbTransport()
				.getAttribute("block-size");
		if (receivedBlockSize != null) {
			int bs = Integer.parseInt(receivedBlockSize);
			if (bs > this.ibbBlockSize) {
				this.ibbBlockSize = bs;
			}
		}
		this.transportId = packet.getJingleContent().getTransportId();
		this.transport = new JingleInbandTransport(this, this.transportId, this.ibbBlockSize);
		this.transport.receive(file, onFileTransmissionSatusChanged);
		JinglePacket answer = bootstrapPacket("transport-accept");
		Content content = new Content("initiator", "a-file-offer");
		content.setTransportId(this.transportId);
		content.ibbTransport().setAttribute("block-size",
				Integer.toString(this.ibbBlockSize));
		answer.setContent(content);
		this.sendJinglePacket(answer);
		return true;
	}

	private boolean receiveTransportAccept(JinglePacket packet) {
		if (packet.getJingleContent().hasIbbTransport()) {
			String receivedBlockSize = packet.getJingleContent().ibbTransport()
					.getAttribute("block-size");
			if (receivedBlockSize != null) {
				int bs = Integer.parseInt(receivedBlockSize);
				if (bs > this.ibbBlockSize) {
					this.ibbBlockSize = bs;
				}
			}
			this.transport = new JingleInbandTransport(this, this.transportId, this.ibbBlockSize);
			this.transport.connect(new ir.bilgisoft.toopeto.xmpp.jingle.OnTransportConnected() {

				@Override
				public void failed() {
					Log.d(Config.LOGTAG, "ibb open failed");
				}

				@Override
				public void established() {
					JingleConnection.this.transport.send(file,
							onFileTransmissionSatusChanged);
				}
			});
			return true;
		} else {
			return false;
		}
	}

	private void receiveSuccess() {
		this.mJingleStatus = JINGLE_STATUS_FINISHED;
		this.mXmppConnectionService.markMessage(this.message,
				Message.STATUS_SEND);
		this.disconnectSocks5Connections();
		if (this.transport != null && this.transport instanceof JingleInbandTransport) {
			this.transport.disconnect();
		}
		this.message.setDownloadable(null);
		this.mJingleConnectionManager.finishConnection(this);
	}

	public void cancel() {
		this.disconnectSocks5Connections();
		if (this.transport != null && this.transport instanceof JingleInbandTransport) {
			this.transport.disconnect();
		}
		this.sendCancel();
		this.mJingleConnectionManager.finishConnection(this);
		if (this.responder.equals(account.getJid())) {
			this.message.setDownloadable(new DownloadablePlaceholder(Downloadable.STATUS_FAILED));
			if (this.file!=null) {
				file.delete();
			}
			this.mXmppConnectionService.updateConversationUi();
		} else {
			this.mXmppConnectionService.markMessage(this.message,
					Message.STATUS_SEND_FAILED);
			this.message.setDownloadable(null);
		}
	}

	private void fail() {
		this.mJingleStatus = JINGLE_STATUS_FAILED;
		this.disconnectSocks5Connections();
		if (this.transport != null && this.transport instanceof JingleInbandTransport) {
			this.transport.disconnect();
		}
		if (this.message != null) {
			if (this.responder.equals(account.getJid())) {
				this.message.setDownloadable(new DownloadablePlaceholder(Downloadable.STATUS_FAILED));
				if (this.file!=null) {
					file.delete();
				}
				this.mXmppConnectionService.updateConversationUi();
			} else {
				this.mXmppConnectionService.markMessage(this.message,
						Message.STATUS_SEND_FAILED);
				this.message.setDownloadable(null);
			}
		}
		this.mJingleConnectionManager.finishConnection(this);
	}

	private void sendCancel() {
		JinglePacket packet = bootstrapPacket("session-terminate");
		Reason reason = new Reason();
		reason.addChild("cancel");
		packet.setReason(reason);
		this.sendJinglePacket(packet);
	}

	private void connectNextCandidate() {
		for (JingleCandidate candidate : this.candidates) {
			if ((!connections.containsKey(candidate.getCid()) && (!candidate
					.isOurs()))) {
				this.connectWithCandidate(candidate);
				return;
			}
		}
		this.sendCandidateError();
	}

	private void connectWithCandidate(final JingleCandidate candidate) {
		final JingleSocks5Transport socksConnection = new JingleSocks5Transport(
				this, candidate);
		connections.put(candidate.getCid(), socksConnection);
		socksConnection.connect(new ir.bilgisoft.toopeto.xmpp.jingle.OnTransportConnected() {

			@Override
			public void failed() {
				Log.d(Config.LOGTAG,
						"connection failed with " + candidate.getHost() + ":"
								+ candidate.getPort());
				connectNextCandidate();
			}

			@Override
			public void established() {
				Log.d(Config.LOGTAG,
						"established connection with " + candidate.getHost()
								+ ":" + candidate.getPort());
				sendCandidateUsed(candidate.getCid());
			}
		});
	}

	private void disconnectSocks5Connections() {
		Iterator<Entry<String, JingleSocks5Transport>> it = this.connections
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, JingleSocks5Transport> pairs = it.next();
			pairs.getValue().disconnect();
			it.remove();
		}
	}

	private void sendProxyActivated(String cid) {
		JinglePacket packet = bootstrapPacket("transport-info");
		Content content = new Content(this.contentCreator, this.contentName);
		content.setTransportId(this.transportId);
		content.socks5transport().addChild("activated")
				.setAttribute("cid", cid);
		packet.setContent(content);
		this.sendJinglePacket(packet);
	}

	private void sendCandidateUsed(final String cid) {
		JinglePacket packet = bootstrapPacket("transport-info");
		Content content = new Content(this.contentCreator, this.contentName);
		content.setTransportId(this.transportId);
		content.socks5transport().addChild("candidate-used")
				.setAttribute("cid", cid);
		packet.setContent(content);
		this.sentCandidate = true;
		if ((receivedCandidate) && (mJingleStatus == JINGLE_STATUS_ACCEPTED)) {
			connect();
		}
		this.sendJinglePacket(packet);
	}

	private void sendCandidateError() {
		JinglePacket packet = bootstrapPacket("transport-info");
		Content content = new Content(this.contentCreator, this.contentName);
		content.setTransportId(this.transportId);
		content.socks5transport().addChild("candidate-error");
		packet.setContent(content);
		this.sentCandidate = true;
		if ((receivedCandidate) && (mJingleStatus == JINGLE_STATUS_ACCEPTED)) {
			connect();
		}
		this.sendJinglePacket(packet);
	}

	public Jid getInitiator() {
		return this.initiator;
	}

	public Jid getResponder() {
		return this.responder;
	}

	public int getJingleStatus() {
		return this.mJingleStatus;
	}

	private boolean equalCandidateExists(JingleCandidate candidate) {
		for (JingleCandidate c : this.candidates) {
			if (c.equalValues(candidate)) {
				return true;
			}
		}
		return false;
	}

	private void mergeCandidate(JingleCandidate candidate) {
		for (JingleCandidate c : this.candidates) {
			if (c.equals(candidate)) {
				return;
			}
		}
		this.candidates.add(candidate);
	}

	private void mergeCandidates(List<JingleCandidate> candidates) {
		for (JingleCandidate c : candidates) {
			mergeCandidate(c);
		}
	}

	private JingleCandidate getCandidate(String cid) {
		for (JingleCandidate c : this.candidates) {
			if (c.getCid().equals(cid)) {
				return c;
			}
		}
		return null;
	}

	public void updateProgress(int i) {
		this.mProgress = i;
		if (SystemClock.elapsedRealtime() - this.mLastGuiRefresh > Config.PROGRESS_UI_UPDATE_INTERVAL) {
			this.mLastGuiRefresh = SystemClock.elapsedRealtime();
			mXmppConnectionService.updateConversationUi();
		}
	}

	interface OnProxyActivated {
		public void success();

		public void failed();
	}

	public boolean hasTransportId(String sid) {
		return sid.equals(this.transportId);
	}

	public JingleTransport getTransport() {
		return this.transport;
	}

	public boolean start() {
		if (account.getStatus() == Account.State.ONLINE) {
			if (mJingleStatus == JINGLE_STATUS_INITIATED) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						sendAccept();
					}
				}).start();
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int getStatus() {
		return this.mStatus;
	}

	@Override
	public long getFileSize() {
		if (this.file != null) {
			return this.file.getExpectedSize();
		} else {
			return 0;
		}
	}

	@Override
	public int getProgress() {
		return this.mProgress;
	}

	@Override
	public String getMimeType() {
		if (this.message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
			String mime = null;
			String path = this.message.getRelativeFilePath();
			if (path != null && !this.message.getRelativeFilePath().isEmpty()) {
				mime = URLConnection.guessContentTypeFromName(this.message.getRelativeFilePath());
				if (mime!=null) {
					return  mime;
				} else {
					return "";
				}
			} else {
				return "";
			}
		} else {
			return "image/webp";
		}
	}
}
