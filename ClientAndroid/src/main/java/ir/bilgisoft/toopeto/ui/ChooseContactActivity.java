package ir.bilgisoft.toopeto.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;

import java.util.Collections;

public class ChooseContactActivity extends ir.bilgisoft.toopeto.ui.AbstractSearchableListItemActivity {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent, final View view,
					final int position, final long id) {
				final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(getSearchEditText().getWindowToken(),
						InputMethodManager.HIDE_IMPLICIT_ONLY);
				final Intent request = getIntent();
				final Intent data = new Intent();
				final ir.bilgisoft.toopeto.entities.ListItem mListItem = getListItems().get(position);
				data.putExtra("contact", mListItem.getJid().toString());
				String account = request.getStringExtra("account");
				if (account == null && mListItem instanceof ir.bilgisoft.toopeto.entities.Contact) {
					account = ((ir.bilgisoft.toopeto.entities.Contact) mListItem).getAccount().getJid().toBareJid().toString();
				}
				data.putExtra("account", account);
				data.putExtra("conversation",
						request.getStringExtra("conversation"));
				setResult(RESULT_OK, data);
				finish();
			}
		});

	}

	protected void filterContacts(final String needle) {
		getListItems().clear();
		for (final ir.bilgisoft.toopeto.entities.Account account : xmppConnectionService.getAccounts()) {
			if (account.getStatus() != ir.bilgisoft.toopeto.entities.Account.State.DISABLED) {
				for (final ir.bilgisoft.toopeto.entities.Contact contact : account.getRoster().getContacts()) {
					if (contact.showInRoster() && contact.match(needle)) {
						getListItems().add(contact);
					}
				}
			}
		}
		Collections.sort(getListItems());
		getListItemAdapter().notifyDataSetChanged();
	}
}
