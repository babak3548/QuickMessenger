package ir.bilgisoft.toopeto.json;

import android.util.EventLogTags;

import com.google.gson.Gson;

import org.json.JSONException;

import java.util.UUID;

import ir.bilgisoft.toopeto.entities.Account;
import ir.bilgisoft.toopeto.json.Enums.PacketNameEnum;

/**
 * Created by mehrang on 02/03/2015.
 */
public  class Json {

Json()
{
    this.id= UUID.randomUUID().toString();
    this.from=Account.jid.getLocalpart();
}

    public String id;

    public String from;

    public String to;

    public String packetName;

    public String type;

    public String receiver;

    public String date;

    public String Description;

    public  String getString()
    {
        Gson gson=new Gson();
        return gson.toJson(this);
    }
    public static  Json GetJson(String jsonString)
    {
        Gson gson = new Gson();
        Json message=gson.fromJson(jsonString,Json.class);
        return  message;
    }


}


