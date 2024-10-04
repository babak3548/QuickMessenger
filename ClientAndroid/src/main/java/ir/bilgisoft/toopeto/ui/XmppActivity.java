package ir.bilgisoft.toopeto.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.java.otr4j.session.SessionID;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import ir.bilgisoft.toopeto.R;
import ir.bilgisoft.toopeto.services.XmppConnectionService.XmppConnectionBinder;
import ir.bilgisoft.toopeto.entities.Account;
import ir.bilgisoft.toopeto.entities.Contact;
import ir.bilgisoft.toopeto.entities.Conversation;
import ir.bilgisoft.toopeto.entities.Message;
import ir.bilgisoft.toopeto.utils.ExceptionHelper;
import ir.bilgisoft.toopeto.xmpp.jid.Jid;

public abstract class XmppActivity extends Activity {

	protected static final int REQUEST_ANNOUNCE_PGP = 0x0101;
	protected static final int REQUEST_INVITE_TO_CONVERSATION = 0x0102;

	public ir.bilgisoft.toopeto.services.XmppConnectionService xmppConnectionService;
	public boolean xmppConnectionServiceBound = false;
	protected boolean registeredListeners = false;

	protected int mPrimaryTextColor;
	protected int mSecondaryTextColor;
	protected int mSecondaryBackgroundColor;
	protected int mColorRed;
	protected int mColorOrange;
	protected int mColorGreen;
	protected int mPrimaryColor;

	protected boolean mUseSubject = true;

	private DisplayMetrics metrics;
	protected int mTheme;
    ///////////////////////////////////////
    //set kardan update koonhaye natije
    protected void registerListeners() {
        if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnConversationUpdate) {
            this.xmppConnectionService.setOnConversationListChangedListener((ir.bilgisoft.toopeto.services.XmppConnectionService.OnConversationUpdate) this);
        }
        if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnAccountUpdate) {
            this.xmppConnectionService.setOnAccountListChangedListener((ir.bilgisoft.toopeto.services.XmppConnectionService.OnAccountUpdate) this);
        }
        if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnRosterUpdate) {
            this.xmppConnectionService.setOnRosterUpdateListener((ir.bilgisoft.toopeto.services.XmppConnectionService.OnRosterUpdate) this);
        }
        if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnMucRosterUpdate) {
            this.xmppConnectionService.setOnMucRosterUpdateListener((ir.bilgisoft.toopeto.services.XmppConnectionService.OnMucRosterUpdate) this);
        }
        if (this instanceof ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist) {
            this.xmppConnectionService.setOnUpdateBlocklistListener((ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist) this);
        }
        if(this instanceof ir.bilgisoft.toopeto.xmpp.OnReceivedRooms) {
            xmppConnectionService.setOnReceivedRooms((ir.bilgisoft.toopeto.xmpp.OnReceivedRooms) this);
        }
    }

   ////////////////////////////////////////
	protected interface OnValueEdited {
		public void onValueEdited(String value);
	}

	public interface OnPresenceSelected {
		public void onPresenceSelected();
	}

	protected ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			XmppConnectionBinder binder = (XmppConnectionBinder) service;
			xmppConnectionService = binder.getService();
			xmppConnectionServiceBound = true;
			if (!registeredListeners && shouldRegisterListeners()) {
				registerListeners();
				registeredListeners = true;
			}
			onBackendConnected();//shorou
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			xmppConnectionServiceBound = false;
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		if (!xmppConnectionServiceBound) {
			connectToBackend();
		} else {
			if (!registeredListeners) {
				this.registerListeners();
				this.registeredListeners = true;
			}
			this.onBackendConnected();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	protected boolean shouldRegisterListeners() {
		if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return !isDestroyed() && !isFinishing();
		} else {
			return !isFinishing();
		}
	}

	public void connectToBackend() {
		Intent intent = new Intent(this, ir.bilgisoft.toopeto.services.XmppConnectionService.class);
		intent.setAction("ui");
		startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (xmppConnectionServiceBound) {
			if (registeredListeners) {
				this.unregisterListeners();
				this.registeredListeners = false;
			}
			unbindService(mConnection);
			xmppConnectionServiceBound = false;
		}
	}

	protected void hideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		View focus = getCurrentFocus();

		if (focus != null) {

			inputManager.hideSoftInputFromWindow(focus.getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	public boolean hasPgp() {
		return xmppConnectionService.getPgpEngine() != null;
	}

	public void showInstallPgpDialog() {
		Builder builder = new Builder(this);
		builder.setTitle(getString(R.string.openkeychain_required));
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		builder.setMessage(getText(R.string.openkeychain_required_long));
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setNeutralButton(getString(R.string.restart),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (xmppConnectionServiceBound) {
							unbindService(mConnection);
							xmppConnectionServiceBound = false;
						}
						stopService(new Intent(XmppActivity.this,
									ir.bilgisoft.toopeto.services.XmppConnectionService.class));
						finish();
					}
				});
		builder.setPositiveButton(getString(R.string.install),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri
							.parse("market://details?id=org.sufficientlysecure.keychain");
						Intent marketIntent = new Intent(Intent.ACTION_VIEW,
								uri);
						PackageManager manager = getApplicationContext()
							.getPackageManager();
						List<ResolveInfo> infos = manager
							.queryIntentActivities(marketIntent, 0);
						if (infos.size() > 0) {
							startActivity(marketIntent);
						} else {
							uri = Uri.parse("http://www.openkeychain.org/");
							Intent browserIntent = new Intent(
									Intent.ACTION_VIEW, uri);
							startActivity(browserIntent);
						}
						finish();
					}
				});
		builder.create().show();
	}

	abstract void onBackendConnected();



	protected void unregisterListeners() {
		if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnConversationUpdate) {
			this.xmppConnectionService.removeOnConversationListChangedListener();
		}
		if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnAccountUpdate) {
			this.xmppConnectionService.removeOnAccountListChangedListener();
		}
		if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnRosterUpdate) {
			this.xmppConnectionService.removeOnRosterUpdateListener();
		}
		if (this instanceof ir.bilgisoft.toopeto.services.XmppConnectionService.OnMucRosterUpdate) {
			this.xmppConnectionService.removeOnMucRosterUpdateListener();
		}
		if (this instanceof ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist) {
			this.xmppConnectionService.removeOnUpdateBlocklistListener();
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				break;
			case R.id.action_accounts:
				startActivity(new Intent(this, ManageAccountActivity.class));
				break;
			case android.R.id.home:
				finish();
				break;
			case R.id.action_show_qr_code:
				showQrCode();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		metrics = getResources().getDisplayMetrics();
		ExceptionHelper.init(getApplicationContext());
		mPrimaryTextColor = getResources().getColor(R.color.primarytext);
		mSecondaryTextColor = getResources().getColor(R.color.secondarytext);
		mColorRed = getResources().getColor(R.color.red);
		mColorOrange = getResources().getColor(R.color.orange);
		mColorGreen = getResources().getColor(R.color.green);
		mPrimaryColor = getResources().getColor(R.color.primary);
		mSecondaryBackgroundColor = getResources().getColor(
				R.color.secondarybackground);
		this.mTheme = findTheme();
		setTheme(this.mTheme);
		mUseSubject = getPreferences().getBoolean("use_subject", true);
	}

	protected SharedPreferences getPreferences() {
		return PreferenceManager
			.getDefaultSharedPreferences(getApplicationContext());
	}

	public boolean useSubjectToIdentifyConference() {
		return mUseSubject;
	}

	public void switchToConversation(Conversation conversation) {
		switchToConversation(conversation, null, false);
	}

	public void switchToConversation(Conversation conversation, String text,
			boolean newTask) {
		switchToConversation(conversation,text,null,newTask);
	}

	public void highlightInMuc(Conversation conversation, String nick) {
		switchToConversation(conversation,null,nick,false);
	}

	private void switchToConversation(Conversation conversation, String text, String nick, boolean newTask) {
		Intent viewConversationIntent = new Intent(this,
				ConversationActivity.class);
		viewConversationIntent.setAction(Intent.ACTION_VIEW);
		viewConversationIntent.putExtra(ConversationActivity.CONVERSATION,
				conversation.getUuid());
		if (text != null) {
			viewConversationIntent.putExtra(ConversationActivity.TEXT, text);
		}
		if (nick != null) {
			viewConversationIntent.putExtra(ConversationActivity.NICK, nick);
		}
		viewConversationIntent.setType(ConversationActivity.VIEW_CONVERSATION);
		if (newTask) {
			viewConversationIntent.setFlags(viewConversationIntent.getFlags()
					| Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		} else {
			viewConversationIntent.setFlags(viewConversationIntent.getFlags()
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		startActivity(viewConversationIntent);
		finish();
	}

	public void switchToContactDetails(Contact contact) {
		Intent intent = new Intent(this, ContactDetailsActivity.class);
		intent.setAction(ContactDetailsActivity.ACTION_VIEW_CONTACT);
		intent.putExtra("account", contact.getAccount().getJid().toBareJid().toString());
		intent.putExtra("contact", contact.getJid().toString());
		startActivity(intent);
	}

	public void switchToAccount(Account account) {
		Intent intent = new Intent(this, EditAccountActivity.class);
		intent.putExtra("jid", account.getJid().toBareJid().toString());
		startActivity(intent);
	}

	protected void inviteToConversation(Conversation conversation) {
		Intent intent = new Intent(getApplicationContext(),
				ChooseContactActivity.class);
		intent.putExtra("conversation", conversation.getUuid());
		startActivityForResult(intent, REQUEST_INVITE_TO_CONVERSATION);
	}

	protected void announcePgp(Account account, final ir.bilgisoft.toopeto.entities.Conversation conversation) {
		xmppConnectionService.getPgpEngine().generateSignature(account,
				"online", new ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.entities.Account>() {

					@Override
					public void userInputRequried(PendingIntent pi,
							Account account) {
						try {
							startIntentSenderForResult(pi.getIntentSender(),
									REQUEST_ANNOUNCE_PGP, null, 0, 0, 0);
						} catch (final SendIntentException ignored) {
						}
					}

					@Override
					public void success(Account account) {
						xmppConnectionService.databaseBackend
							.updateAccount(account);
						xmppConnectionService.sendPresencePacket(account);
						if (conversation != null) {
							conversation
								.setNextEncryption(Message.ENCRYPTION_PGP);
							xmppConnectionService.databaseBackend
								.updateConversation(conversation);
						}
					}

					@Override
					public void error(int error, Account account) {
						displayErrorDialog(error);
					}
				});
	}

	protected void displayErrorDialog(final int errorCode) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Builder builder = new Builder(
						XmppActivity.this);
				builder.setIconAttribute(android.R.attr.alertDialogIcon);
				builder.setTitle(getString(R.string.error));
				builder.setMessage(errorCode);
				builder.setNeutralButton(R.string.accept, null);
				builder.create().show();
			}
		});

	}

	protected void showAddToRosterDialog(final Conversation conversation) {
		final Jid jid = conversation.getJid();
		Builder builder = new Builder(this);
		builder.setTitle(jid.toString());
		builder.setMessage(getString(R.string.not_in_roster));
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.add_contact),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = conversation.getJid();
						ir.bilgisoft.toopeto.entities.Account account = conversation.getAccount();
						ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(jid.getLocalpart());
						xmppConnectionService.createContact(contact);
						switchToContactDetails(contact);
					}
				});
		builder.create().show();
	}

	private void showAskForPresenceDialog(final ir.bilgisoft.toopeto.entities.Contact contact) {
		Builder builder = new Builder(this);
		builder.setTitle(contact.getJid().toString());
		builder.setMessage(R.string.request_presence_updates);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.request_now,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (xmppConnectionServiceBound) {
							xmppConnectionService.sendPresencePacket(contact.getAccount());
						}
					}
				});
		builder.create().show();
	}

	private void warnMutalPresenceSubscription(final ir.bilgisoft.toopeto.entities.Conversation conversation,
			final OnPresenceSelected listener) {
		Builder builder = new Builder(this);
		builder.setTitle(conversation.getContact().getJid().toString());
		builder.setMessage(R.string.without_mutual_presence_updates);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.ignore, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				conversation.setNextCounterpart(null);
				if (listener != null) {
					listener.onPresenceSelected();
				}
			}
		});
		builder.create().show();
	}

	protected void quickEdit(String previousValue, OnValueEdited callback) {
		quickEdit(previousValue, callback, false);
	}

	protected void quickPasswordEdit(String previousValue,
			OnValueEdited callback) {
		quickEdit(previousValue, callback, true);
	}

	@SuppressLint("InflateParams")
	private void quickEdit(final String previousValue,
			final OnValueEdited callback, boolean password) {
		Builder builder = new Builder(this);
		View view = getLayoutInflater().inflate(R.layout.quickedit, null);
		final EditText editor = (EditText) view.findViewById(R.id.editor);
		OnClickListener mClickListener = new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String value = editor.getText().toString();
				if (!previousValue.equals(value) && value.trim().length() > 0) {
					callback.onValueEdited(value);
				}
			}
		};
		if (password) {
			editor.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			editor.setHint(R.string.password);
			builder.setPositiveButton(R.string.accept, mClickListener);
		} else {
			builder.setPositiveButton(R.string.edit, mClickListener);
		}
		editor.requestFocus();
		editor.setText(previousValue);
		builder.setView(view);
		builder.setNegativeButton(R.string.cancel, null);
		builder.create().show();
	}

	public void selectPresence(final ir.bilgisoft.toopeto.entities.Conversation conversation,
			final OnPresenceSelected listener) {
		final ir.bilgisoft.toopeto.entities.Contact contact = conversation.getContact();
		if (conversation.hasValidOtrSession()) {
			SessionID id = conversation.getOtrSession().getSessionID();
			ir.bilgisoft.toopeto.xmpp.jid.Jid jid;
			try {
				jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(id.getAccountID() + "/" + id.getUserID());
			} catch (ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
				jid = null;
			}
			conversation.setNextCounterpart(jid);
			listener.onPresenceSelected();
		} else 	if (!contact.showInRoster()) {
			showAddToRosterDialog(conversation);
		} else {
			ir.bilgisoft.toopeto.entities.Presences presences = contact.getPresences();
			if (presences.size() == 0) {
				if (!contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.TO)
						&& !contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.ASKING)
						&& contact.getAccount().getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
					showAskForPresenceDialog(contact);
				} else if (!contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.TO)
						|| !contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.FROM)) {
					warnMutalPresenceSubscription(conversation, listener);
				} else {
					conversation.setNextCounterpart(null);
					listener.onPresenceSelected();
				}
			} else if (presences.size() == 1) {
				String presence = presences.asStringArray()[0];
				try {
					conversation.setNextCounterpart(ir.bilgisoft.toopeto.xmpp.jid.Jid.fromParts(contact.getJid().getLocalpart(), contact.getJid().getDomainpart(), presence));
				} catch (ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
					conversation.setNextCounterpart(null);
				}
				listener.onPresenceSelected();
			} else {
				final StringBuilder presence = new StringBuilder();
				Builder builder = new Builder(this);
				builder.setTitle(getString(R.string.choose_presence));
				final String[] presencesArray = presences.asStringArray();
				int preselectedPresence = 0;
				for (int i = 0; i < presencesArray.length; ++i) {
					if (presencesArray[i].equals(contact.lastseen.presence)) {
						preselectedPresence = i;
						break;
					}
				}
				presence.append(presencesArray[preselectedPresence]);
				builder.setSingleChoiceItems(presencesArray,
						preselectedPresence,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								presence.delete(0, presence.length());
								presence.append(presencesArray[which]);
							}
						});
				builder.setNegativeButton(R.string.cancel, null);
				builder.setPositiveButton(R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							conversation.setNextCounterpart(ir.bilgisoft.toopeto.xmpp.jid.Jid.fromParts(contact.getJid().getLocalpart(), contact.getJid().getDomainpart(), presence.toString()));
						} catch (ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
							conversation.setNextCounterpart(null);
						}
						listener.onPresenceSelected();
					}
				});
				builder.create().show();
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode,
			final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_INVITE_TO_CONVERSATION
				&& resultCode == RESULT_OK) {
			try {
				ir.bilgisoft.toopeto.xmpp.jid.Jid jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(data.getStringExtra("contact"));
				String conversationUuid = data.getStringExtra("conversation");
				ir.bilgisoft.toopeto.entities.Conversation conversation = xmppConnectionService
					.findConversationByUuid(conversationUuid);
				if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
					xmppConnectionService.invite(conversation, jid);
				} else {
					List<ir.bilgisoft.toopeto.xmpp.jid.Jid> jids = new ArrayList<ir.bilgisoft.toopeto.xmpp.jid.Jid>();
					jids.add(conversation.getJid());
					jids.add(jid);
					xmppConnectionService.createAdhocConference(conversation.getAccount(), jids, adhocCallback);
				}
			} catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException ignored) {

			}
				}
	}

	private ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.entities.Conversation> adhocCallback = new ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.entities.Conversation>() {
		@Override
		public void success(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
			switchToConversation(conversation);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(XmppActivity.this,R.string.conference_created,Toast.LENGTH_LONG).show();
				}
			});
		}

		@Override
		public void error(final int errorCode, ir.bilgisoft.toopeto.entities.Conversation object) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(XmppActivity.this,errorCode,Toast.LENGTH_LONG).show();
				}
			});
		}

		@Override
		public void userInputRequried(PendingIntent pi, ir.bilgisoft.toopeto.entities.Conversation object) {

		}
	};

	public int getSecondaryTextColor() {
		return this.mSecondaryTextColor;
	}

	public int getPrimaryTextColor() {
		return this.mPrimaryTextColor;
	}

	public int getWarningTextColor() {
		return this.mColorRed;
	}

	public int getPrimaryColor() {
		return this.mPrimaryColor;
	}

	public int getSecondaryBackgroundColor() {
		return this.mSecondaryBackgroundColor;
	}

	public int getPixel(int dp) {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		return ((int) (dp * metrics.density));
	}

	public boolean copyTextToClipboard(String text, int labelResId) {
		ClipboardManager mClipBoardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		String label = getResources().getString(labelResId);
		if (mClipBoardManager != null) {
			ClipData mClipData = ClipData.newPlainText(label, text);
			mClipBoardManager.setPrimaryClip(mClipData);
			return true;
		}
		return false;
	}

	protected void registerNdefPushMessageCallback() {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter != null && nfcAdapter.isEnabled()) {
			nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
				@Override
				public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
					return new NdefMessage(new NdefRecord[]{
						NdefRecord.createUri(getShareableUri()),
							NdefRecord.createApplicationRecord("ir.bilgisoft.toopeto")
					});
				}
			}, this);
		}
	}

	protected void unregisterNdefPushMessageCallback() {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter != null && nfcAdapter.isEnabled()) {
			nfcAdapter.setNdefPushMessageCallback(null,this);
		}
	}

	protected String getShareableUri() {
		return null;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.getShareableUri()!=null) {
			this.registerNdefPushMessageCallback();
		}
	}

	protected int findTheme() {
        /*
        		String config =getPreferences().getString("theme", "ConversationsTheme");
		try {
			return (int)Long.parseLong(config);
		} catch (NumberFormatException e) {
			return 524288;
		}*/

/**/
		if ("ConversationsTheme".equals( getPreferences().getString("theme_key", "")))
        {
			return R.style.ConversationsTheme;
		} else if ("ConversationsThemeSecond".equals(getPreferences().getString("theme_key", "")))
        {
           return R.style.ConversationsThemeSecond;
        }
        else if ("ConversationsThemeSecond_LargerText".equals(getPreferences().getString("theme_key", "")))
             {
            return  R.style.ConversationsThemeSecond_LargerText;
        }
        else if("ConversationsTheme_LargerText".equals( getPreferences().getString("theme_key", "")))
             {
			return R.style.ConversationsTheme_LargerText;
		}
        else {
            return R.style.ConversationsTheme;
        }
	}

	@Override
	public void onPause() {
		super.onPause();
		this.unregisterNdefPushMessageCallback();
	}

	protected void showQrCode() {
		String uri = getShareableUri();
		if (uri!=null) {
			Point size = new Point();
			getWindowManager().getDefaultDisplay().getSize(size);
			final int width = (size.x < size.y ? size.x : size.y);
			Bitmap bitmap = createQrCodeBitmap(uri, width);
			ImageView view = new ImageView(this);
			view.setImageBitmap(bitmap);
			Builder builder = new Builder(this);
			builder.setView(view);
			builder.create().show();
		}
	}

	protected Bitmap createQrCodeBitmap(String input, int size) {
		Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,"qr code requested size: "+size);
		try {
			final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();
			final Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
			final BitMatrix result = QR_CODE_WRITER.encode(input, BarcodeFormat.QR_CODE, size, size, hints);
			final int width = result.getWidth();
			final int height = result.getHeight();
			final int[] pixels = new int[width * height];
			for (int y = 0; y < height; y++) {
				final int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
				}
			}
			final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,"output size: "+width+"x"+height);
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			return bitmap;
		} catch (final WriterException e) {
			return null;
		}
	}

	public ir.bilgisoft.toopeto.services.AvatarService avatarService() {
		return xmppConnectionService.getAvatarService();
	}

	class BitmapWorkerTask extends AsyncTask<ir.bilgisoft.toopeto.entities.Message, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private ir.bilgisoft.toopeto.entities.Message message = null;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<>(imageView);
		}

		@Override
		protected Bitmap doInBackground(ir.bilgisoft.toopeto.entities.Message... params) {
			message = params[0];
			try {
				return xmppConnectionService.getFileBackend().getThumbnail(
						message, (int) (metrics.density * 288), false);
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					imageView.setBackgroundColor(0x00000000);
				}
			}
		}
	}

	public void loadBitmap(ir.bilgisoft.toopeto.entities.Message message, ImageView imageView) {
		Bitmap bm;
		try {
			bm = xmppConnectionService.getFileBackend().getThumbnail(message,
					(int) (metrics.density * 288), true);
		} catch (FileNotFoundException e) {
			bm = null;
		}
		if (bm != null) {
			imageView.setImageBitmap(bm);
			imageView.setBackgroundColor(0x00000000);
		} else {
			if (cancelPotentialWork(message, imageView)) {
				imageView.setBackgroundColor(0xff333333);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(
						getResources(), null, task);
				imageView.setImageDrawable(asyncDrawable);
				try {
					task.execute(message);
				} catch (final RejectedExecutionException ignored) {
				}
			}
		}
	}

	public static boolean cancelPotentialWork(ir.bilgisoft.toopeto.entities.Message message,
			ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final ir.bilgisoft.toopeto.entities.Message oldMessage = bitmapWorkerTask.message;
			if (oldMessage == null || message != oldMessage) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap,
				BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<>(
					bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}
}
