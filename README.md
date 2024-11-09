1. change the google API key value with your own key value at findandfriend\app\src\main\res\values\strings.xml
2. The two installation file of database are mysql-workbench-community-8.0.40-winx64.msi and mysql-9.1.0-winx64.msi which can't be pushed to github you can download it
3. Run the MySQL Config Server
5. the sql database file was export to findandfriend\findandfriendserver\all_databases.sql on MySQL Workbench select Server > Data Import > Import from Self-Contained File (select all_databases.sql) > Start Import
7. the server python package requirement is stored at findandfriend\findandfriendserver\requirements.txt you can use pip -r requirements.txt to install them NOTE: Use Python 3.10
8. a simple python code findandfriend\findandfriendserver\readdatabase.py to check database read and write 
9. The findandfriend\findandfriendserver\sqlserver.py is the server code. I tested at home network, it doesn't require any configuration
10. you will need to change findandfriend\app\src\main\java\com\example\findandfriend
   --LoginActivity.java line 179 and line 237 with your local ip address
   --SearchFriendActivity line 52 with your Ip address
