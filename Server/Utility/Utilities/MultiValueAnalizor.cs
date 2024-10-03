using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace AnarSoft.Utility.Utilities
{
   public  class MultiValueAnalizor
    {
        /// <summary>
        /// اگر پارامتر اول در پارمتر دوم وجود داشت مقدار درست را بر میگرداند
        /// </summary>
        /// <param name="value">پارامتر اول  </param>
        /// <param name="MultiValue">پارمتر دوم  </param>
        /// <returns></returns>
        public static bool ValueIsMultiValue(long value, long MultiValue)
        {
            List<long> power2 = new List<long>();
            List<long> resultAccessEnum = new List<long>();

            Power2List(MultiValue, power2);
            AnalizeAccessEnums(MultiValue, power2, resultAccessEnum);

            return IfHaveAccessSetTrue(resultAccessEnum, value);
        }

        private static bool IfHaveAccessSetTrue(List<long> resultAccessEnum, long accessEnumValue)
        {
            foreach (var item in resultAccessEnum)
            {
                if (item == accessEnumValue)
                {
                    return true;
                }
            }

            return false;
        }
        private static void AnalizeAccessEnums(long AccessEnums, List<long> power2, List<long> resultAccessEnum)
        {
            power2.Reverse();
            foreach (var item in power2)
            {
                if (item <= AccessEnums)
                {
                    AccessEnums -= item;
                    resultAccessEnum.Add(item);

                }
            }

        }
        static List<long> pow2 = new List<long> { 2048, 1024, 512, 256,128,64, 32,16 ,8 ,4 , 2 };

        public static void AnalizeAccessEnums(long AccessEnums, List<long> resultAccessEnum)
        {
           
            foreach (var item in pow2)
            {
                if (item <= AccessEnums)
                {
                    AccessEnums -= item;
                    resultAccessEnum.Add(item);

                }
            }
            resultAccessEnum.Reverse();
        }
        private static void Power2List(long AccessEnums, List<long> power2)
        {
            long x = 2;
            while (x <= AccessEnums)
            {
                power2.Add(x);
                x *= 2;
            }
        }
    }
}
