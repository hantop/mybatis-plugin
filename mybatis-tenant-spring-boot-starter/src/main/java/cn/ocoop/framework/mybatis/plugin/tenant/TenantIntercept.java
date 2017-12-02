package cn.ocoop.framework.mybatis.plugin.tenant;

import cn.ocoop.framework.sql.TC;
import cn.ocoop.framework.sql.tenant.MySqlDeleteTenantOptimizer;
import cn.ocoop.framework.sql.tenant.MySqlInsertTenantOptimizer;
import cn.ocoop.framework.sql.tenant.MySqlSelectTenantOptimizer;
import cn.ocoop.framework.sql.tenant.MySqlUpdateTenantOptimizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
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
    private final String TENANT_COLUMN;
    private final String TENANT_COLUMN_TYPE;
    private final boolean INSERT_ENABLED, DELETE_ENABLED, UPDATE_ENABLED, SELECT_ENABLED;

    public TenantIntercept(Properties properties) {
        TENANT_COLUMN = MapUtils.getString(properties, "tenantColumn", "TENANT_ID");
        TENANT_COLUMN_TYPE = MapUtils.getString(properties, "tenantColumnType", "Number");
        INSERT_ENABLED = MapUtils.getBoolean(properties, "tenantInsertEnabled", true);
        DELETE_ENABLED = MapUtils.getBoolean(properties, "tenantDeleteEnabled", true);
        UPDATE_ENABLED = MapUtils.getBoolean(properties, "tenantUpdateEnabled", true);
        SELECT_ENABLED = MapUtils.getBoolean(properties, "tenantSelectEnabled", true);
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
        if (TC.get() == null) {
            log.warn("tenant id is not present through TC.set(o),tenant filter is disabled");
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String optimizedSql = boundSql.getSql();
        if (INSERT_ENABLED && SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlInsertTenantOptimizer(TENANT_COLUMN, TENANT_COLUMN_TYPE).optimize(optimizedSql);
        } else if (DELETE_ENABLED && SqlCommandType.DELETE.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlDeleteTenantOptimizer(TENANT_COLUMN, TENANT_COLUMN_TYPE).optimize(optimizedSql);
        } else if (UPDATE_ENABLED && SqlCommandType.UPDATE.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlUpdateTenantOptimizer(TENANT_COLUMN, TENANT_COLUMN_TYPE).optimize(optimizedSql);
        } else if (SELECT_ENABLED && SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            optimizedSql = new MySqlSelectTenantOptimizer(TENANT_COLUMN, TENANT_COLUMN_TYPE).optimize(optimizedSql);
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
