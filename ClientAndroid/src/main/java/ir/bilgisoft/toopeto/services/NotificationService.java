package ir.bilgisoft.toopeto.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigPictureStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import ir.bilgisoft.toopeto.R;
import ir.bilgisoft.toopeto.entities.Account;
import ir.bilgisoft.toopeto.ui.ConversationActivity;
import ir.bilgisoft.toopeto.ui.ManageAccountActivity;

public class NotificationService {

	private ir.bilgisoft.toopeto.services.XmppConnectionService mXmppConnectionService;

	private final LinkedHashMap<String, ArrayList<ir.bilgisoft.toopeto.entities.Message>> notifications = new LinkedHashMap<>();

	public static final int NOTIFICATION_ID = 0x2342;
	public static final int FOREGROUND_NOTIFICATION_ID = 0x8899;
	public static final int ERROR_NOTIFICATION_ID = 0x5678;

	private ir.bilgisoft.toopeto.entities.Conversation mOpenConversation;
	private boolean mIsInForeground;
	private long mLastNotification;

	public NotificationService(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		this.mXmppConnectionService = service;
	}

	public boolean notify(final ir.bilgisoft.toopeto.entities.Message message) {
		return (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED)
			&& notificationsEnabled()
			&& !message.getConversation().isMuted()
			&& (message.getConversation().getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE
					|| conferenceNotificationsEnabled()
					|| wasHighlightedOrPrivate(message)
				 );
	}

	public void notifyPebble(ir.bilgisoft.toopeto.entities.Message message) {
		final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

		final HashMap data = new HashMap();
		final ir.bilgisoft.toopeto.entities.Conversation conversation = message.getConversation();
		data.put("title", conversation.getName());
		data.put("body", message.getBody());
		final JSONObject jsonData = new JSONObject(data);
		final String notificationData = new JSONArray().put(jsonData).toString();

		i.putExtra("messageType", "PEBBLE_ALERT");
		i.putExtra("sender", "Conversations"); /* XXX: Shouldn't be hardcoded, e.g., AbstractGenerator.APP_NAME); */
		i.putExtra("notificationData", notificationData);

		mXmppConnectionService.sendBroadcast(i);
	}


	public boolean notificationsEnabled() {
		return mXmppConnectionService.getPreferences().getBoolean("show_notification", true);
	}

	public boolean isQuietHours() {
		if (!mXmppConnectionService.getPreferences().getBoolean("enable_quiet_hours", false)) {
			return false;
		}
		final long startTime = mXmppConnectionService.getPreferences().getLong("quiet_hours_start", ir.bilgisoft.toopeto.ui.TimePreference.DEFAULT_VALUE) % ir.bilgisoft.toopeto.Config.MILLISECONDS_IN_DAY;
		final long endTime = mXmppConnectionService.getPreferences().getLong("quiet_hours_end", ir.bilgisoft.toopeto.ui.TimePreference.DEFAULT_VALUE) % ir.bilgisoft.toopeto.Config.MILLISECONDS_IN_DAY;
		final long nowTime = Calendar.getInstance().getTimeInMillis() % ir.bilgisoft.toopeto.Config.MILLISECONDS_IN_DAY;

		if (endTime < startTime) {
			return nowTime > startTime || nowTime < endTime;
		} else {
			return nowTime > startTime && nowTime < endTime;
		}
	}

	public boolean conferenceNotificationsEnabled() {
		return mXmppConnectionService.getPreferences().getBoolean("always_notify_in_conference", false);
	}

	public void push(final ir.bilgisoft.toopeto.entities.Message message) {
		if (!notify(message)) {
			return;
		}
		final PowerManager pm = (PowerManager) mXmppConnectionService
			.getSystemService(Context.POWER_SERVICE);
		final boolean isScreenOn = pm.isScreenOn();

		if (this.mIsInForeground && isScreenOn
				&& this.mOpenConversation == message.getConversation()) {
			return;
				}
		synchronized (notifications) {
			final String conversationUuid = message.getConversationUuid();
			if (notifications.containsKey(conversationUuid)) {
				notifications.get(conversationUuid).add(message);
			} else {
				final ArrayList<ir.bilgisoft.toopeto.entities.Message> mList = new ArrayList<>();
				mList.add(message);
				notifications.put(conversationUuid, mList);
			}
			final ir.bilgisoft.toopeto.entities.Account account = message.getConversation().getAccount();
			final boolean doNotify = (!(this.mIsInForeground && this.mOpenConversation == null) || !isScreenOn)
					&& !account.inGracePeriod()
					&& !this.inMiniGracePeriod(account);
			updateNotification(doNotify);
			if (doNotify) {
				notifyPebble(message);
			}
		}

	}

	public void clear() {
		synchronized (notifications) {
			notifications.clear();
			updateNotification(false);
		}
	}

	public void clear(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
		synchronized (notifications) {
			notifications.remove(conversation.getUuid());
			updateNotification(false);
		}
	}

	private void updateNotification(final boolean notify) {
		final NotificationManager notificationManager = (NotificationManager) mXmppConnectionService
			.getSystemService(Context.NOTIFICATION_SERVICE);
		final SharedPreferences preferences = mXmppConnectionService.getPreferences();

		final String ringtone = preferences.getString("notification_ringtone", null);
		final boolean vibrate = preferences.getBoolean("vibrate_on_notification", true);

		if (notifications.size() == 0) {
			notificationManager.cancel(NOTIFICATION_ID);
		} else {
			if (notify) {
				this.markLastNotification();
			}
			final Builder mBuilder;
			if (notifications.size() == 1) {
				mBuilder = buildSingleConversations(notify);
			} else {
				mBuilder = buildMultipleConversation();
			}
			if (notify && !isQuietHours()) {
				if (vibrate) {
					final int dat = 70;
					final long[] pattern = {0, 3 * dat, dat, dat};
					mBuilder.setVibrate(pattern);
				}
				if (ringtone != null) {
					mBuilder.setSound(Uri.parse(ringtone));
				}
			}
			mBuilder.setSmallIcon(R.drawable.ic_notification);
			mBuilder.setDeleteIntent(createDeleteIntent());
			mBuilder.setLights(0xffffffff, 2000, 4000);
			final Notification notification = mBuilder.build();
			notificationManager.notify(NOTIFICATION_ID, notification);
		}
	}

	private Builder buildMultipleConversation() {
		final Builder mBuilder = new Builder(
				mXmppConnectionService);
		NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
		style.setBigContentTitle(notifications.size()
				+ " "
				+ mXmppConnectionService
				.getString(R.string.unread_conversations));
		final StringBuilder names = new StringBuilder();
		ir.bilgisoft.toopeto.entities.Conversation conversation = null;
		for (ArrayList<ir.bilgisoft.toopeto.entities.Message> messages : notifications.values()) {
			if (messages.size() > 0) {
				conversation = messages.get(0).getConversation();
				String name = conversation.getName();
				style.addLine(Html.fromHtml("<b>" + name + "</b> "
							+ getReadableBody(messages.get(0))));
				names.append(name);
				names.append(", ");
			}
		}
		if (names.length() >= 2) {
			names.delete(names.length() - 2, names.length());
		}
		mBuilder.setContentTitle(notifications.size()
				+ " "
				+ mXmppConnectionService
				.getString(R.string.unread_conversations));
		mBuilder.setContentText(names.toString());
		mBuilder.setStyle(style);
		if (conversation != null) {
			mBuilder.setContentIntent(createContentIntent(conversation
						.getUuid()));
		}
		return mBuilder;
	}

	private Builder buildSingleConversations(final boolean notify) {
		final Builder mBuilder = new Builder(
				mXmppConnectionService);
		final ArrayList<ir.bilgisoft.toopeto.entities.Message> messages = notifications.values().iterator().next();
		if (messages.size() >= 1) {
			final ir.bilgisoft.toopeto.entities.Conversation conversation = messages.get(0).getConversation();
			mBuilder.setLargeIcon(mXmppConnectionService.getAvatarService()
					.get(conversation, getPixel(64)));
			mBuilder.setContentTitle(conversation.getName());
			final ir.bilgisoft.toopeto.entities.Message message;
			if ((message = getImage(messages)) != null) {
				modifyForImage(mBuilder, message, messages, notify);
			} else {
				modifyForTextOnly(mBuilder, messages, notify);
			}
			mBuilder.setContentIntent(createContentIntent(conversation
						.getUuid()));
		}
		return mBuilder;
	}

	private void modifyForImage(final Builder builder, final ir.bilgisoft.toopeto.entities.Message message,
			final ArrayList<ir.bilgisoft.toopeto.entities.Message> messages, final boolean notify) {
		try {
			final Bitmap bitmap = mXmppConnectionService.getFileBackend()
				.getThumbnail(message, getPixel(288), false);
			final ArrayList<ir.bilgisoft.toopeto.entities.Message> tmp = new ArrayList<>();
			for (final ir.bilgisoft.toopeto.entities.Message msg : messages) {
				if (msg.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_TEXT
						&& msg.getDownloadable() == null) {
					tmp.add(msg);
						}
			}
			final BigPictureStyle bigPictureStyle = new BigPictureStyle();
			bigPictureStyle.bigPicture(bitmap);
			if (tmp.size() > 0) {
				bigPictureStyle.setSummaryText(getMergedBodies(tmp));
				builder.setContentText(getReadableBody(tmp.get(0)));
			} else {
				builder.setContentText(mXmppConnectionService.getString(R.string.image_file));
			}
			builder.setStyle(bigPictureStyle);
		} catch (final FileNotFoundException e) {
			modifyForTextOnly(builder, messages, notify);
		}
	}

	private void modifyForTextOnly(final Builder builder,
			final ArrayList<ir.bilgisoft.toopeto.entities.Message> messages, final boolean notify) {
		builder.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(getMergedBodies(messages)));
		builder.setContentText(getReadableBody(messages.get(0)));
		if (notify) {
			builder.setTicker(getReadableBody(messages.get(messages.size() - 1)));
		}
	}

	private ir.bilgisoft.toopeto.entities.Message getImage(final ArrayList<ir.bilgisoft.toopeto.entities.Message> messages) {
		for (final ir.bilgisoft.toopeto.entities.Message message : messages) {
			if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE
					&& message.getDownloadable() == null
					&& message.getEncryption() != ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
				return message;
					}
		}
		return null;
	}

	private String getMergedBodies(final ArrayList<ir.bilgisoft.toopeto.entities.Message> messages) {
		final StringBuilder text = new StringBuilder();
		for (int i = 0; i < messages.size(); ++i) {
			text.append(getReadableBody(messages.get(i)));
			if (i != messages.size() - 1) {
				text.append("\n");
			}
		}
		return text.toString();
	}

	private String getReadableBody(final ir.bilgisoft.toopeto.entities.Message message) {
		if (message.getDownloadable() != null
				&& (message.getDownloadable().getStatus() == ir.bilgisoft.toopeto.entities.Downloadable.STATUS_OFFER || message
					.getDownloadable().getStatus() == ir.bilgisoft.toopeto.entities.Downloadable.STATUS_OFFER_CHECK_FILESIZE)) {
			if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
				return mXmppConnectionService.getString(R.string.file_offered_for_download);
			} else {
				return mXmppConnectionService.getText(
						R.string.image_offered_for_download).toString();
			}
		} else if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
			return mXmppConnectionService.getText(
					R.string.encrypted_message_received).toString();
		} else if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTION_FAILED) {
			return mXmppConnectionService.getText(R.string.decryption_failed)
				.toString();
		} else if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE) {
			ir.bilgisoft.toopeto.entities.DownloadableFile file = mXmppConnectionService.getFileBackend().getFile(message);
			return mXmppConnectionService.getString(R.string.file,file.getMimeType());
		} else if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE) {
			return mXmppConnectionService.getText(R.string.image_file)
				.toString();
		} else {
			return message.getBody().trim();
		}
	}

	private PendingIntent createContentIntent(final String conversationUuid) {
		final TaskStackBuilder stackBuilder = TaskStackBuilder
			.create(mXmppConnectionService);
		stackBuilder.addParentStack(ir.bilgisoft.toopeto.ui.ConversationActivity.class);

		final Intent viewConversationIntent = new Intent(mXmppConnectionService,
				ir.bilgisoft.toopeto.ui.ConversationActivity.class);
		viewConversationIntent.setAction(Intent.ACTION_VIEW);
		if (conversationUuid != null) {
			viewConversationIntent.putExtra(ir.bilgisoft.toopeto.ui.ConversationActivity.CONVERSATION,
					conversationUuid);
			viewConversationIntent.setType(ir.bilgisoft.toopeto.ui.ConversationActivity.VIEW_CONVERSATION);
		}

		stackBuilder.addNextIntent(viewConversationIntent);

		return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent createDeleteIntent() {
		final Intent intent = new Intent(mXmppConnectionService,
				ir.bilgisoft.toopeto.services.XmppConnectionService.class);
		intent.setAction(ir.bilgisoft.toopeto.services.XmppConnectionService.ACTION_CLEAR_NOTIFICATION);
		return PendingIntent.getService(mXmppConnectionService, 0, intent, 0);
	}

	private PendingIntent createDisableForeground() {
		final Intent intent = new Intent(mXmppConnectionService,
				ir.bilgisoft.toopeto.services.XmppConnectionService.class);
		intent.setAction(ir.bilgisoft.toopeto.services.XmppConnectionService.ACTION_DISABLE_FOREGROUND);
		return PendingIntent.getService(mXmppConnectionService, 0, intent, 0);
	}

	private boolean wasHighlightedOrPrivate(final ir.bilgisoft.toopeto.entities.Message message) {
		final String nick = message.getConversation().getMucOptions().getActualNick();
		final Pattern highlight = generateNickHighlightPattern(nick);
		if (message.getBody() == null || nick == null) {
			return false;
		}
		final Matcher m = highlight.matcher(message.getBody());
		return (m.find() || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_PRIVATE);
	}

	private static Pattern generateNickHighlightPattern(final String nick) {
		// We expect a word boundary, i.e. space or start of string, followed by
		// the
		// nick (matched in case-insensitive manner), followed by optional
		// punctuation (for example "bob: i disagree" or "how are you alice?"),
		// followed by another word boundary.
		return Pattern.compile("\\b" + nick + "\\p{Punct}?\\b",
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	public void setOpenConversation(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
		this.mOpenConversation = conversation;
	}

	public void setIsInForeground(final boolean foreground) {
		if (foreground != this.mIsInForeground) {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,"setIsInForeground("+Boolean.toString(foreground)+")");
		}
		this.mIsInForeground = foreground;
	}

	private int getPixel(final int dp) {
		final DisplayMetrics metrics = mXmppConnectionService.getResources()
			.getDisplayMetrics();
		return ((int) (dp * metrics.density));
	}

	private void markLastNotification() {
		this.mLastNotification = SystemClock.elapsedRealtime();
	}

	private boolean inMiniGracePeriod(final ir.bilgisoft.toopeto.entities.Account account) {
		final int miniGrace = account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE ? ir.bilgisoft.toopeto.Config.MINI_GRACE_PERIOD
			: ir.bilgisoft.toopeto.Config.MINI_GRACE_PERIOD * 2;
		return SystemClock.elapsedRealtime() < (this.mLastNotification + miniGrace);
	}

	public Notification createForegroundNotification() {
		final Builder mBuilder = new Builder(mXmppConnectionService);
		mBuilder.setSmallIcon(R.drawable.ic_stat_communication_import_export);
		mBuilder.setContentTitle(mXmppConnectionService.getString(R.string.conversations_foreground_service));
		mBuilder.setContentText(mXmppConnectionService.getString(R.string.touch_to_disable));
		mBuilder.setContentIntent(createDisableForeground());
		mBuilder.setWhen(0);
		mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
		return mBuilder.build();
	}

	public void updateErrorNotification() {
		final NotificationManager mNotificationManager = (NotificationManager) mXmppConnectionService.getSystemService(Context.NOTIFICATION_SERVICE);
		final List<Account> errors = new ArrayList<>();
		for (final Account account : mXmppConnectionService.getAccounts()) {
			if (account.hasErrorStatus()) {
				errors.add(account);
			}
		}
		final Builder mBuilder = new Builder(mXmppConnectionService);
		if (errors.size() == 0) {
			mNotificationManager.cancel(ERROR_NOTIFICATION_ID);
			return;
		} else if (errors.size() == 1) {
			mBuilder.setContentTitle(mXmppConnectionService.getString(R.string.problem_connecting_to_account));
			mBuilder.setContentText(errors.get(0).getJid().toBareJid().toString());
		} else {
			mBuilder.setContentTitle(mXmppConnectionService.getString(R.string.problem_connecting_to_accounts));
			mBuilder.setContentText(mXmppConnectionService.getString(R.string.touch_to_fix));
		}
		mBuilder.setOngoing(true);
		mBuilder.setLights(0xffffffff, 2000, 4000);
		mBuilder.setSmallIcon(R.drawable.ic_stat_alert_warning);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(mXmppConnectionService);
		stackBuilder.addParentStack(ConversationActivity.class);

		final Intent manageAccountsIntent = new Intent(mXmppConnectionService,ManageAccountActivity.class);
		stackBuilder.addNextIntent(manageAccountsIntent);

		final PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(resultPendingIntent);
		mNotificationManager.notify(ERROR_NOTIFICATION_ID, mBuilder.build());
	}
}
