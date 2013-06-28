tomcat-dynamodb-session-manager
===============================

AWS DynamoDB session store for Tomcat server

Configuration:
-------------
Add the below details to your Tomcat context.xml file.

    <Valve className="com.pandiaraj.catalina.session.DynamoSessionHandlerValve" />

    <Manager className="com.pandiaraj.catalina.session.DynamoSessionManager" 
        accessKey="YOUR AWS ACCESS KEY"
        secretKey="YOUT AWS SECRET KEY"
        tableName="tomcat_sessions" <!-- DynamoDB table name to store sessions -->
        hashKey="session_id"  <!-- DynamoDB session table hash key -->
        region="ap-southeast-1" <!-- AWS region name --> />
