package cn.ocoop.framework.mybatis.plugin.tenant;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by liolay on 2017/11/8.
 */
@Configuration
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class InterceptConfiguration {
    @Autowired
    private MybatisProperties mybatisProperties;

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @PostConstruct
    public void addInterceptor() {
        Interceptor pagingInterceptor = new TenantIntercept(mybatisProperties.getConfigurationProperties());
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(pagingInterceptor);
        }
    }

}
