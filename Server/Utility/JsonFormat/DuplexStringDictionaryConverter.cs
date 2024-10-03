using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace AnarSoft.Utility.JsonFormat
{
    /// <summary>
    /// a class that converts between a JsonFromatString and A settingsDictionary 
    /// </summary> 
    public class DuplexStringDictionaryConverter 
    {
        /// <summary>
        /// an array of splitters, maybe added to this list in the future
        /// </summary>
        private string[] Splitters = { "=", ":" };

        /// <summary>
        /// the dictionary that is converted from JsonFormatString
        /// </summary>
        public Dictionary<string, object> ConvertionDictionary
        {
            get;
            set;
        }

        /// <summary>
        /// JsonFormatString that is built from settings dictionary
        /// </summary>
        public string StringValue
        {
            get;
            set;
        }

        public OptimizedDictionray Dictionary
        {
            get
            {
                return GetOptimaizedDictionary(ConvertionDictionary);
            }
        }

        private OptimizedDictionray GetOptimaizedDictionary(Dictionary<string, Object> datas)
        {
            OptimizedDictionray op = new OptimizedDictionray();
            if (datas != null)
            {
                foreach (var data in datas)
                {
                    if (data.Value.GetType() == typeof(string) && data.Value.ToString().Length > 0 && data.Value.ToString().First() == '{' && data.Value.ToString().Last() == '}')
                    {
                        try
                        {
                            DuplexStringDictionaryConverter convert = new DuplexStringDictionaryConverter(data.Value.ToString());
                            OptimizedDictionray op1 = GetOptimaizedDictionary(convert.ConvertionDictionary);
                            op1.StringValue = data.Value.ToString();
                            op1.Value = convert.ConvertionDictionary;

                            op.Add(data.Key, op1);

                        }
                        catch
                        {
                            op.Add(data.Key, new OptimizedDictionray() { StringValue = data.Value.ToString(), Value = data.Value });
                        }
                    }
                    else if (data.Value.GetType() == typeof(DuplexStringDictionaryConverter))
                    {
                        OptimizedDictionray op1 = GetOptimaizedDictionary(((DuplexStringDictionaryConverter)data.Value).ConvertionDictionary);
                        op1.StringValue = data.Value.ToString();
                        op1.Value = ((DuplexStringDictionaryConverter)data.Value).ConvertionDictionary;
                        op.Add(data.Key, op1);
                    }
                    else
                    {
                        op.Add(data.Key, new OptimizedDictionray() { StringValue = data.Value.ToString(), Value = data.Value });
                    }
                }
            }
            return op;
        }

        /// <summary>
        /// Creates a dictionary of string/object key value pairs from JsonFormatString
        /// </summary>
        public void CreateDictionary()
        {
            List<String> blocksList = StringBlockExtractor.GetBlocks(StringValue);
            if (BlockListIsNotNullOrEmpty(blocksList))
            {
                ConvertionDictionary = new Dictionary<string, object>();
                int index = 0;
                for (int i = 0; i < blocksList.Count; i++)
                {
                    String[] firstRestParts = StringBlockExtractor.FirstRestSplitter(blocksList[i], Splitters);
                    firstRestParts[1] = StringBlockExtractor.SplitterPlaceHolderCommaReplacer(firstRestParts[1]);
                    if (TheRestPartIsOfTypeString(firstRestParts))
                    {
                        DuplexStringDictionaryConverter sp = new DuplexStringDictionaryConverter(firstRestParts[1]);
                        if (sp.ConvertionDictionary == null)
                        {
                            if (FirstPartIsNotEmpty(firstRestParts))
                                ConvertionDictionary.Add(firstRestParts[0], StringBlockExtractor.ConvertObjectValueToItsNativeType(firstRestParts[1])); // Why Do We Convert One More Time?
                            else
                                ConvertionDictionary.Add("#Key" + index++, StringBlockExtractor.ConvertObjectValueToItsNativeType(firstRestParts[1]));// Why Do We Convert One More Time?
                        }
                        else
                        {
                            if (FirstPartIsNotEmpty(firstRestParts))
                                ConvertionDictionary.Add(firstRestParts[0], sp);
                            else
                                ConvertionDictionary.Add("#Key" + index++, sp);
                        }

                    }
                    else
                    {
                        if (FirstPartIsNotEmpty(firstRestParts))
                            ConvertionDictionary.Add(firstRestParts[0], StringBlockExtractor.ConvertObjectValueToItsNativeType(firstRestParts[1]));
                        else
                            ConvertionDictionary.Add("#Key" + index++, StringBlockExtractor.ConvertObjectValueToItsNativeType(firstRestParts[1]));
                    }
                }
            }
            else
                ConvertionDictionary = null;
        }

        #region Helper Methods
        private bool FirstPartIsNotEmpty(string[] elements)
        {
            return elements[0] != String.Empty;
        }

        private bool TheRestPartIsOfTypeString(string[] elements)
        {
            return StringBlockExtractor.ConvertObjectValueToItsNativeType(elements[1]).GetType() == typeof(string);
        }

        private bool BlockListIsNotNullOrEmpty(List<string> blocksList)
        {
            return blocksList != null && blocksList.Count > 0;
        }
        #endregion


        public void CreateString()
        {
            StringValue = CreateJsonFormatString(ConvertionDictionary);
        }

        #region Recursive Method to create a JsonFormatString from input settings dictionary
        public String CreateJsonFormatString(Dictionary<string, object> settingsDictionary)
        {
            if (settingsDictionary == null) { StringValue = string.Empty; return "{}"; }
            string result = string.Empty;
            foreach (string key in settingsDictionary.Keys)
            {
                try
                {
                    Dictionary<string, object> tempDic = (Dictionary<string, object>)settingsDictionary[key];
                    result +=
                        StringBlockExtractor.Splitters[0] +
                        DuplexStringDictionaryConverter.GetKey(key, Splitters[0]) +
                        CreateJsonFormatString(tempDic);
                }
                catch
                {
                    try
                    {
                        object[] subData = (object[])settingsDictionary[key];
                        string _result = "";
                        for (int i = 0; i < subData.Length; i++)
                        {
                            _result += StringBlockExtractor.Splitters[0] + subData[i].ToString();
                        }
                        result +=
                            StringBlockExtractor.Splitters[0] +
                            DuplexStringDictionaryConverter.GetKey(key, Splitters[0]) +
                            "{" + StringBlockExtractor.SplitterPlaceHolderReplacer(_result.Substring(1)) + "}";
                    }
                    catch
                    {
                        result +=
                        StringBlockExtractor.Splitters[0] +
                        DuplexStringDictionaryConverter.GetKey(key, Splitters[0]) +
                        StringBlockExtractor.SplitterPlaceHolderReplacer(settingsDictionary[key].ToString());
                    }

                }
            }
            if (result == string.Empty) return string.Empty;
            return "{" + StringBlockExtractor.SplitterPlaceHolderCommaReplacer(result.Substring(1)) + "}";
        }
        #endregion

        public DuplexStringDictionaryConverter()
        {
            ConvertionDictionary = new Dictionary<string, object>();
        }
        public DuplexStringDictionaryConverter(string script)
        {
            ConvertionDictionary = new Dictionary<string, object>();
            StringValue = script;
            CreateDictionary();
        }
        public DuplexStringDictionaryConverter(Dictionary<string, object> contents)
        {
            ConvertionDictionary = new Dictionary<string, object>();
            ConvertionDictionary = contents;
            CreateString();
        }

        public override string ToString()
        {
            return StringValue;
        }
        private static long EmptyKeyCounter = 0;
        public static string EmptyKey
        {
            get
            {
                EmptyKeyCounter++;
                return "#Key" + EmptyKeyCounter.ToString();
            }
        }
        private static string GetKey(String key, String Split)
        {
            if (key.Contains("#Key"))
                return string.Empty;
            return key + Split;
        }

        public string GetDictionaryItem(string key)
        {
            if (ConvertionDictionary != null && ConvertionDictionary.Keys.Contains(key))
                return ConvertionDictionary[key].ToString();
            return null;
        }

        public static string DecodeJsonFormat(string s)
        {
            return s
                 .Replace("<colon>", ":")
                 .Replace("<comma>", ",")
                .Replace("<equal>", "=")
                 .Replace("<semicolon>", ";")
                 .Replace("<SAccolade>", "{")
                 .Replace("<FAccolade>", "}");
        }

        public static string EncodeJsonFormat(string s)
        {
            return s
                  .Replace(":", "<colon>")
                  .Replace(",", "<comma>")
                  .Replace("=", "<equal>")
                  .Replace(";", "<semicolon>")
                  .Replace("{", "<SAccolade>")
                  .Replace("}", "<FAccolade>");
        }
    }
    public class OptimizedDictionray : Dictionary<string, OptimizedDictionray>
    {
        public string StringValue { get; set; }
        public object Value { get; set; }
    }
}
