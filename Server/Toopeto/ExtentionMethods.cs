using System;
using System.Collections.Generic;
using System.Data.Entity.Spatial;
using System.Linq;
using System.Text;
using Toopeto.JsonPacket;
namespace Toopeto
{
    public static class ExtentionMethods
    {

        public static DbGeography CreatePoint(double latitude, double longitude)
        {
            var text = string.Format("POINT({0} {1})", longitude, latitude);
            // 4326 is most common coordinate system used by GPS/Maps
            return DbGeography.PointFromText(text, 4326);
        }
        public static DbGeography CreatePoint(string latitudeLongitude)
        {
            var tokens = latitudeLongitude.Split('*', ' ');
            if (tokens.Length != 2)
                throw new ArgumentException("invalid latitude or longitude");
            var text = string.Format("POINT({0} {1})", tokens[1], tokens[0]);
            return DbGeography.PointFromText(text, 4326);
        }

        public static string getRelationGroupName(this string one, string two)
        {
            if (string.Compare(one, two) > 0) return two + "+" + one;
            else return one + "+" + two;
        }
    }
}
