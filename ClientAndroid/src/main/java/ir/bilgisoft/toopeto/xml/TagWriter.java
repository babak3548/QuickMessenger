package ir.bilgisoft.toopeto.xml;


import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.MessagePacket;
import ir.bilgisoft.toopeto.xmpp.stanzas.AbstractStanza;

import ir.bilgisoft.toopeto.json.Json;

public class TagWriter {

	private OutputStream plainOutputStream;
	private MyOutputStreamWriter outputStream;
	private boolean finshed = false;
	private LinkedBlockingQueue<AbstractStanza> writeQueueOld = new LinkedBlockingQueue<AbstractStanza>();
	LinkedBlockingQueue<Json> writeQueue = new LinkedBlockingQueue<Json>();
	private Thread asyncStanzaWriter = new Thread() {
		private boolean shouldStop = false;
		@Override
		public void run() {
			while (!shouldStop) {
				if ((finshed) && (writeQueue.size() == 0)) {
					return;
				}
				try {
					Json output = writeQueue.take();

					if (outputStream == null) {
						shouldStop = true;
					} else {
						outputStream.write(output.getString());
						outputStream.flush();
					}
				} catch (IOException e) {
					shouldStop = true;
                    Log.d("outputStream IO error :",e.getMessage());

                } catch (InterruptedException e) {
					shouldStop = true;
                    Log.d("outputStream Interrupted error :",e.getMessage());
                }
                catch (Exception e)
                {
                    shouldStop=true;
                    Log.d("outputStream unknown error :",e.getMessage());

                }
			}
		}
	};

	public TagWriter() {
	}

	public void setOutputStream(OutputStream out) throws IOException {
		if (out == null) {
			throw new IOException();
		}
		this.plainOutputStream = out;
		this.outputStream = new MyOutputStreamWriter(out);
	}

	public OutputStream getOutputStream() throws IOException {
		if (this.plainOutputStream == null) {
			throw new IOException();
		}
		return this.plainOutputStream;
	}

	public TagWriter beginDocument() throws IOException {
		if (outputStream == null) {
			throw new IOException("output stream was null");
		}
        MessagePacket messagePacket =new MessagePacket();
        messagePacket.type= Enums.MessageTypeEnum.ping.toString();
		outputStream.write(messagePacket.getString());
		outputStream.flush();
		return this;
	}

	public TagWriter writeTag(Json json) throws IOException {
		if (outputStream == null) {
			throw new IOException("output stream was null");
		}
       /// Log.d("SendXMLwriteTag",tag.toString());//babak
		outputStream.write(json.getString());
		outputStream.flush();
		return this;
	}

	public TagWriter writeElement(Element element) throws IOException {
		if (outputStream == null) {
			throw new IOException("output stream was null");
		}
     //   Log.d("SendXMLwriteElement",element.toString());//babak
		outputStream.write(element.toString());
		outputStream.flush();
		return this;
	}

	public TagWriter writeStanzaAsync(Json stanza) {
		if (finshed) {
			return this;
		} else {
			if (!asyncStanzaWriter.isAlive()) {
				try {
					asyncStanzaWriter.start();
				} catch (IllegalThreadStateException e) {
					// already started
				}
			}
			writeQueue.add(stanza);
			return this;
		}
	}

	public void finish() {
		this.finshed = true;
	}

	public boolean finished() {
		return (this.writeQueueOld.size() == 0);
	}

	public boolean isActive() {
		return outputStream != null;
	}
}
