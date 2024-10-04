package ir.bilgisoft.toopeto.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.PowerManager.WakeLock;
import android.util.JsonReader;
import android.util.Log;
import android.util.Xml;

public class XmlReader {
	private XmlPullParser parser;
	private WakeLock wakeLock;
	private InputStream is;
    private String line;
    private StringBuilder builder =new StringBuilder();
    private  BufferedReader bReader;




	public void setInputStream(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			throw new IOException();
		}
		this.is = inputStream;
		try {

     //       InputStreamReader  x=  new InputStreamReader(this.is);

		//	parser.setInput(new InputStreamReader(this.is));
            bReader = new BufferedReader(new InputStreamReader(this.is));
		} catch (Exception e) {
			throw new IOException("error resetting parser");
		}
	}

	public InputStream getInputStream() throws IOException {
		if (this.is == null) {
			throw new IOException();
		}
		return is;
	}

	public void reset() throws IOException {
		if (this.is == null) {
			throw new IOException();
		}
		try {
			parser.setInput(new InputStreamReader(this.is));
		} catch (XmlPullParserException e) {
			throw new IOException("error resetting parser");
		}
	}


    public XmlReader(WakeLock wakeLock) {
       // org.xmlpull.v1.XmlPullParser.

        this.parser = Xml.newPullParser();
        try {
            this.parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                    true);
        } catch (XmlPullParserException e) {
            Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "error setting namespace feature on parser");
        }
        this.wakeLock = wakeLock;
    }
	public String readTag() throws XmlPullParserException, IOException {
		if (wakeLock.isHeld()) {
			try {
				wakeLock.release();
			} catch (RuntimeException re) {
			}
		}
		try {
             line = null;

            if(bReader.ready()) {
                line=bReader.readLine();
              // char[] x= new char[bReader.read()];
              //  bReader .read(x);
              //  line=x.toString();
            }
           // while ((line = bReader.readLine()) != null) {
            if (wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (RuntimeException re) {
                }
            }
          return  line;


/*
			while (this.is != null
					&& parser.next() != XmlPullParser.END_DOCUMENT) {
				wakeLock.acquire();

				if (parser.getEventType() == XmlPullParser.START_TAG) {
					ir.bilgisoft.toopeto.xml.Tag tag = ir.bilgisoft.toopeto.xml.Tag.start(parser.getName());
                  //  Log.d("reciveXML1",tag.toString());//babak
					for (int i = 0; i < parser.getAttributeCount(); ++i) {
						tag.setAttribute(parser.getAttributeName(i),
								parser.getAttributeValue(i));
					}
					String xmlns = parser.getNamespace();
					if (xmlns != null) {
						tag.setAttribute("xmlns", xmlns);
					}
                  //  Log.d("reciveXML2",tag.toString());//babak
					return tag;
				} else if (parser.getEventType() == XmlPullParser.END_TAG) {
					ir.bilgisoft.toopeto.xml.Tag tag = ir.bilgisoft.toopeto.xml.Tag.end(parser.getName());
                  //  Log.d("reciveXML3",tag.toString());//babak
					return tag;
				} else if (parser.getEventType() == XmlPullParser.TEXT) {
					ir.bilgisoft.toopeto.xml.Tag tag = ir.bilgisoft.toopeto.xml.Tag.no(parser.getText());
                //  Log.d("reciveXML4",tag.toString());//babak
                    return tag;
				}
                else {
                    Log.d("error lost data:",parser.getText());//babak
                }
			} */

		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IOException(
					"xml parser mishandled ArrayIndexOufOfBounds", e);
		} catch (StringIndexOutOfBoundsException e) {
			throw new IOException(
					"xml parser mishandled StringIndexOufOfBounds", e);
		} catch (NullPointerException e) {
			throw new IOException("xml parser mishandled NullPointerException",
					e);
		} catch (IndexOutOfBoundsException e) {
			throw new IOException("xml parser mishandled IndexOutOfBound", e);
		}
        catch (Exception e)
        {
            Log.d( "error","unhandled");
        }
		return null;
	}
//bayad baresi shava ke in method ba greftan tage jari chi tahvil midahad
    /*
	public ir.bilgisoft.toopeto.xml.Element readElement(ir.bilgisoft.toopeto.xml.Tag currentTag) throws XmlPullParserException,
			IOException {
		ir.bilgisoft.toopeto.xml.Element element = new ir.bilgisoft.toopeto.xml.Element(currentTag.getName());
		element.setAttributes(currentTag.getAttributes());
		ir.bilgisoft.toopeto.xml.Tag nextTag = this.readTag();
		if (nextTag == null) {
			throw new IOException("unterupted mid tag");
		}
		if (nextTag.isNo()) {
			element.setContent(nextTag.getName());
			nextTag = this.readTag();
			if (nextTag == null) {
				throw new IOException("unterupted mid tag");
			}
		}
		while (!nextTag.isEnd(element.getName())) {
			if (!nextTag.isNo()) {
				ir.bilgisoft.toopeto.xml.Element child = this.readElement(nextTag);
				element.addChild(child);
			}
			nextTag = this.readTag();
			if (nextTag == null) {
				throw new IOException("unterupted mid tag");
			}
		}

		return element;
	}
    */
}
