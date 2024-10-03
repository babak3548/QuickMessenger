using AnarSoft.Utility.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;

namespace Toopeto.JsonPacket
{
    public class ListTransferPacket : Json
    {
        [DataMember]
        public List<string> list;

        [DataMember]
        public int count;

        [DataMember]
        public int currentNumber;

        public ListTransferPacket()
            : base()
        {
            this.packetName = PacketNameEnum.listTransfer.ToString();
        }

        public override string getString()
        {
            return JsonSerializer.Serialize(this);
        }
        public static ListTransferPacket GetListTransferPacket(string jsonString)
        {
            return JsonSerializer.DeSerialize<ListTransferPacket>(jsonString);
        }

    }
}
