mybatis-plugin
==================
一、项目说明
------------------
1. 该项目主要提供两个插件`mybatis-paging-spring-boot-starter（MySQL分页）`,`mybatis-tenant-spring-boot-starter(MySQL多租户)`
2. 由于我的项目在使用spring-boot,所以插件便直接使用spring-boot-starter形式提供了
3. 由于我的项目在使用MySQL数据库，所以插件目前只支持MySQL数据库


二、使用
------------------
在使用这两个插件时所必须要做的是引入mybaits-spring-boot-starter,例如:
```xml
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>1.3.1</version>
    </dependency>
```
接下来增加依赖

**1.分页依赖**
```xml
    <dependency>
        <groupId>cn.ocoop.framework</groupId>
        <artifactId>mybatis-paging-spring-boot-starter</artifactId>
        <version>1.2</version>
    </dependency>
```
正如你看到的，就那么多，你便可以实现自动分页了,除此之外你什么都不需要做

java demo:
```java
public Page<Order> getOrder(Order order) {
    Page<Order> page = new Page<>();
    orderDao.getOrder1(page, order.getOrderId());
    return page;
}
```
>_**目前只支持MySQL分页**_


**2.多租户依赖**
1. 用处，我们在多商户的系统里写sql一般都会加上商户ID，列如：`select * from user where merchant_id = ?; `，`update user set name = ? where merchant_id = ?;`，` delete user where merchant_id = ?;`，`insert user(merchant_id,u_id,name) values(?,?,?);`
写的多了就会发现越写越烦，每个sql都要加上商户ID,这个插件就是解决这个问题的
2. 形式：
   ```sql
     insert into T(id) VALUES(?) --> insert into T(merchant_id,id) VALUES(?,?)
     insert into T(id) select id FROM T2--> insert into T(merchant_id,id) select merchant_id,id FROM T2 where merchant_id = ?
     
     delete from user a where a.name = 'aaa' and exists(select null from member a where a.name like '%aa%'); 
     -->DELETE FROM user a WHERE a.name = 'aaa' AND EXISTS ( SELECT NULL FROM member a WHERE a.name LIKE '%aa%' ) AND a.merchant_id = 'aaaa'

     
     update t_a a set a.name='liolay' where a.sex='M';
     -->UPDATE t_a a SET a.name = 'liolay' WHERE a.sex = 'M' AND a.merchant_id = 'aaaa';
  
     select * from T --> select * from user where merchant_id = ?
    
     select t.* from T t --> select a.* from user a where a.merchant_id = ?

     select t.* FROM T a join T1 b on a.id = b.id --> select t.* FROM T a join T1 b on a.id = b.id where a.merchant_id = ?
    
     select t.* FROM T1 a ,T2 b WHERE a.id = b.id --> select t.* FROM T1 a ,T2 b WHERE a.id = b.id and a.merchant_id = ?
     
     -->其他 
   ``` 
  
3. 添加依赖
```xml
<dependency>
    <groupId>cn.ocoop.framework.mybatis.plugin</groupId>
    <artifactId>mybatis-tenant-spring-boot-starter</artifactId>
    <version>1.2</version>
</dependency>
```
使用该starter你需要做一些mybatis-spring-boot-starter的配置，不过这很简单，例如：
```yaml
mybatis:
  mapper-locations: classpath:**/mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
  configuration-properties:
    #这个配置是我们插件的，表示商户ID在数据库的列名
    tenantColumn : TENANT_ID 
    #这个配置是我们插件的，表示商户ID在数据库的数据类型，支持String/Number，正确的数据类型能够更好的使用索引
    tenantColumnType : Number
    #这个配置是我们插件的，表示插件特定类型的sql是否启用
    tenantInsertEnabled : true
    tenantDeleteEnabled : true
    tenantUpdateEnabled : true
    tenantSelectEnabled : true
```
>_**如果你还没见过这种配置，请参照下spring-boot yaml配置方式，并参阅[mybatis-spring-boot-starter](https://github.com/mybatis/spring-boot-starter "https://github.com/mybatis/spring-boot-starter")**_

4. java
```java
    public void getOrder(String orderId) {
        TC.set("这里设置商户ID");//设置的商户ID会自动拼接到sql里，不设置则不改变sql
        orderDao.getOrder(orderId);
        
        TC.clear();//清除设置的商户ID,不会改变sql
        orderDao.getOrder(orderId);
    }
```


    
    


