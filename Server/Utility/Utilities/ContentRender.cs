using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace AnarSoft.Utility.Utilities
{
    public class ContentRender
    {
        delegate string contentMethodDelegate(string stringItem);
        private  string ContentRenderDelegate(List<string> listDataItem, contentMethodDelegate contentMethod)
        {
            string resultStringContent = string.Empty;
            foreach (var item in listDataItem )
            {
                resultStringContent += contentMethod(item);
            }
            return resultStringContent;
        }

        public string  menuItemGenerator(string ControlerName)
        {
            throw  new NotImplementedException();
        }

    }
}
