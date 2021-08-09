// 1. ������������ Cassandra Db
// ��� ������ ����� ��������� �� � ������� � ���������
// ��������� (���� �����): https://phoenixnap.com/kb/install-cassandra-on-windows  
// 2. ���� �� ���������� redis, ����� ��������  - https://github.com/microsoftarchive/redis/releases �
// ��������� �������� ������ redis (����� installer  � ����������� msi), ��� ��������� ��������� ������� �� 
// ������: "�������� ����� � redis � ���������� ��������� PATH", ��������� �������� �� ���������.
// ����� ��������� ������ ������� � cmd ����� ������: "Redis-cli ping" - ����� ������ ���� "PONG"
// ����� ���������� ������ redis � cmd  ����� ���������:  redis-server --service-stop
// ����� ��������� ������ � cmd ����� ���������:            redis-server --service-start
// � �������� API ��� ������ � redis ������������ ���������� jedis
// ������ ��� ��������� ����������, �����, ����� ���� �������� ����������, ��������� ����
   
//              Cassandra Db     
//                   /\    
//                   ||     
//               Controller
//                   /\    
//                   ||                    
        	   ������
//              ||       /\
//   ������     ||      /  \������ � ��������� ������
//             \  /      ||
//              \/       ||
              �������� ������ //<== redis ��� (jedis API)
//              ||       /\
//   ������ 	||      /  \ �������
//             \  /      ||
//              \/       ||
//               [�������]      

//  ��������� ������ ��� GET �������� (����������� ����� ����� �������� �� ���� �������, ����� ��������������� ���������):
// http://localhost:20647/sendMessage?message=bla-bla - �������� ��������� �� ������
// http://localhost:20647/getMessage - ��������� ��������� � �������
// http://localhost:20647/setDataFormat?type=xml|json - ����� ������� ������
// http://localhost:20647/insertCountry?countryName=name&countryCode=xxx - ���������� ������ � ���� ������
// http://localhost:20647/getAll - ��������� ���� ������� ���� ������
// http://localhost:20647/getCountryByCode?countryCode=348 - ������ ������ ���� ������ (������) �� ��������� (�� �� ����) 
// http://localhost:20647/getCountryByName?countryName=name - ������ ������ ���� ������ (������) �� ��������� (�� �� ��������)  
// http://localhost:20647/deleteCountryByCode?countryCode=xxx - ������ �� �������� ������ ���� ������ (������) �� ��������� (�� �� ����)  

// POST ������� �������� ����������� �������
// POST ������� ����� ������ � ������� ��������������� �������: "POST_Requester.exe", ������� ����� � ����� �������
// ��������� ��� POST ������� ������ � ���� ������: param1=val1&param2=val2&...