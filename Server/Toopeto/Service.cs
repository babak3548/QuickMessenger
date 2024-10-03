using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Toopeto
{
    public class Service
    {

        private tupetooEntities1 dbContext = null;
        /// <summary>
        /// تا اولین مراجعه استفاد ایجاد نمی گردد
        /// </summary>
        protected tupetooEntities1 DbContext
        {
            get
            {
                if (dbContext == null) {
                    dbContext = new tupetooEntities1();
                    return dbContext;
                }
                else
                {
                    return dbContext;
                }
            }
        }

        internal void SaveDbContect()
        {
            DbContext.SaveChanges();
        }

        /// <summary>
        /// دی بی کانتکست ایجاد شده را حذف می کند
        /// </summary>
        protected void Dispose()
        {

                throw new System.NotImplementedException();

        }
        /// <summary>
        /// ایجاد دی بی کانتکست
        /// </summary>
        private tupetooEntities1 CreateDBContext()
        {
            throw new System.NotImplementedException();
        }


        public void HandelMessage()
        {
            throw new System.NotImplementedException();
        }

        protected void Add()
        {
            throw new System.NotImplementedException();
        }

        protected void Delete()
        {
            throw new System.NotImplementedException();
        }

        protected void Update()
        {
            throw new System.NotImplementedException();
        }
    }
}
