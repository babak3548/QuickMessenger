package ir.bilgisoft.toopeto.json;

import com.google.gson.Gson;

import java.util.List;

/**
 * Created by mehrang on 20/03/2015.
 */
public class ListTransferPacket extends  Json{
    public List<String> list;
    public int count;
    public int currentNumber;

    public ListTransferPacket()
    {
        super();
        this.packetName =Enums.PacketNameEnum.listTransfer.toString();
    }

    @Override
    public  String getString()
    {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public static ListTransferPacket GetListTransferPacket(String JsonPacket)
    {
        Gson gson = new Gson();
        ListTransferPacket lp= gson.fromJson(JsonPacket,ListTransferPacket.class);
        return lp;
    }

}
