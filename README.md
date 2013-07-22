Amazon DynamoDB Session Manager for Tomcat 
===============================

AWS DynamoDB session store for Tomcat server.
I tried to port [jcoleman/tomcat-redis-session-manager](https://github.com/jcoleman/tomcat-redis-session-manager) to AWS DynamoDB and created this project. You will find most of the classes/methods are similar as the referenced project.

Note: This is an experimental project. I haven't done any performance testing, so it is not ready for production environment. Also, DynamoDB read/write costs should be considered before using this session manager in production.

Tomcat Version Compatibility:
---
Tested with Tomcat 7. It should work with Tomcat 6 also, will test it and update this section.

Usage:
---

Add the below details to your Tomcat context.xml file.

    <Valve className="com.pandiaraj.catalina.session.DynamoSessionHandlerValve" />

    <Manager className="com.pandiaraj.catalina.session.DynamoSessionManager" 
        accessKey="YOUR AWS ACCESS KEY"
        secretKey="YOUT AWS SECRET KEY"
        tableName="tomcat_sessions" <!-- DynamoDB table name to store sessions -->
        hashKey="session_id"  <!-- DynamoDB session table hash key -->
        region="ap-southeast-1" <!-- AWS region name --> />

