using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using AnarSoft.Utility.Utilities;

namespace Toopeto.JsonPacket
{
    public class UserPacket : Json
    {
        [DataMember]
        public string userName;

        [DataMember]
        public string Signature;
        [DataMember]
        public string avatar;

        public UserPacket():base()
        {
            this.packetName = PacketNameEnum.user.ToString();
          
        }
        public override string getString()
        {
            return JsonSerializer.Serialize(this);
        }
        public static UserPacket GetUserPacket(string JsonPacket)
        {
            return JsonSerializer.DeSerialize<UserPacket>(JsonPacket);
        }

    }
}
