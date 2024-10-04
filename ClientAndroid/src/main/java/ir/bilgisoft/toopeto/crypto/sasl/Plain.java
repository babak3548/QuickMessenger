package ir.bilgisoft.toopeto.crypto.sasl;

import android.util.Base64;

import java.nio.charset.Charset;

public class Plain extends ir.bilgisoft.toopeto.crypto.sasl.SaslMechanism {
	public Plain(final ir.bilgisoft.toopeto.xml.TagWriter tagWriter, final ir.bilgisoft.toopeto.entities.Account account) {
		super(tagWriter, account, null);
	}

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public String getMechanism() {
		return "PLAIN";
	}

	@Override
	public String getClientFirstMessage() {
		final String sasl = '\u0000' + account.getUsername() + '\u0000' + account.getPassword();
		return Base64.encodeToString(sasl.getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
	}
}
