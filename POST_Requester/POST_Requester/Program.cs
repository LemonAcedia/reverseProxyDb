using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;

namespace POST_Requester
{
    class Program
    {
        static void getResponse(string link, string postData) 
        {
            Console.WriteLine("Начинаю делать POST запрос на адрес: " + link);
            
            Console.WriteLine("С параметрами: " + postData);

            Console.WriteLine();
            Console.WriteLine();
            Console.WriteLine();


            WebRequest request = WebRequest.Create(link);

            request.Method = "POST";

            byte[] byteArray = Encoding.UTF8.GetBytes(postData);

            request.ContentType = "application/x-www-form-urlencoded";

            request.ContentLength = byteArray.Length;

            Stream dataStream = request.GetRequestStream();

            dataStream.Write(byteArray, 0, byteArray.Length);

            dataStream.Close();

            WebResponse response = request.GetResponse();

            dataStream = response.GetResponseStream();

            StreamReader reader = new StreamReader(dataStream);

            string responseFromServer = reader.ReadToEnd();

            Console.WriteLine("RESPONSE:");
            Console.WriteLine();

            Console.WriteLine(((HttpWebResponse)response).StatusDescription);
            Console.WriteLine(responseFromServer);

            Console.WriteLine();
            Console.WriteLine();
            Console.WriteLine();

            reader.Close();
            dataStream.Close();
            response.Close();
        }

        static void makeRequestPOST()
        {
            Console.WriteLine("Выберите метод:");

            string[] links = new string [] { "http://localhost:20647/sendMessage",
                                                "http://localhost:20647/getMessage",
                                                "http://localhost:20647/setDataFormat",
                                                "http://localhost:20647/insertCountry",
                                                "http://localhost:20647/getAll",
                                                "http://localhost:20647/getCountryByCode",
                                                "http://localhost:20647/getCountryByName",
                                                "http://localhost:20647/deleteCountryByCode"};

            for (int i = 0; i < links.Length; i++)
                Console.WriteLine("№" + i + " " + links[i]);

            Console.Write("№: ");

            string num = Console.ReadLine();

            try {

                int methodNumber = Convert.ToInt32(num);

                string message, typeFormat, countryName, countryCode;

                switch (methodNumber) {

                    case 0:
                        Console.WriteLine("Пожалуйста, введите сообщение:");
                        message = Console.ReadLine();
                        getResponse(links[methodNumber], "message=" + message);
                        break;

                    case 1:
                        getResponse(links[methodNumber], "");
                        break;

                    case 2:
                        Console.WriteLine("Пожалуйста, введите формат данных (xml | json):");
                        typeFormat = Console.ReadLine();
                        getResponse(links[methodNumber], "type=" + typeFormat);
                        break;

                    case 3:
                        Console.WriteLine("Пожалуйста, введите название страны, которую хотите добавить:");
                        countryName = Console.ReadLine();
                        Console.WriteLine("Пожалуйста, введите код страны, которую хотите добавить:");
                        countryCode = Console.ReadLine();
                        getResponse(links[methodNumber], "countryName=" + countryName + "&countryCode=" + countryCode);
                        break;

                    case 4:
                        getResponse(links[methodNumber], "");
                        break;

                    case 5:
                        Console.WriteLine("Пожалуйста, введите код страны:");
                        countryCode = Console.ReadLine();
                        getResponse(links[methodNumber], "countryCode=" + countryCode);
                        break;

                    case 6:
                        Console.WriteLine("Пожалуйста, введите название страны:");
                        countryName = Console.ReadLine();
                        getResponse(links[methodNumber], "countryName=" + countryName);
                        break;

                    case 7:
                        Console.WriteLine("Пожалуйста, введите код страны:");
                        countryCode = Console.ReadLine();
                        getResponse(links[methodNumber], "countryCode=" + countryCode);
                        break;

                    default:
                        break;
                }

            }
            catch (Exception e) { }
        }
        static void Main(string[] args)
        {
            while (true) 
            {
                makeRequestPOST();
            }
        }
    }
}
