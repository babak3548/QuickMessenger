package ir.bilgisoft.toopeto.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import ir.bilgisoft.toopeto.R;
import ir.bilgisoft.toopeto.entities.Account;
import ir.bilgisoft.toopeto.xmpp.jid.Jid;

public class PublishProfilePictureActivity extends ir.bilgisoft.toopeto.ui.XmppActivity {

	private static final int REQUEST_CHOOSE_FILE = 0xac23;

	private ImageView avatar;
	private TextView accountTextView;
	private TextView hintOrWarning;
	private TextView secondaryHint;
	private Button cancelButton;
	private Button publishButton;

	private Uri avatarUri;
	private Uri defaultUri;

	private ir.bilgisoft.toopeto.entities.Account account;

	private boolean support = false;

	private boolean mInitialAccountSetup;

	private ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.xmpp.pep.Avatar> avatarPublication = new ir.bilgisoft.toopeto.ui.UiCallback<ir.bilgisoft.toopeto.xmpp.pep.Avatar>() {

		@Override
		public void success(ir.bilgisoft.toopeto.xmpp.pep.Avatar object) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mInitialAccountSetup) {
						startActivity(new Intent(getApplicationContext(),
								ir.bilgisoft.toopeto.ui.StartConversationActivity.class));
					}
					finish();
				}
			});
		}

		@Override
		public void error(final int errorCode, ir.bilgisoft.toopeto.xmpp.pep.Avatar object) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					hintOrWarning.setText(errorCode);
					hintOrWarning.setTextColor(getWarningTextColor());
					publishButton.setText(R.string.publish);
					enablePublishButton();
				}
			});

		}

		@Override
		public void userInputRequried(PendingIntent pi, ir.bilgisoft.toopeto.xmpp.pep.Avatar object) {
		}
	};

	private OnLongClickListener backToDefaultListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			avatarUri = defaultUri;
			loadImageIntoPreview(defaultUri);
			return true;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_publish_profile_picture);
		this.avatar = (ImageView) findViewById(R.id.account_image);
		this.cancelButton = (Button) findViewById(R.id.cancel_button);
		this.publishButton = (Button) findViewById(R.id.publish_button);
		this.accountTextView = (TextView) findViewById(R.id.account);
		this.hintOrWarning = (TextView) findViewById(R.id.hint_or_warning);
		this.secondaryHint = (TextView) findViewById(R.id.secondary_hint);
		this.publishButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (avatarUri != null) {
					publishButton.setText(R.string.publishing);
					disablePublishButton();
					xmppConnectionService.publishAvatar(account, avatarUri,
							avatarPublication);
				}
			}
		});
		this.cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mInitialAccountSetup) {
					startActivity(new Intent(getApplicationContext(),
							ir.bilgisoft.toopeto.ui.StartConversationActivity.class));
				}
				finish();
			}
		});
		this.avatar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent attachFileIntent = new Intent();
				attachFileIntent.setType("image/*");
				attachFileIntent.setAction(Intent.ACTION_GET_CONTENT);
				Intent chooser = Intent.createChooser(attachFileIntent,
						getString(R.string.attach_file));
				startActivityForResult(chooser, REQUEST_CHOOSE_FILE);
			}
		});
		this.defaultUri = ir.bilgisoft.toopeto.utils.PhoneHelper.getSefliUri(getApplicationContext());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_CHOOSE_FILE) {
				this.avatarUri = data.getData();
				if (xmppConnectionServiceBound) {
					loadImageIntoPreview(this.avatarUri);
				}
			}
		}
	}

	@Override
	protected void onBackendConnected() {
		if (getIntent() != null) {
            ir.bilgisoft.toopeto.xmpp.jid.Jid jid;
            try {
                jid = ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(getIntent().getStringExtra("account"));
            } catch (ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
                jid = null;
            }
            if (jid != null) {
				this.account = xmppConnectionService.findAccountByJid(jid);
				if (this.account.getXmppConnection() != null) {
					this.support = this.account.getXmppConnection()
							.getFeatures().pubsub();
				}
				if (this.avatarUri == null) {
					if (this.account.getAvatar() != null
							|| this.defaultUri == null) {
						this.avatar.setImageBitmap(avatarService().get(account,
								getPixel(194)));
						if (this.defaultUri != null) {
							this.avatar
									.setOnLongClickListener(this.backToDefaultListener);
						} else {
							this.secondaryHint.setVisibility(View.INVISIBLE);
						}
						if (!support) {
							this.hintOrWarning
									.setTextColor(getWarningTextColor());
							this.hintOrWarning
									.setText(R.string.error_publish_avatar_no_server_support);
						}
					} else {
						this.avatarUri = this.defaultUri;
						loadImageIntoPreview(this.defaultUri);
						this.secondaryHint.setVisibility(View.INVISIBLE);
					}
				} else {
					loadImageIntoPreview(avatarUri);
				}
				this.accountTextView.setText(this.account.getJid().toBareJid().toString().replaceAll(Jid.constantDomainpart,""));
			}
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (getIntent() != null) {
			this.mInitialAccountSetup = getIntent().getBooleanExtra("setup",
					false);
		}
		if (this.mInitialAccountSetup) {
			this.cancelButton.setText(R.string.skip);
		}
	}

	protected void loadImageIntoPreview(Uri uri) {
		Bitmap bm = xmppConnectionService.getFileBackend().cropCenterSquare(
				uri, 384);
		if (bm == null) {
			disablePublishButton();
			this.hintOrWarning.setTextColor(getWarningTextColor());
			this.hintOrWarning
					.setText(R.string.error_publish_avatar_converting);
			return;
		}
		this.avatar.setImageBitmap(bm);
		if (support) {
			enablePublishButton();
			this.publishButton.setText(R.string.publish);
			this.hintOrWarning.setText(R.string.publish_avatar_explanation);
			this.hintOrWarning.setTextColor(getPrimaryTextColor());
		} else {
			disablePublishButton();
			this.hintOrWarning.setTextColor(getWarningTextColor());
			this.hintOrWarning
					.setText(R.string.error_publish_avatar_no_server_support);
		}
		if (this.defaultUri != null && uri.equals(this.defaultUri)) {
			this.secondaryHint.setVisibility(View.INVISIBLE);
			this.avatar.setOnLongClickListener(null);
		} else if (this.defaultUri != null) {
			this.secondaryHint.setVisibility(View.VISIBLE);
			this.avatar.setOnLongClickListener(this.backToDefaultListener);
		}
	}

	protected void enablePublishButton() {
		this.publishButton.setEnabled(true);
		this.publishButton.setTextColor(getPrimaryTextColor());
	}

	protected void disablePublishButton() {
		this.publishButton.setEnabled(false);
		this.publishButton.setTextColor(getSecondaryTextColor());
	}

}
