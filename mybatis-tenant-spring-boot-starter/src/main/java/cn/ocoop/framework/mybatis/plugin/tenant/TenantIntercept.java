package cn.ocoop.framework.mybatis.plugin.tenant;

import cn.ocoop.framework.sql.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Properties;

/**
 * Created by liolay on 2017/11/24.
 */
@Intercepts(
        {
                @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        }
)
@Slf4j
public class TenantIntercept implements Interceptor {
    private static String tenantColumn;
    private static String tenantColumnType;

    public TenantIntercept(Properties properties) {
        tenantColumn = (String) properties.getOrDefault("tenantColumn", "MERCHANT_ID");
        tenantColumnType = (String) properties.getOrDefault("tenantColumnType", "String");
    }


    public Object realTarget(Object target) {
        if (Proxy.isProxyClass(target.getClass())) {
            MetaObject metaObject = SystemMetaObject.forObject(target);
            return realTarget(metaObject.getValue("h.target"));
        }
        return target;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (TC.getTenantId() == null) {
            log.warn("tenant id is not present through TC.set(o),tenant filter is disabled");
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String optimizedSql = boundSql.getSql();
        if (SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlInsertTenantOptimizer(tenantColumn, tenantColumnType).optimize(optimizedSql).get(0);
        } else if (SqlCommandType.DELETE.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlDeleteTenantOptimizer(tenantColumn, tenantColumnType).optimize(optimizedSql).get(0);
        } else if (SqlCommandType.UPDATE.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlUpdateTenantOptimizer(tenantColumn, tenantColumnType).optimize(optimizedSql).get(0);
        } else if (SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlSelectTenantOptimizer(tenantColumn, tenantColumnType).optimize(optimizedSql).get(0);
        }
        metaObject.setValue("delegate.boundSql.sql", optimizedSql);
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
