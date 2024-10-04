package ir.bilgisoft.toopeto.json;

import org.json.JSONException;
import com.google.gson.Gson;

import ir.bilgisoft.toopeto.entities.Account;

/**
 * Created by mehrang on 03/03/2015.
 */
public class MessagePacket extends Json {


    public String data;

    public MessagePacket()
    {
        super();
        this.packetName =Enums.PacketNameEnum.message.toString();
    }




    @Override
    public String getString() {
        Gson gson=new Gson();
        return gson.toJson(this);
    }


    public static MessagePacket GetMessagePacket(String stringJson)  {
        Gson gson = new Gson();
        MessagePacket message=gson.fromJson(stringJson,MessagePacket.class);
        return  message;

    }


}
