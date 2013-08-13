Amazon DynamoDB Session Manager for Tomcat 
===============================

This is an attempt to port [jcoleman/tomcat-redis-session-manager](https://github.com/jcoleman/tomcat-redis-session-manager) to AWS DynamoDB. You will find most of the classes/methods are similar to the referenced project.

Note: This is an experimental project. I haven't done any performance testing, so I am not sure this is ready for production environment.

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

How to Build:
---

gradle assemble
