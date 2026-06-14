package com.railway.common.config;

import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis + PageHelper 配置：
 * <ul>
 *   <li>{@code @MapperScan} 扫描所有业务模块的 mapper 接口</li>
 *   <li>手动创建 {@link SqlSessionFactory}，确保 mapperLocations 被正确加载</li>
 *   <li>注册 {@link PageInterceptor}</li>
 * </ul>
 */
@Configuration
@MapperScan("com.railway.**.mapper")
public class MyBatisConfig {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml"));
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        // PageHelper 插件
        PageInterceptor pageInterceptor = new PageInterceptor();
        java.util.Properties props = new java.util.Properties();
        props.setProperty("helperDialect", "mysql");
        props.setProperty("reasonable", "true");
        pageInterceptor.setProperties(props);
        bean.setPlugins(pageInterceptor);
        return bean.getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public PageInterceptor pageInterceptor() {
        return new PageInterceptor();
    }
}
