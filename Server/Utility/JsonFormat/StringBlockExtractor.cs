using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace AnarSoft.Utility.JsonFormat
{
    /// <summary>
    /// a class that extracts Blocks "{{{},{{{}, {},{}}}}}" from a given string
    /// </summary>
    public class StringBlockExtractor
    {
        private static string _blockStartSymbol;
        private static string _blockEndSymbol;
        public StringBlockExtractor(string startSymbol, string endSymbol)
        {
            _blockStartSymbol = startSymbol;
            _blockEndSymbol = endSymbol;
        }

        // These 2 Properties Might changed , as user will give his optional value for them in constructor
        public static string BlockStartSymbol = "{";
        public static string BlockEndSymbol = "}";

        private const string BlockPlaceHolderSymbol = "<BLOCK>";
        private const string SplitterPlaceHolder = "<SplitterPlaceHolder>";
        private const string CommaPlaceHolder = "<CommaPlaceHolder>";
        private const string StartBracketPlaceHolder = "<StartBracket>";
        private const string EndBracketPlaceHolder = "<EndBracket>";

        public static string[] Splitters = { ",", ";" };

        /// <summary>
        /// replaces All occurences of splitters listed in Splitters array found in input string with SplitterPlaceHolder constant value
        /// </summary>
        /// <param name="inputString"></param>
        /// <returns></returns>
        public static String SplitterPlaceHolderReplacer(String inputString)
        {
            for (int i = 0; i < Splitters.Length; i++)
            {
                inputString = inputString.Replace(Splitters[i], SplitterPlaceHolder);
            }
            return inputString;
        }



        /// <summary>
        /// replaces a SplitterPlaceHolder with a comma in a given string
        /// </summary>
        /// <param name="main"></param>
        /// <returns></returns>
        public static String SplitterPlaceHolderCommaReplacer(String main)
        {
            return main.Replace(SplitterPlaceHolder, Splitters[0]);
        }


        /// <summary>
        /// replaces All occurences of splitters listed in Splitters array found in input string with SplitterPlaceHolder constant value
        /// </summary>
        /// <param name="inputString"></param>
        /// <returns></returns>
        public static String StringToCommaPlaceHolderReplacer(String inputString, string splitterChars)
        {
            inputString = inputString.Replace(splitterChars, CommaPlaceHolder);
            return inputString;
        }



        /// <summary>
        /// replaces a SplitterPlaceHolder with a comma in a given string
        /// </summary>
        /// <param name="main"></param>
        /// <returns></returns>
        public static String CommaPlaceHolderReplacer(String main, string splitterChars)
        {
            return main.Replace(CommaPlaceHolder, splitterChars);
        }


        /// <summary>
        /// replaces All occurences of splitters listed in Splitters array found in input string with SplitterPlaceHolder constant value
        /// </summary>
        /// <param name="inputString"></param>
        /// <returns></returns>
        public static String BracketReplacer(String inputString)
        {
            inputString = inputString.Replace(BlockStartSymbol, StartBracketPlaceHolder).Replace(BlockEndSymbol, EndBracketPlaceHolder);
            return inputString;
        }
        public static String BracketPlaceHolderReplacer(String inputString)
        {
            inputString = inputString.Replace(StartBracketPlaceHolder, BlockStartSymbol).Replace(EndBracketPlaceHolder, BlockEndSymbol);
            return inputString;
        }




        /// <summary>
        /// 
        /// </summary>
        /// <param name="inputString"></param>
        /// <returns></returns>
        public static List<string> GetBlocks(String inputString)
        {
            //already decoded
            if (inputString == null || inputString.Contains(BlockPlaceHolderSymbol))
                return null;

            List<string> blocks = new List<string>();

            // if string starts with with a block
            if (inputString.Trim().IndexOf(BlockStartSymbol) == 0)
            {
                String inputStringWithBlocksReplacedWithBlockPlaceHolders = String.Empty;
                String innerBlockContent = String.Empty;
                List<String> level0OriginalblocksList = new List<string>();
                //{1,2,{3}} => 1,2,{3}
                int level = 0;
                bool weAreInsideABlock = false;
                // do not count start end symbols in calculation <{> .................................... <}>
                // start from first char go to end
                //Extracts each inner block and saves it in a List
                for (int i = BlockStartSymbol.Length; i <= inputString.Length - (BlockEndSymbol.Length); i++)
                {
                    // if the immediate character is a block symbol
                    if (inputString.IndexOf(BlockStartSymbol, i) == i)
                    {
                        // we go into the block   <{> { now we are here} <}>
                        level++;
                        weAreInsideABlock = true;
                    }
                    // if we has not reached the end of string but we have reached to a EndBlockSymbol
                    if (i - BlockEndSymbol.Length + 1 > 0 && inputString.IndexOf(BlockEndSymbol, i - BlockEndSymbol.Length + 1) == i)
                    {
                        // we go out of inner block <{>{}<We are now here }>
                        level--;
                    }
                    // Save InnerBlock Content
                    if (weAreInsideABlock)
                        innerBlockContent += inputString[i]; //{
                    // if we are at the same level as input string most outter block
                    if (level == 0)
                    {
                        // if string had a block
                        if (weAreInsideABlock)
                        {
                            inputStringWithBlocksReplacedWithBlockPlaceHolders += BlockPlaceHolderSymbol; // builds the content after = sign with replaced symbol
                            weAreInsideABlock = false;
                            i += BlockEndSymbol.Length - 1; // Move position to the end of BlockSymbolString
                            level0OriginalblocksList.Add(innerBlockContent + BlockEndSymbol.Substring(1));// this will concatnate the ENDSYMBOL without its starting char
                            innerBlockContent = "";
                        }
                        else
                            // Saves the String till seeing the StartBlockSymbol -- > XYZSetting = 
                            inputStringWithBlocksReplacedWithBlockPlaceHolders += inputString[i];
                    }
                }

                int index = 0;
                String[] level0Blocks = inputStringWithBlocksReplacedWithBlockPlaceHolders.Split(Splitters, int.MaxValue, StringSplitOptions.RemoveEmptyEntries);
                // for every <BLOCK> symbol replace it with real content
                for (int i = 0; i < level0Blocks.Length; i++)
                {
                    if (level0Blocks[i].Contains(BlockPlaceHolderSymbol))
                    {
                        level0Blocks[i] = level0Blocks[i].Replace(BlockPlaceHolderSymbol, level0OriginalblocksList[index++]);
                    }
                    //???? We Do Not have Such Symbols YET
                    blocks.Add(level0Blocks[i].Replace("<<ScriptSplit1>>", Splitters[0]).Replace("<<ScriptSplit2>>", Splitters[1]));
                }

            }
            else
                return null;

            return blocks;
        }


        /// <summary>
        /// if the string value is convertible to a type return that native type
        /// </summary>
        /// <param name="strValue"></param>
        /// <returns></returns>
        public static Object ConvertObjectValueToItsNativeType(String strValue)
        {
            bool _bool = false;

            byte _byte = 0;
            int _int = 0;
            long _long = 0;

            float _float = 0.0f;
            double _double = 0.0;

            System.DateTime _DateTime = System.DateTime.Now;

            Guid _guid = new Guid();

            if (bool.TryParse(strValue, out _bool))
                return _bool;

            if (byte.TryParse(strValue, out _byte))
                return _byte;
            if (int.TryParse(strValue, out _int))
                return _int;
            if (long.TryParse(strValue, out _long))
                return _long;

            if (float.TryParse(strValue, out _float))
                return _float;
            if (double.TryParse(strValue, out _double))
                return _double;

            if (System.DateTime.TryParse(strValue, out _DateTime))
                return _DateTime;

            //if (Guid.TryParse(strValue, out _guid))
            //    return _guid;
            return strValue;
        }

        /// <summary>
        /// splits a string based on given splitters and returns the first portion along with the rest of the original string [1% to 99%]
        /// </summary>
        /// <param name="inputString"></param>
        /// <param name="splitters"></param>
        /// <returns></returns>
        public static String[] FirstRestSplitter(String inputString, String[] splitters)
        {
            String[] Result = new String[2];
            if (inputString.Length > 0 && inputString[0] == '{')
            {
                Result[0] = String.Empty;
                Result[1] = inputString;
                return Result;
            }
            try
            {
                Result[0] = inputString.Split(splitters, StringSplitOptions.None)[0];
                Result[1] = inputString.Substring(Result[0].Length + 1);
            }
            catch
            {
                Result[0] = String.Empty;
                Result[1] = inputString;
            }
            return Result;
        }
    }
}