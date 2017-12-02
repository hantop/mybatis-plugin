package cn.ocoop.framework.mybatis.plugin.paging;

import cn.ocoop.framework.sql.MySqlRemoveOrderByOptimizer;
import cn.ocoop.framework.sql.MySqlReplaceSelectItemToCountOptimizer;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by liolay on 2017/11/23.
 */
@Intercepts(
        {
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        }
)
public class PagingInterceptor implements Interceptor {
    private static Map<String, MappedStatement> id_mappedStatement = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];
        Executor executor = (Executor) invocation.getTarget();
        RowBounds rowBounds = (RowBounds) args[2];
        if (rowBounds == null) {
            rowBounds = RowBounds.DEFAULT;
        }

        BoundSql boundSql;
        CacheKey cacheKey;
        if (args.length == 4) {
            boundSql = mappedStatement.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(mappedStatement, parameter, rowBounds, boundSql);
        } else {
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
        }


        ResultHandler resultHandler = (ResultHandler) args[3];
        if (!(rowBounds instanceof Page)) {
            return executor.query(mappedStatement, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        }


        Map<String, Object> additionalParameters = (Map<String, Object>) SystemMetaObject.forObject(boundSql).getValue("additionalParameters");
        Configuration configuration = mappedStatement.getConfiguration();
        String countMappedStatementId = mappedStatement.getId() + "_COUNT";

        long count;
        MappedStatement countMappedStatement = getExistedCountMappedStatement(configuration, countMappedStatementId);
        if (countMappedStatement != null) {//存在手写的sql
            CacheKey countKey = executor.createCacheKey(countMappedStatement, parameter, RowBounds.DEFAULT, boundSql);
            BoundSql countBoundSql = countMappedStatement.getBoundSql(parameter);

            List<Number> countResultList = executor.query(countMappedStatement, parameter, RowBounds.DEFAULT, resultHandler, countKey, countBoundSql);
            count = countResultList.get(0).longValue();
        } else {
            countMappedStatement = id_mappedStatement.get(countMappedStatementId);
            if (countMappedStatement == null) {
                countMappedStatement = MappedStatementUtils.newCountMappedStatement(mappedStatement, countMappedStatementId);
                id_mappedStatement.put(countMappedStatementId, countMappedStatement);
            }

            String countSql = new MySqlRemoveOrderByOptimizer().optimize(boundSql.getSql());
            countSql = new MySqlReplaceSelectItemToCountOptimizer().optimize(countSql);
            BoundSql countBoundSql = new BoundSql(countMappedStatement.getConfiguration(), countSql, boundSql.getParameterMappings(), parameter);
            for (String key : additionalParameters.keySet()) {
                countBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
            }
            CacheKey countKey = executor.createCacheKey(countMappedStatement, parameter, RowBounds.DEFAULT, boundSql);

            List<Long> countResultList = executor.query(countMappedStatement, parameter, RowBounds.DEFAULT, resultHandler, countKey, countBoundSql);
            count = countResultList.get(0);
        }

        Page page = (Page) rowBounds;
        page.setTotalRow(count);
        if (page.getTotalRow() <= 0) {
            return new ArrayList<>(0);
        }


        String sql = boundSql.getSql() + " LIMIT " + page.getStart() + "," + page.getPageSize();
        BoundSql pageBoundSql = new BoundSql(configuration, sql, boundSql.getParameterMappings(), parameter);
        for (String key : additionalParameters.keySet()) {
            pageBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
        }
        List resultList = executor.query(mappedStatement, parameter, RowBounds.DEFAULT, resultHandler, cacheKey, pageBoundSql);
        page.setData(resultList);
        return resultList;
    }


    private MappedStatement getExistedCountMappedStatement(Configuration configuration, String msId) {
        MappedStatement mappedStatement = null;
        try {
            mappedStatement = configuration.getMappedStatement(msId, false);
        } catch (Throwable t) {
            //ignore
        }
        return mappedStatement;
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }


}
