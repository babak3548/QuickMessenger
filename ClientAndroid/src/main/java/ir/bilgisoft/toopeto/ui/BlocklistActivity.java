package ir.bilgisoft.toopeto.ui;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;

import java.util.Collections;

public class BlocklistActivity extends ir.bilgisoft.toopeto.ui.AbstractSearchableListItemActivity implements ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist {

	private ir.bilgisoft.toopeto.entities.Account account = null;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(final AdapterView<?> parent,
					final View view,
					final int position,
					final long id) {
				ir.bilgisoft.toopeto.ui.BlockContactDialog.show(parent.getContext(), xmppConnectionService, (ir.bilgisoft.toopeto.entities.Contact) getListItems().get(position));
				return true;
			}
		});
	}

	@Override
	public void onBackendConnected() {
		for (final ir.bilgisoft.toopeto.entities.Account account : xmppConnectionService.getAccounts()) {
			if (account.getJid().toString().equals(getIntent().getStringExtra("account"))) {
				this.account = account;
				break;
			}
		}
		filterContacts();
	}

	@Override
	protected void filterContacts(final String needle) {
		getListItems().clear();
		if (account != null) {
			for (final ir.bilgisoft.toopeto.xmpp.jid.Jid jid : account.getBlocklist()) {
				final ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(jid.getLocalpart());
				if (contact.match(needle) && contact.isBlocked()) {
					getListItems().add(contact);
				}
			}
			Collections.sort(getListItems());
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getListItemAdapter().notifyDataSetChanged();
			}
		});
	}

	@Override
	public void OnUpdateBlocklist(final Status status) {
		final Editable editable = getSearchEditText().getText();
		if (editable != null) {
			filterContacts(editable.toString());
		} else {
			filterContacts();
		}
	}
}
