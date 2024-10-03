using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using AnarSoft.Utility.JsonFormat;

namespace AnarSoft.Utility.Utilities
{
    public class ObjectManager
    {
        public ObjectManager()
        {
        }
        public ObjectManager(object value, object defaultValue)
        {
            Value = value;
            DefaultValue = defaultValue;
        }

        public object Value { get; set; }
        public object DefaultValue { get; set; }

        public bool BooleanValue
        {
            get
            {
                var value = GetCorrectValue();

                bool convertedValue = false;
                if (bool.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return false;
            }
        }

        public byte ByteValue
        {
            get
            {
                var value = GetCorrectValue();

                byte convertedValue = 0;
                if (byte.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return 0;
            }
        }

        public int IntegerValue
        {
            get
            {
                var value = GetCorrectValue();

                int convertedValue = 0;
                if (int.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return 0;
            }
        }

        public long LongValue
        {
            get
            {
                var value = GetCorrectValue();

                long convertedValue = 0;
                if (long.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return 0;
            }
        }

        public Single SingleValue
        {
            get
            {
                var value = GetCorrectValue();

                Single convertedValue = 0;
                if (Single.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return 0;
            }
        }

        public float FloatValue
        {
            get
            {
                var value = GetCorrectValue();

                float convertedValue = 0;
                if (float.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return 0;
            }
        }

        public double DoubleValue
        {
            get
            {
                var value = GetCorrectValue();

                double convertedValue = 0;
                if (double.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return 0;
            }
        }

        public decimal DecimalValue
        {
            get
            {
                var value = GetCorrectValue();

                decimal convertedValue = 0;
                if (decimal.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return 0;
            }
        }

        public DateTime DateTimeValue
        {
            get
            {
                var value = GetCorrectValue();

                DateTime convertedValue = new DateTime();
                if (DateTime.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return new DateTime();
            }
        }

        public bool? NullableBooleanValue
        {
            get
            {
                var value = GetCorrectValue();

                bool convertedValue = false;
                if (bool.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return null;
            }
        }

        public byte? NullableByteValue
        {
            get
            {
                var value = GetCorrectValue();

                byte convertedValue = 0;
                if (byte.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return null;
            }
        }

        public int? NullableIntegerValue
        {
            get
            {
                var value = GetCorrectValue();

                int convertedValue = 0;
                if (int.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return null;
            }
        }

        public long? NullableLongValue
        {
            get
            {
                var value = GetCorrectValue();

                long convertedValue = 0;
                if (long.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return null;
            }
        }

        public float? NullableFloatValue
        {
            get
            {
                var value = GetCorrectValue();

                float convertedValue = 0;
                if (float.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return null;
            }
        }

        public double? NullableDoubleValue
        {
            get
            {
                var value = GetCorrectValue();

                double convertedValue = 0;
                if (double.TryParse(value.ToString(), out convertedValue))
                    return convertedValue;
                return null;
            }
        }

        public Guid GuidValue
        {
            get
            {
                var value = GetCorrectValue();

                Guid convertedValue = new Guid();
                if (Guid.TryParse(value.ToString().Replace("_", "-"), out convertedValue))
                    return convertedValue;
                return new Guid();
            }
        }

        public string StringValue
        {
            get
            {
                var value = GetCorrectValue();

                return value.ToString();
            }
        }

        public OptimizedList OptimizedListValue
        {
            get
            {
                var value = GetCorrectValue();
                OptimizedList optimizedList = new OptimizedList(value.ToString());
                return optimizedList;
            }
        }



        private object GetCorrectValue()
        {
            var value = Value;

            if (value == null && DefaultValue == null)
                return string.Empty;
            else if (value == null || (value.GetType() == typeof(string) && value.ToString().Trim() == string.Empty))
                value = DefaultValue;

            return value;
        }




    }

}
