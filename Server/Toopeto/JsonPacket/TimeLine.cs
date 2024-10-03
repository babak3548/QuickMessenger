using AnarSoft.Utility.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Toopeto.JsonPacket
{
  [DataContract]
   public class TimeLine
    {
        [DataMember]
        public string message;
        [DataMember]
        public int countLike;

        public TimeLine(string message, int countLike)
        {
            this.message = message;
            this.countLike = countLike;
        }
        public  string getString()
        {
            return JsonSerializer.Serialize(this);
        }
        public static TimeLine GetTimeLinePacket(string JsonPacket)
        {
            return JsonSerializer.DeSerialize<TimeLine>(JsonPacket);
        }
    }
}
