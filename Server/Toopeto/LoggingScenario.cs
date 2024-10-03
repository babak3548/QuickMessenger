using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Toopeto.JsonPacket;

namespace Toopeto
{
    public static class LoggingScenario
    {

        static List<Model> loggingScenarioDic = new List<Model>();

        private static Object thisLock = new Object();
        internal static void loging(Json json)
        {
            try
            {
                string keyTemp = json.packetName + "_" + json.type;
                foreach (Model model in loggingScenarioDic)
                {
                    if (model.key == keyTemp)
                    {
                        model.setValue(model.getValue() + 1);
                        saveLog(keyTemp, model);
                        keyTemp = "";
                        break;
                    }
                }
                if (keyTemp != "")
                {
                    loggingScenarioDic.Add(new Model { key = keyTemp });
                }
            }
            catch (Exception e)
            {
                System.IO.File.AppendAllText("ExceptionLogging.txt", DateTime.Now + " # " + e.Message +"# stack: "+e.StackTrace  + " \r\n");
            }

        }
        static void saveLog(string scenarioName, Model model)
        {
            if (model.getValue() > 200)//int.MaxValue - 1)
            {
                lock (thisLock)
                {
                    System.IO.File.AppendAllText("scenarioCount" + getDate() + ".txt",
                        DateTime.Now + scenarioName + " : " + model.getValue()
                       + " loggingScenarioDic count:" + loggingScenarioDic.Count + " \r\n");
                    model.setValue(0);
                }
            }
        }
        public static string getDate()
        {
            return DateTime.Now.Year + "_" + DateTime.Now.Month + "_" + DateTime.Now.Day;
        }
    }
    public class Model
    {
        private  Object thisLock = new Object();
        public string key = "";
        private int value = 0;
        public void setValue(int value) {
            lock (thisLock)
            {
                this.value = value;
            }
        }
        public int getValue() {
            return this.value;
        }
    }


}
