package ir.bilgisoft.toopeto.json;

import org.json.JSONException;

import com.google.gson.Gson;

import java.util.UUID;

import ir.bilgisoft.toopeto.entities.Account;

/**
 * Created by mehrang on 03/03/2015.
 */
public class UserPacket extends Json {

    public String userName;

    public String password;

    public String Signature;

    public UserPacket()
    {
        super();
        this.packetName =Enums.PacketNameEnum.user.toString();

    }

    @Override
    public  String getString()
    {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public static UserPacket GetUserPacket(String JsonPacket)
    {
        Gson gson = new Gson();
        UserPacket up= gson.fromJson(JsonPacket,UserPacket.class);
        return  up;
    }



}
