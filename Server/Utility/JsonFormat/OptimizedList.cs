using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace AnarSoft.Utility.JsonFormat
{
    /// <summary>
    /// represents a List of divided items based on block definition we have
    /// </summary>
    public class OptimizedList : List<OptimizedList>
    {
        /// <summary>
        /// default .ctor
        /// </summary>
        public OptimizedList()
        {
        }
        /// <summary>
        /// takes a string and convert it into a list of blocks based on our BLOCK definition
        /// </summary>
        /// <param name="stringValue"></param>
        public OptimizedList(string stringValue)
        {
            this.StringValue = stringValue;
        }
        /// <summary>
        /// 
        /// </summary>
        private string _stringValue;
        /// <summary>
        /// keeps splitting chars which are important to us
        /// </summary>
        private string[] _splitters = { "=", ":" };

        /// <summary>
        /// Creates a list of blocks based on our definition on what a block is
        /// </summary>
        public void CreateList(string stringValue)
        {
            // keeps 
            List<String> blocksList = StringBlockExtractor.GetBlocks(stringValue);

            this.Clear();

            if (BlockListIsNotNullOrEmpty(blocksList))
            {
                for (int i = 0; i < blocksList.Count; i++)
                {
                    String[] firstRestParts = StringBlockExtractor.FirstRestSplitter(blocksList[i], _splitters);
                    firstRestParts[1] = StringBlockExtractor.SplitterPlaceHolderCommaReplacer(firstRestParts[1]);


                    if (FirstPartIsNotEmpty(firstRestParts))
                        this.Add(new OptimizedList() { StringKey = firstRestParts[0], StringValue = firstRestParts[1] });
                    else
                        this.Add(new OptimizedList() { StringValue = firstRestParts[1] });

                }
            }
        }

        #region Helper Methods
        private bool FirstPartIsNotEmpty(string[] elements)
        {
            return elements[0] != String.Empty;
        }

        private bool BlockListIsNotNullOrEmpty(List<string> blocksList)
        {
            return blocksList != null && blocksList.Count > 0;
        }
        #endregion

        public void CreateString()
        {
            StringValue = CreateJsonFormatString(this);
        }

        #region Recursive Method to create a JsonFormatString from input settings dictionary
        public String CreateJsonFormatString(List<OptimizedList> list)
        {
            if (list.Count == 0) { StringValue = string.Empty; return "{}"; }
            string result = string.Empty;

            foreach (var item in list)
            {
                result +=
                       StringBlockExtractor.Splitters[0];
                if (!string.IsNullOrEmpty(item.StringKey))
                    result += item.StringKey + _splitters[0];
                if (item.Count > 0)
                {
                    result += CreateJsonFormatString(item);
                }
                else
                {
                    result += item.StringValue;
                }
            }
            if (result == string.Empty) return string.Empty;
            return "{" + StringBlockExtractor.SplitterPlaceHolderCommaReplacer(result.Substring(1)) + "}";
        }
        #endregion

        public OptimizedList GetItemByKey(string key)
        {
            return this.Where(x => x.StringKey.Trim() == key).FirstOrDefault();
        }

        public override string ToString()
        {
            return StringValue;
        }
        /// <summary>
        /// represents the string that will be converted into blocks list
        /// the block list creation is involved in setting this property
        /// </summary>
        public string StringValue
        {
            get
            {
                try { if (string.IsNullOrEmpty(_stringValue)) CreateString(); }
                catch { }
                return _stringValue;
            }
            set
            {
                _stringValue = value;
                CreateList(_stringValue);
            }
        }
        public string StringKey { get; set; }
    }
}
