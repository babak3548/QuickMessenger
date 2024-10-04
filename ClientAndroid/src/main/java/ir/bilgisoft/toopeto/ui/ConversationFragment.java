package ir.bilgisoft.toopeto.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import net.java.otr4j.session.SessionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

import ir.bilgisoft.toopeto.R;
import ir.bilgisoft.toopeto.ui.EditMessage.OnEnterPressed;
import ir.bilgisoft.toopeto.ui.XmppActivity.OnPresenceSelected;
import ir.bilgisoft.toopeto.ui.XmppActivity.OnValueEdited;
import ir.bilgisoft.toopeto.ui.adapter.MessageAdapter.OnContactPictureClicked;
import ir.bilgisoft.toopeto.ui.adapter.MessageAdapter.OnContactPictureLongClicked;
import ir.bilgisoft.toopeto.entities.Contact;
import ir.bilgisoft.toopeto.entities.Conversation;
import ir.bilgisoft.toopeto.entities.Message;
import ir.bilgisoft.toopeto.entities.Presences;
import ir.bilgisoft.toopeto.services.XmppConnectionService;

public class ConversationFragment extends Fragment {

	protected ir.bilgisoft.toopeto.entities.Conversation conversation;
	private OnClickListener leaveMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.endConversation(conversation);
		}
	};
	private OnClickListener joinMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.xmppConnectionService.joinMuc(conversation);
		}
	};
	private OnClickListener enterPassword = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ir.bilgisoft.toopeto.entities.MucOptions muc = conversation.getMucOptions();
			String password = muc.getPassword();
			if (password == null) {
				password = "";
			}
			activity.quickPasswordEdit(password, new OnValueEdited() {

				@Override
				public void onValueEdited(String value) {
					activity.xmppConnectionService.providePasswordForMuc(
							conversation, value);
				}
			});
		}
	};
	protected ListView messagesView;
	final protected List<ir.bilgisoft.toopeto.entities.Message> messageList = new ArrayList<>();
	protected ir.bilgisoft.toopeto.ui.adapter.MessageAdapter messageListAdapter;
	protected ir.bilgisoft.toopeto.entities.Contact contact;
	private ir.bilgisoft.toopeto.ui.EditMessage mEditMessage;
	private ImageButton mSendButton;
	private RelativeLayout snackbar;
	private TextView snackbarMessage;
	private TextView snackbarAction;
	private boolean messagesLoaded = true;
	private Toast messageLoaderToast;
///////////////////////my code ////////////////////////////
	private OnScrollListener mOnScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			synchronized (ConversationFragment.this.messageList) {
				if (firstVisibleItem < 5 && messagesLoaded && messageList.size() > 0) {
					long timestamp = ConversationFragment.this.messageList.get(0).getTimeSent();
					messagesLoaded = false;
					activity.xmppConnectionService.loadMoreMessages(conversation, timestamp, new ir.bilgisoft.toopeto.services.XmppConnectionService.OnMoreMessagesLoaded() {
						@Override
						public void onMoreMessagesLoaded(final int count, ir.bilgisoft.toopeto.entities.Conversation conversation) {
							if (ConversationFragment.this.conversation != conversation) {
								return;
							}
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									final int oldPosition = messagesView.getFirstVisiblePosition();
									View v = messagesView.getChildAt(0);
									final int pxOffset = (v == null) ? 0 : v.getTop();
									ConversationFragment.this.conversation.populateWithMessages(ConversationFragment.this.messageList);
									updateStatusMessages();
									messageListAdapter.notifyDataSetChanged();
									if (count != 0) {
										final int newPosition = oldPosition + count;
										int offset = 0;
										try {
											Message tmpMessage = messageList.get(newPosition);

											while(tmpMessage.wasMergedIntoPrevious()) {
												offset++;
												tmpMessage = tmpMessage.prev();
											}
										} catch (final IndexOutOfBoundsException ignored) {

										}
										messagesView.setSelectionFromTop(newPosition - offset, pxOffset);
										messagesLoaded = true;
										if (messageLoaderToast != null) {
											messageLoaderToast.cancel();
										}
									}
								}
							});
						}

						@Override
						public void informUser(final int resId) {

							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (messageLoaderToast != null) {
										messageLoaderToast.cancel();
									}
									if (ConversationFragment.this.conversation != conversation) {
										return;
									}
									messageLoaderToast = Toast.makeText(activity,resId,Toast.LENGTH_LONG);
									messageLoaderToast.show();
								}
							});

						}
					});

				}
			}
		}
	};

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_conversation,
                container, false);
        mEditMessage = (EditMessage) view.findViewById(R.id.textinput);
        mEditMessage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                activity.hideConversationsOverview();
            }
        });
        mEditMessage.setOnEditorActionListener(mEditorActionListener);
        mEditMessage.setOnEnterPressedListener(new OnEnterPressed() {

            @Override
            public void onEnterPressed() {
                sendMessage();
            }
        });

        mSendButton = (ImageButton) view.findViewById(R.id.textSendButton);
        mSendButton.setOnClickListener(this.mSendButtonListener);

        snackbar = (RelativeLayout) view.findViewById(R.id.snackbar);
        snackbarMessage = (TextView) view.findViewById(R.id.snackbar_message);
        snackbarAction = (TextView) view.findViewById(R.id.snackbar_action);

        messagesView = (ListView) view.findViewById(R.id.messages_view);
        messagesView.setOnScrollListener(mOnScrollListener);

        messagesView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        messageListAdapter = new ir.bilgisoft.toopeto.ui.adapter.MessageAdapter((ir.bilgisoft.toopeto.ui.ConversationActivity) getActivity(), this.messageList);
        messageListAdapter.setOnContactPictureClicked(new OnContactPictureClicked() {

            @Override
            public void onContactPictureClicked(ir.bilgisoft.toopeto.entities.Message message) {
                if (message.getStatus() <= ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED) {
                    if (message.getConversation().getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
                        if (message.getCounterpart() != null) {
                            if (!message.getCounterpart().isBareJid()) {
                                highlightInConference(message.getCounterpart().getResourcepart());
                            } else {
                                highlightInConference(message.getCounterpart().toString());
                            }
                        }
                    } else {
                        ir.bilgisoft.toopeto.entities.Contact contact = message.getConversation()
                                .getContact();
                        if (contact.showInRoster()) {
                            activity.switchToContactDetails(contact);
                        } else {
                            activity.showAddToRosterDialog(message
                                    .getConversation());
                        }
                    }
                } else {
                    ir.bilgisoft.toopeto.entities.Account account = message.getConversation().getAccount();
                    Intent intent = new Intent(activity, ir.bilgisoft.toopeto.ui.EditAccountActivity.class);
                    intent.putExtra("jid", account.getJid().toBareJid().toString());
                    startActivity(intent);
                }
            }
        });
        messageListAdapter
                .setOnContactPictureLongClicked(new OnContactPictureLongClicked() {

                    @Override
                    public void onContactPictureLongClicked(ir.bilgisoft.toopeto.entities.Message message) {
                        if (message.getStatus() <= ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED) {
                            if (message.getConversation().getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
                                if (message.getCounterpart() != null) {
                                    privateMessageWith(message.getCounterpart());
                                }
                            }
                        } else {
                            activity.showQrCode();
                        }
                    }
                });
        messagesView.setAdapter(messageListAdapter);

        registerForContextMenu(messagesView);

        return view;
    }

    /////////////////end code ////////////////////////////

	private IntentSender askForPassphraseIntent = null;
	protected OnClickListener clickToDecryptListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (activity.hasPgp() && askForPassphraseIntent != null) {
				try {
					getActivity().startIntentSenderForResult(
							askForPassphraseIntent,
							ir.bilgisoft.toopeto.ui.ConversationActivity.REQUEST_DECRYPT_PGP, null, 0,
							0, 0);
				} catch (SendIntentException e) {
					//
				}
			}
		}
	};
	protected OnClickListener clickToVerify = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.verifyOtrSessionDialog(conversation,v);
		}
	};
	private ConcurrentLinkedQueue<ir.bilgisoft.toopeto.entities.Message> mEncryptedMessages = new ConcurrentLinkedQueue<>();
	private boolean mDecryptJobRunning = false;
	private OnEditorActionListener mEditorActionListener = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEND) {
				InputMethodManager imm = (InputMethodManager) v.getContext()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				sendMessage();
				return true;
			} else {
				return false;
			}
		}
	};
	private OnClickListener mSendButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			sendMessage();
		}
	};
	private OnClickListener clickToMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getActivity(),
					ir.bilgisoft.toopeto.ui.ConferenceDetailsActivity.class);
			intent.setAction(ir.bilgisoft.toopeto.ui.ConferenceDetailsActivity.ACTION_VIEW_MUC);
			intent.putExtra("uuid", conversation.getUuid());
			startActivity(intent);
		}
	};
	private ConversationActivity activity;
	private Message selectedMessage;

	private void sendMessage() {
		if (this.conversation == null) {
			return;
		}
		if (mEditMessage.getText().length() < 1) {
			if (this.conversation.getMode() == Conversation.MODE_MULTI) {
				conversation.setNextCounterpart(null);
				updateChatMsgHint();
			}
			return;
		}
		Message message = new Message(conversation, mEditMessage.getText()
				.toString(), conversation.getNextEncryption(activity
					.forceEncryption()));
		if (conversation.getMode() == Conversation.MODE_MULTI) {
			if (conversation.getNextCounterpart() != null) {
				message.setCounterpart(conversation.getNextCounterpart());
				message.setType(Message.TYPE_PRIVATE);
				conversation.setNextCounterpart(null);
			}
		}
		if (conversation.getNextEncryption(activity.forceEncryption()) == Message.ENCRYPTION_OTR) {
			sendOtrMessage(message);
		} else if (conversation.getNextEncryption(activity.forceEncryption()) == Message.ENCRYPTION_PGP) {
			sendPgpMessage(message);
		} else {
			sendPlainTextMessage(message);
		}
	}

	public void updateChatMsgHint() {
		if (conversation.getMode() == Conversation.MODE_MULTI
				&& conversation.getNextCounterpart() != null) {
			this.mEditMessage.setHint(getString(
						R.string.send_private_message_to,
						conversation.getNextCounterpart().getResourcepart()));
		} else {
			switch (conversation.getNextEncryption(activity.forceEncryption())) {
				case Message.ENCRYPTION_NONE:
					mEditMessage
						.setHint(getString(R.string.send_plain_text_message));
					break;
				case Message.ENCRYPTION_OTR:
					mEditMessage.setHint(getString(R.string.send_otr_message));
					break;
				case Message.ENCRYPTION_PGP:
					mEditMessage.setHint(getString(R.string.send_pgp_message));
					break;
				default:
					break;
			}
		}
	}



	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		synchronized (this.messageList) {
			super.onCreateContextMenu(menu, v, menuInfo);
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
			this.selectedMessage = this.messageList.get(acmi.position);
			populateContextMenu(menu);
		}
	}

	private void populateContextMenu(ContextMenu menu) {
		final ir.bilgisoft.toopeto.entities.Message m = this.selectedMessage;
		if (m.getType() != ir.bilgisoft.toopeto.entities.Message.TYPE_STATUS) {
			activity.getMenuInflater().inflate(R.menu.message_context, menu);
			menu.setHeaderTitle(R.string.message_options);
			MenuItem copyText = menu.findItem(R.id.copy_text);
			MenuItem shareImage = menu.findItem(R.id.share_image);
			MenuItem sendAgain = menu.findItem(R.id.send_again);
			MenuItem copyUrl = menu.findItem(R.id.copy_url);
			MenuItem downloadImage = menu.findItem(R.id.download_image);
			MenuItem cancelTransmission = menu.findItem(R.id.cancel_transmission);
			if (m.getType() != ir.bilgisoft.toopeto.entities.Message.TYPE_TEXT || m.getDownloadable() != null) {
				copyText.setVisible(false);
			}
			if (m.getType() != ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE || m.getDownloadable() != null) {
				shareImage.setVisible(false);
			}
			if (m.getStatus() != ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_FAILED) {
				sendAgain.setVisible(false);
			}
			if ((m.getType() != ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE && m.getDownloadable() == null)
					|| m.getImageParams().url == null) {
				copyUrl.setVisible(false);
			}
			if (m.getType() != ir.bilgisoft.toopeto.entities.Message.TYPE_TEXT
					|| m.getDownloadable() != null
					|| !m.bodyContainsDownloadable()) {
				downloadImage.setVisible(false);
			}
			if (!((m.getDownloadable() != null && !(m.getDownloadable() instanceof ir.bilgisoft.toopeto.entities.DownloadablePlaceholder))
					|| (m.isFileOrImage() && m.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_WAITING))) {
				cancelTransmission.setVisible(false);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.share_image:
				shareImage(selectedMessage);
				return true;
			case R.id.copy_text:
				copyText(selectedMessage);
				return true;
			case R.id.send_again:
				resendMessage(selectedMessage);
				return true;
			case R.id.copy_url:
				copyUrl(selectedMessage);
				return true;
			case R.id.download_image:
				downloadImage(selectedMessage);
				return true;
			case R.id.cancel_transmission:
				cancelTransmission(selectedMessage);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	private void shareImage(ir.bilgisoft.toopeto.entities.Message message) {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM,
				activity.xmppConnectionService.getFileBackend()
				.getJingleFileUri(message));
		shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		shareIntent.setType("image/webp");
		activity.startActivity(Intent.createChooser(shareIntent,
					getText(R.string.share_with)));
	}

	private void copyText(ir.bilgisoft.toopeto.entities.Message message) {
		if (activity.copyTextToClipboard(message.getMergedBody(),
					R.string.message_text)) {
			Toast.makeText(activity, R.string.message_copied_to_clipboard,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void resendMessage(ir.bilgisoft.toopeto.entities.Message message) {
		if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_FILE || message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_IMAGE) {
			ir.bilgisoft.toopeto.entities.DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
			if (!file.exists()) {
				Toast.makeText(activity,R.string.file_deleted,Toast.LENGTH_SHORT).show();
				message.setDownloadable(new ir.bilgisoft.toopeto.entities.DownloadablePlaceholder(ir.bilgisoft.toopeto.entities.Downloadable.STATUS_DELETED));
				return;
			}
		}
		activity.xmppConnectionService.resendFailedMessages(message);
	}

	private void copyUrl(ir.bilgisoft.toopeto.entities.Message message) {
		if (activity.copyTextToClipboard(
					message.getImageParams().url.toString(), R.string.image_url)) {
			Toast.makeText(activity, R.string.url_copied_to_clipboard,
					Toast.LENGTH_SHORT).show();
					}
	}

	private void downloadImage(ir.bilgisoft.toopeto.entities.Message message) {
		activity.xmppConnectionService.getHttpConnectionManager()
			.createNewConnection(message);
	}

	private void cancelTransmission(ir.bilgisoft.toopeto.entities.Message message) {
		ir.bilgisoft.toopeto.entities.Downloadable downloadable = message.getDownloadable();
		if (downloadable!=null) {
			downloadable.cancel();
		} else {
			activity.xmppConnectionService.markMessage(message, ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_FAILED);
		}
	}

	protected void privateMessageWith(final ir.bilgisoft.toopeto.xmpp.jid.Jid counterpart) {
		this.mEditMessage.setText("");
		this.conversation.setNextCounterpart(counterpart);
		updateChatMsgHint();
	}

	protected void highlightInConference(String nick) {
		String oldString = mEditMessage.getText().toString().trim();
		if (oldString.isEmpty() || mEditMessage.getSelectionStart() == 0) {
			mEditMessage.getText().insert(0, nick + ": ");
		} else {
			if (mEditMessage.getText().charAt(
						mEditMessage.getSelectionStart() - 1) != ' ') {
				nick = " " + nick;
						}
			mEditMessage.getText().insert(mEditMessage.getSelectionStart(),
					nick + " ");
		}
	}

	@Override
	public void onStop() {
		mDecryptJobRunning = false;
		super.onStop();
		if (this.conversation != null) {
			this.conversation.setNextMessage(mEditMessage.getText().toString());
		}
	}

	public void reInit(ir.bilgisoft.toopeto.entities.Conversation conversation) {
		if (conversation == null) {
			return;
		}
		if (this.conversation != null) {
			this.conversation.setNextMessage(mEditMessage.getText().toString());
			this.conversation.trim();
		}
		this.activity = (ir.bilgisoft.toopeto.ui.ConversationActivity) getActivity();
		this.conversation = conversation;
		this.mDecryptJobRunning = false;
		this.mEncryptedMessages.clear();
		if (this.conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI) {
			this.conversation.setNextCounterpart(null);
		}
		this.mEditMessage.setText("");
		this.mEditMessage.append(this.conversation.getNextMessage());
		this.messagesView.invalidateViews();
		updateMessages();
		this.messagesLoaded = true;
		int size = this.messageList.size();
		if (size > 0) {
			messagesView.setSelection(size - 1);
		}
	}

	public void updateMessages() {
		synchronized (this.messageList) {
			if (getView() == null) {
				return;
			}
			hideSnackbar();
			final ir.bilgisoft.toopeto.ui.ConversationActivity activity = (ir.bilgisoft.toopeto.ui.ConversationActivity) getActivity();
			if (this.conversation != null) {
				final ir.bilgisoft.toopeto.entities.Contact contact = this.conversation.getContact();
				if (this.conversation.isBlocked()) {
					showSnackbar(R.string.contact_blocked, R.string.unblock,
							new OnClickListener() {
								@Override
								public void onClick(final View v) {
									v.post(new Runnable() {
										@Override
										public void run() {
											v.setVisibility(View.INVISIBLE);
										}
									});
									if (conversation.isDomainBlocked()) {
										ir.bilgisoft.toopeto.ui.BlockContactDialog.show(getActivity(), ((ir.bilgisoft.toopeto.ui.ConversationActivity) getActivity()).xmppConnectionService, conversation);
									} else {
										((ir.bilgisoft.toopeto.ui.ConversationActivity) getActivity()).unblockConversation(conversation);
									}
								}
							});
				} else if (this.conversation.isMuted()) {
					showSnackbar(R.string.notifications_disabled, R.string.enable,
							new OnClickListener() {

								@Override
								public void onClick(final View v) {
									activity.unmuteConversation(conversation);
								}
							});
				} else if (!contact.showInRoster()
						&& contact
						.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
					showSnackbar(R.string.contact_added_you, R.string.add_back,
							new OnClickListener() {

								@Override
								public void onClick(View v) {
									activity.xmppConnectionService
										.createContact(contact);
									activity.switchToContactDetails(contact);
								}
							});
				} else if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE) {
					makeFingerprintWarning();
				} else if (!conversation.getMucOptions().online()
						&& conversation.getAccount().getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
					int error = conversation.getMucOptions().getError();
					switch (error) {
						case ir.bilgisoft.toopeto.entities.MucOptions.ERROR_NICK_IN_USE:
							showSnackbar(R.string.nick_in_use, R.string.edit,
									clickToMuc);
							break;
						case ir.bilgisoft.toopeto.entities.MucOptions.ERROR_UNKNOWN:
							showSnackbar(R.string.conference_not_found,
									R.string.leave, leaveMuc);
							break;
						case ir.bilgisoft.toopeto.entities.MucOptions.ERROR_PASSWORD_REQUIRED:
							showSnackbar(R.string.conference_requires_password,
									R.string.enter_password, enterPassword);
							break;
						case ir.bilgisoft.toopeto.entities.MucOptions.ERROR_BANNED:
							showSnackbar(R.string.conference_banned,
									R.string.leave, leaveMuc);
							break;
						case ir.bilgisoft.toopeto.entities.MucOptions.ERROR_MEMBERS_ONLY:
							showSnackbar(R.string.conference_members_only,
									R.string.leave, leaveMuc);
							break;
						case ir.bilgisoft.toopeto.entities.MucOptions.KICKED_FROM_ROOM:
							showSnackbar(R.string.conference_kicked, R.string.join,
									joinMuc);
							break;
						default:
							break;
					}
						}
				conversation.populateWithMessages(ConversationFragment.this.messageList);
				for (ir.bilgisoft.toopeto.entities.Message message : this.messageList) {
					if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP
							&& (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED || message
								.getStatus() >= ir.bilgisoft.toopeto.entities.Message.STATUS_SEND)
							&& message.getDownloadable() == null) {
						if (!mEncryptedMessages.contains(message)) {
							mEncryptedMessages.add(message);
						}
							}
				}
				decryptNext();
				updateStatusMessages();
				this.messageListAdapter.notifyDataSetChanged();
				updateChatMsgHint();
				if (!activity.isConversationsOverviewVisable() || !activity.isConversationsOverviewHideable()) {
					activity.sendReadMarkerIfNecessary(conversation);
				}
				this.updateSendButton();
			}
		}
	}

	private void decryptNext() {
		ir.bilgisoft.toopeto.entities.Message next = this.mEncryptedMessages.peek();
		ir.bilgisoft.toopeto.crypto.PgpEngine engine = activity.xmppConnectionService.getPgpEngine();

		if (next != null && engine != null && !mDecryptJobRunning) {
			mDecryptJobRunning = true;
			engine.decrypt(next, new UiCallback<ir.bilgisoft.toopeto.entities.Message>() {

				@Override
				public void userInputRequried(PendingIntent pi, ir.bilgisoft.toopeto.entities.Message message) {
					mDecryptJobRunning = false;
					askForPassphraseIntent = pi.getIntentSender();
					showSnackbar(R.string.openpgp_messages_found,
							R.string.decrypt, clickToDecryptListener);
				}

				@Override
				public void success(ir.bilgisoft.toopeto.entities.Message message) {
					mDecryptJobRunning = false;
					try {
						mEncryptedMessages.remove();
					} catch (final NoSuchElementException ignored) {

					}
					activity.xmppConnectionService.updateMessage(message);
				}

				@Override
				public void error(int error, ir.bilgisoft.toopeto.entities.Message message) {
					message.setEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTION_FAILED);
					mDecryptJobRunning = false;
					try {
						mEncryptedMessages.remove();
					} catch (final NoSuchElementException ignored) {

					}
					activity.xmppConnectionService.updateConversationUi();
				}
			});
		}
	}

	private void messageSent() {
		int size = this.messageList.size();
		messagesView.setSelection(size - 1);
		mEditMessage.setText("");
		updateChatMsgHint();
	}

	public void updateSendButton() {
		ir.bilgisoft.toopeto.entities.Conversation c = this.conversation;
		if (activity.useSendButtonToIndicateStatus() && c != null
				&& c.getAccount().getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
			if (c.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE) {
				switch (c.getContact().getMostAvailableStatus()) {
					case ir.bilgisoft.toopeto.entities.Presences.CHAT:
						this.mSendButton
							.setImageResource(R.drawable.ic_action_send_now_online);
						break;
					case Presences.ONLINE:
						this.mSendButton
							.setImageResource(R.drawable.ic_action_send_now_online);
						break;
					case Presences.AWAY:
						this.mSendButton
							.setImageResource(R.drawable.ic_action_send_now_away);
						break;
					case Presences.XA:
						this.mSendButton
							.setImageResource(R.drawable.ic_action_send_now_away);
						break;
					case Presences.DND:
						this.mSendButton
							.setImageResource(R.drawable.ic_action_send_now_dnd);
						break;
					default:
						this.mSendButton
							.setImageResource(R.drawable.ic_action_send_now_offline);
						break;
				}
			} else if (c.getMode() == Conversation.MODE_MULTI) {
				if (c.getMucOptions().online()) {
					this.mSendButton
						.setImageResource(R.drawable.ic_action_send_now_online);
				} else {
					this.mSendButton
						.setImageResource(R.drawable.ic_action_send_now_offline);
				}
			} else {
				this.mSendButton
					.setImageResource(R.drawable.ic_action_send_now_offline);
			}
		} else {
			this.mSendButton
				.setImageResource(R.drawable.ic_action_send_now_offline);
		}
	}

	protected void updateStatusMessages() {
		synchronized (this.messageList) {
			if (conversation.getMode() == Conversation.MODE_SINGLE) {
				for (int i = this.messageList.size() - 1; i >= 0; --i) {
					if (this.messageList.get(i).getStatus() == Message.STATUS_RECEIVED) {
						return;
					} else {
						if (this.messageList.get(i).getStatus() == Message.STATUS_SEND_DISPLAYED) {
							this.messageList.add(i + 1, Message.createStatusMessage(conversation));
							return;
						}
					}
				}
			}
		}
	}

	protected void makeFingerprintWarning() {
		if (conversation.smpRequested()) {
			showSnackbar(R.string.smp_requested, R.string.verify, new OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent intent = new Intent(activity, VerifyOTRActivity.class);
					intent.setAction(VerifyOTRActivity.ACTION_VERIFY_CONTACT);
					intent.putExtra("contact", conversation.getContact().getJid().toBareJid().toString());
					intent.putExtra("account", conversation.getAccount().getJid().toBareJid().toString());
					intent.putExtra("mode", VerifyOTRActivity.MODE_ANSWER_QUESTION);
					startActivity(intent);
				}
			});
		} else if (conversation.hasValidOtrSession() && (conversation.getOtrSession().getSessionStatus() == SessionStatus.ENCRYPTED)
				&& (!conversation.isOtrFingerprintVerified())) {
			showSnackbar(R.string.unknown_otr_fingerprint, R.string.verify, clickToVerify);
				}
	}

	protected void showSnackbar(final int message, final int action,
			final OnClickListener clickListener) {
		snackbar.setVisibility(View.VISIBLE);
		snackbar.setOnClickListener(null);
		snackbarMessage.setText(message);
		snackbarMessage.setOnClickListener(null);
		snackbarAction.setVisibility(View.VISIBLE);
		snackbarAction.setText(action);
		snackbarAction.setOnClickListener(clickListener);
	}

	protected void hideSnackbar() {
		snackbar.setVisibility(View.GONE);
	}

	protected void sendPlainTextMessage(Message message) {
		ConversationActivity activity = (ConversationActivity) getActivity();
		activity.xmppConnectionService.sendMessage(message);
		messageSent();
	}

	protected void sendPgpMessage(final Message message) {
		final ConversationActivity activity = (ConversationActivity) getActivity();
		final XmppConnectionService xmppService = activity.xmppConnectionService;
		final Contact contact = message.getConversation().getContact();
		if (activity.hasPgp()) {
			if (conversation.getMode() == Conversation.MODE_SINGLE) {
				if (contact.getPgpKeyId() != 0) {
					xmppService.getPgpEngine().hasKey(contact,
							new UiCallback<Contact>() {

								@Override
								public void userInputRequried(PendingIntent pi,
										Contact contact) {
									activity.runIntent(
											pi,
											ConversationActivity.REQUEST_ENCRYPT_MESSAGE);
								}

								@Override
								public void success(Contact contact) {
									messageSent();
									activity.encryptTextMessage(message);
								}

								@Override
								public void error(int error, Contact contact) {

								}
							});

				} else {
					showNoPGPKeyDialog(false,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									conversation
										.setNextEncryption(Message.ENCRYPTION_NONE);
									xmppService.databaseBackend
										.updateConversation(conversation);
									message.setEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE);
									xmppService.sendMessage(message);
									messageSent();
								}
							});
				}
			} else {
				if (conversation.getMucOptions().pgpKeysInUse()) {
					if (!conversation.getMucOptions().everybodyHasKeys()) {
						Toast warning = Toast
							.makeText(getActivity(),
									R.string.missing_public_keys,
									Toast.LENGTH_LONG);
						warning.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
						warning.show();
					}
					activity.encryptTextMessage(message);
					messageSent();
				} else {
					showNoPGPKeyDialog(true,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									conversation
										.setNextEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE);
									message.setEncryption(ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE);
									xmppService.databaseBackend
										.updateConversation(conversation);
									xmppService.sendMessage(message);
									messageSent();
								}
							});
				}
			}
		} else {
			activity.showInstallPgpDialog();
		}
	}

	public void showNoPGPKeyDialog(boolean plural,
			DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		if (plural) {
			builder.setTitle(getString(R.string.no_pgp_keys));
			builder.setMessage(getText(R.string.contacts_have_no_pgp_keys));
		} else {
			builder.setTitle(getString(R.string.no_pgp_key));
			builder.setMessage(getText(R.string.contact_has_no_pgp_key));
		}
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.send_unencrypted),
				listener);
		builder.create().show();
	}

	protected void sendOtrMessage(final ir.bilgisoft.toopeto.entities.Message message) {
		final ir.bilgisoft.toopeto.ui.ConversationActivity activity = (ir.bilgisoft.toopeto.ui.ConversationActivity) getActivity();
		final ir.bilgisoft.toopeto.services.XmppConnectionService xmppService = activity.xmppConnectionService;
		activity.selectPresence(message.getConversation(),
				new OnPresenceSelected() {
					@Override
					public void onPresenceSelected() {
						message.setCounterpart(conversation.getNextCounterpart());
						xmppService.sendMessage(message);
						messageSent();
					}
				});
	}

	public void appendText(String text) {
		String previous = this.mEditMessage.getText().toString();
		if (previous.length() != 0 && !previous.endsWith(" ")) {
			text = " " + text;
		}
		this.mEditMessage.append(text);
	}

	public void clearInputField() {
		this.mEditMessage.setText("");
	}
}
