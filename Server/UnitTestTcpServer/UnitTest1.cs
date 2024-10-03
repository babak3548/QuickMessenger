using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Toopeto;
using System.Net.Sockets;
namespace UnitTestTcpServer
{
    [TestClass]
    public class UnitTest1
    {

        [TestMethod]
        public void x() {
            int i = 1, j = 2;
            var x = i + j;
        }
        [TestMethod]
        public void TestMethod1()
        {
           // Toopeto.Manager manager = new Manager();
            System.Net.Sockets.TcpClient clientSocket = new System.Net.Sockets.TcpClient();
            clientSocket.Connect("127.0.0.1", 3001);

            NetworkStream serverStream = clientSocket.GetStream();
            byte[] outStream = System.Text.Encoding.ASCII.GetBytes("xxxxxxxx" + "$");
            serverStream.Write(outStream, 0, outStream.Length);
            serverStream.Flush();

            byte[] inStream = new byte[10025];
            serverStream.Read(inStream, 0, (int)clientSocket.ReceiveBufferSize);
            string returndata = System.Text.Encoding.ASCII.GetString(inStream);
            reciveResault(returndata);
          //  textBox2.Text = "";
         //   textBox2.Focus();
        }

        private void reciveResault(string returndata)
        {
            var x = returndata;
            throw new NotImplementedException();
        }
    }
}
