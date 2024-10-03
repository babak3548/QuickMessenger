using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.IO;

namespace AnarSoft.Utility.Utilities
{
    public static class JsonSerializer
    {
        public static string Serialize(object graph)
        {
            MemoryStream stream = new MemoryStream();
            DataContractJsonSerializer ser = new DataContractJsonSerializer(graph.GetType());
            ser.WriteObject(stream, graph);
            stream.Position = 0;
            StreamReader sr = new StreamReader(stream);
            string str = sr.ReadToEnd();
            stream.Close();
            sr.Close();
            return str;
            //  DataContractSerializer ser = new DataContractSerializer(graph.GetType());
            //  StringWriter sw = new StringWriter();
            //  XmlTextWriter tw = new System.Xml.XmlTextWriter(sw);
            //  ser.WriteObject(tw, graph);
            // string str = sw.ToString();
            // tw.Close();
            //  sw.Close();
            // return str;
        }

        public static T DeSerialize<T>(string graph) where T : class
        {
            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(T));
            MemoryStream stream = new MemoryStream(Encoding.UTF8.GetBytes(graph));
            stream.Position = 0;
            Object obj = ser.ReadObject(stream);

            stream.Close();
            return obj as T;
        }
    }
}
