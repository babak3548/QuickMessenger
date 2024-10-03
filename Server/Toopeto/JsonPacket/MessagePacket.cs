
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using AnarSoft.Utility.Utilities;
namespace Toopeto.JsonPacket
{
/**
 * Created by mehrang on 03/03/2015.
 */
public class MessagePacket : Json {

       [DataMember]
    public string data;

    public MessagePacket():base()
    {
        
        this.packetName = PacketNameEnum.message.ToString();
       
    }

  public override string getString()  {
        return JsonSerializer.Serialize(this);
    }
  public static MessagePacket GetMessagePacket(string jsonString)
  {
      return JsonSerializer.DeSerialize<MessagePacket>(jsonString);
  }
  
}

}
