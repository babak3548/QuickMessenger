using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.Text;
using AnarSoft.Utility.Utilities;


namespace Toopeto.JsonPacket
{
    /**
     * Created by mehrang on 02/03/2015.
     */
    [DataContract]
    public  class Json
    {
        public Json()
        {
          //  this.from = Setting.fromServerToopeto;بهتر نال باشه تا خطای منطقی پیش نیاد
        }

        [DataMember]
        public string id;
        [DataMember]
        public string from;
        [DataMember]
        public string password;
        [DataMember]
        public string to;
        [DataMember]
        public string packetName;
        [DataMember]
        public string type;
        [DataMember]
        public string groupName;
         [DataMember]
        public string date;
         [DataMember]
         public string description;
         [DataMember]
         public string groupType;

        public virtual string getString()
        {
          return  JsonSerializer.Serialize(this);
        }
        public static  Json GetJson(string jsonString)
        {
            return JsonSerializer.DeSerialize<Json>(jsonString);
        }

    }

}
