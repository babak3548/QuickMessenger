using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.Serialization;
using System.IO;
using System.Xml;
using System.Xml.Serialization;
using System.Web.Configuration;


namespace AnarSoft.Utility.Utilities
{
   

    public class XmlSerializerUtility
    {
        
        public static string Serialize(object graph)
        {
           
            DataContractSerializer ser = new DataContractSerializer(graph.GetType());
            StringWriter sw = new StringWriter();
            XmlTextWriter tw = new System.Xml.XmlTextWriter(sw);
            ser.WriteObject(tw, graph);
            string str = sw.ToString();
            tw.Close();
            sw.Close();
            return str;
        }

        public static T DeSerialize<T>(string graph) where T : class
        {
            DataContractSerializer ser = new DataContractSerializer(typeof(T));
            StringReader sw = new StringReader(graph);
            XmlTextReader tw = new System.Xml.XmlTextReader(sw);
            var obj = ser.ReadObject(tw);

            tw.Close();
            sw.Close();
            return obj as T;
        }
    }
}
