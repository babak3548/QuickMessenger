package ir.bilgisoft.toopeto.http;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HttpConnectionManager extends ir.bilgisoft.toopeto.services.AbstractConnectionManager {

	public HttpConnectionManager(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}

	private List<ir.bilgisoft.toopeto.http.HttpConnection> connections = new CopyOnWriteArrayList<ir.bilgisoft.toopeto.http.HttpConnection>();

	public ir.bilgisoft.toopeto.http.HttpConnection createNewConnection(ir.bilgisoft.toopeto.entities.Message message) {
		ir.bilgisoft.toopeto.http.HttpConnection connection = new ir.bilgisoft.toopeto.http.HttpConnection(this);
		connection.init(message);
		this.connections.add(connection);
		return connection;
	}

	public void finishConnection(ir.bilgisoft.toopeto.http.HttpConnection connection) {
		this.connections.remove(connection);
	}
}
