using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using System.Text;
using Utility;

namespace AnarSoft.Utility.Utilities
{
    public class Captcha 
    {
      Random rand = new Random();
  
    public string capcthaText { get; set; }

     public Bitmap CreateImage()
{

 //  code = GetRandomText();

   return GenerateImage();
}

     private Bitmap GenerateImage()
     {
         string code = GetRandomText();

         Bitmap bitmap = new Bitmap(150, 50, System.Drawing.Imaging.PixelFormat.Format32bppArgb);

         Graphics g = Graphics.FromImage(bitmap);
         Pen pen = new Pen(Color.Yellow);
         Rectangle rect = new Rectangle(0, 0, 150, 50);

         //   SolidBrush b = new SolidBrush(Color.BlueViolet);
          SolidBrush b = new SolidBrush(Color.FromArgb(50,50,100,220));
         SolidBrush white = new SolidBrush(Color.White);

         int counter = 0;

         g.DrawRectangle(pen, rect);
         g.FillRectangle(b, rect);

         for (int i = 0; i < code.Length; i++)
         {
             g.DrawString(code[i].ToString(), new Font("Verdena", 10 + rand.Next(14, 18)), white, new PointF(10 + counter, 10));
             counter += 20;
         }

         DrawRandomLines(g);
         g.Dispose();
        return  bitmap;
         //bitmap.Dispose();

     }

     private void DrawRandomLines(Graphics g)
     {
         SolidBrush green = new SolidBrush(Color.YellowGreen);
         for (int i = 0; i < 2; i++)
         {
             g.DrawLines(new Pen(green, 2), GetRandomPoints());
         }

     }

     private Point[] GetRandomPoints()
     {
        // Point[] points = { new Point(rand.Next(10, 150), rand.Next(10, 150)), new Point(rand.Next(10, 100), rand.Next(10, 100)) };
         Point[] points = { new Point(rand.Next(2, 150), rand.Next(1, 50)), new Point(rand.Next(2, 150), rand.Next(1, 50)) };
         return points;
     }


     private string GetRandomText()
     {
         StringBuilder randomText = new StringBuilder();
         if (String.IsNullOrEmpty(capcthaText))
         {
             string alphabets = "1234567890";//"abcdefghijklmnopqrstuvwxyz1234567890";
             Random r = new Random();
             for (int j = 0; j <= 3; j++)
             {
                 randomText.Append(alphabets[r.Next(alphabets.Length)]);
             }
             capcthaText = randomText.ToString();
         }
         return capcthaText;
     }

     //private string GetRandomText()
     //{
     //    StringBuilder randomText = new StringBuilder();

     //    if (Session["Code"] == null)
     //    {
     //        string alphabets = "abcdefghijklmnopqrstuvwxyz1234567890";

     //        Random r = new Random();
     //        for (int j = 0; j <= 5; j++)
     //        {

     //            randomText.Append(alphabets[r.Next(alphabets.Length)]);
     //        }

     //        Session["Code"] = randomText.ToString();
     //    }

     //    return Session["Code"] as String;
     //}





//private void DrawRandomLines(Graphics g)
//{
//    SolidBrush green = new SolidBrush(Color.Green);
//     for (int i = 0; i < 20; i++)

//     {
//      g.DrawLines(new Pen(green, 2), GetRandomPoints());

//     }

//}

//private Point[] GetRandomPoints()

//{
    
//    Point[] points = { new Point(rand.Next(10, 150), rand.Next(10, 150)), new Point(rand.Next(10, 100), rand.Next(10, 100)) };

//    return points;

//}


//private Bitmap GenerateImage(int width, int height,string text)
//{
//    Bitmap bitmap = new Bitmap
//      (width, height, PixelFormat.Format32bppArgb);
//    Graphics g = Graphics.FromImage(bitmap);
//    g.SmoothingMode = SmoothingMode.AntiAlias;
//    Rectangle rect = new Rectangle(0, 0, width,height);
//    HatchBrush hatchBrush = new HatchBrush(HatchStyle.SmallConfetti,
//        Color.LightGray, Color.White);
//    g.FillRectangle(hatchBrush, rect);
//    SizeF size;
//    float fontSize = rect.Height + 1;
//    Font font;

//    do
//    {
//        fontSize--;
//        font = new Font(FontFamily.GenericSansSerif, fontSize, FontStyle.Bold);
//        size = g.MeasureString(text, font);
//    } while (size.Width > rect.Width);
//    StringFormat format = new StringFormat();
//    format.Alignment = StringAlignment.Center;
//    format.LineAlignment = StringAlignment.Center;
//    GraphicsPath path = new GraphicsPath();

//    //path.AddString(text, font.FontFamily, (int)font.Style, 75, rect, format);m
//    path.AddString(text, font.FontFamily, (int)font.Style, 75, rect, format);
//    //  float v = 4F;m
//      float v = 3F;
//    PointF[] points =
//          {
//                new PointF(rand.Next(rect.Width) / v, rand.Next(
//                   rect.Height) / v),
//                new PointF(rect.Width - this.rand.Next(rect.Width) / v, 
//                    this.rand.Next(rect.Height) / v),
//                new PointF(this.rand.Next(rect.Width) / v, 
//                    rect.Height - this.rand.Next(rect.Height) / v),
//                new PointF(rect.Width - this.rand.Next(rect.Width) / v,
//                    rect.Height - this.rand.Next(rect.Height) / v)
//          };
//    Matrix matrix = new Matrix();
//    matrix.Translate(0F, 0F);
//    path.Warp(points, rect, matrix, WarpMode.Perspective, 0F);
//    hatchBrush = new HatchBrush(HatchStyle.Percent10, Color.Black, Color.SkyBlue);
//    g.FillPath(hatchBrush, path);
//    //اندازه خالهارا مشخص می کند
//    int m = Math.Max(rect.Width, rect.Height);
//    //خال خالی میکند
//    for (int i = 0; i < (int)(rect.Width * rect.Height / 30F); i++)
//    {
//        int x = this.rand.Next(rect.Width);
//        int y = this.rand.Next(rect.Height);
//        int w = this.rand.Next(m / 50);
//        int h = this.rand.Next(m / 50);
//        g.FillEllipse(hatchBrush, x, y, w, h);
//    }
//    font.Dispose();
//    hatchBrush.Dispose();
//    g.Dispose();
//    return bitmap;
//}

//private void Page_Load(object sender, System.EventArgs e)
//{
//    Bitmap objBMP = new System.Drawing.Bitmap(60, 20);
//    Graphics objGraphics = System.Drawing.Graphics.FromImage(objBMP);
//    objGraphics.Clear(Color.Green);
//    objGraphics.TextRenderingHint = TextRenderingHint.AntiAlias;
//    //' Configure font to use for text
//    Font objFont = new Font("Arial", 8, FontStyle.Bold);
//    string randomStr = "";
//    int[] myIntArray = new int[5];
//    int x;
//    //That is to create the random # and add it to our string 
//    Random autoRand = new Random();
//    for (x = 0; x < 5; x++)
//    {
//        myIntArray[x] = System.Convert.ToInt32(autoRand.Next(0, 9));
//        randomStr += (myIntArray[x].ToString());
//    }
//    //This is to add the string to session cookie, to be compared later
//    Session.Add("randomStr", randomStr);
//    //' Write out the text
//    objGraphics.DrawString(randomStr, objFont, Brushes.White, 3, 3);
//    //' Set the content type and return the image
//    Response.ContentType = "image/GIF";
//    objBMP.Save(Response.OutputStream, ImageFormat.Gif);
//    objFont.Dispose();
//    objGraphics.Dispose();
//    objBMP.Dispose();
//}



    }
}
