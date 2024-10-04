package ir.bilgisoft.toopeto.xml;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MyOutputStreamWriter extends  OutputStreamWriter {

    public MyOutputStreamWriter(OutputStream out) {
        super(out);
    }

    @Override
    public void write(String str) throws IOException {
        //Log.d("sendXML",str);
        super.write(str+'\n');
    }
}
