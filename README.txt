// 1. Используется Cassandra Db
// Для начала нужно поставить ее в систему и запустить
// Иструкция (если нужно): https://phoenixnap.com/kb/install-cassandra-on-windows  
// 2. Если не установлен redis, нужно посетить  - https://github.com/microsoftarchive/redis/releases и
// загрузить новейшую версию redis (можно installer  с расширением msi), при установке поставить галочку на 
// пункте: "Добавить папку с redis в переменную окружения PATH", остальное оставить по умолчанию.
// Чтобы проверить работу сервиса в cmd нужно ввести: "Redis-cli ping" - ответ должен быть "PONG"
// Чтобы остановить сервер redis в cmd  нужно выполнить:  redis-server --service-stop
// Чтобы запустить сервер в cmd нужно выполнить:            redis-server --service-start
// В качестве API для работы с redis используется библиотека jedis
// Прежде чем запускать приложение, нужно, чтобы были запущены компоненты, описанные выше
   
//              Cassandra Db     
//                   /\    
//                   ||     
//               Controller
//                   /\    
//                   ||                    
        	   Брокер
//              ||       /\
//   Ответы     ||      /  \Запрос с обратного прокси
//             \  /      ||
//              \/       ||
              Обратный прокси //<== redis кеш (jedis API)
//              ||       /\
//   Ответы 	||      /  \ Запросы
//             \  /      ||
//              \/       ||
//               [Клиенты]      

//  Доступные адреса для GET запросов (Тестировать можно прямо переходя по этим ссылкам, меняя соответствующие параметры):
// http://localhost:20647/sendMessage?message=bla-bla - Отправка сообщений на сервер
// http://localhost:20647/getMessage - получение сообщений с сервера
// http://localhost:20647/setDataFormat?type=xml|json - Смена формата данных
// http://localhost:20647/insertCountry?countryName=name&countryCode=xxx - Добавление записи в базу данных
// http://localhost:20647/getAll - Получение всех записей базы данных
// http://localhost:20647/getCountryByCode?countryCode=348 - Запрос записи базы данных (страны) по параметру (по ее коду) 
// http://localhost:20647/getCountryByName?countryName=name - Запрос записи базы данных (страны) по параметру (по ее названию)  
// http://localhost:20647/deleteCountryByCode?countryCode=xxx - Запрос на удаление записи базы данных (страны) по параметру (по ее коду)  

// POST запросы работают аналогичным образом
// POST запросы можно делать с помощью вспомогательной софтины: "POST_Requester.exe", которая лежит в папке проекта
// Параметры для POST запроса шлются в виде строки: param1=val1&param2=val2&...