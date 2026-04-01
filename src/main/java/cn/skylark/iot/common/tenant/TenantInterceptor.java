package cn.skylark.iot.common.tenant;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Properties;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class TenantInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_ID_COLUMN = "tenant_id";
    private static final String[] TENANT_TABLES = {
            "iot_product",
            "iot_device",
            "iot_thing_model",
            "iot_acl_policy"
    };

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return invocation.proceed();
        }

        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        if (!shouldIntercept(originalSql)) {
            return invocation.proceed();
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);
            String modifiedSql = originalSql;

            if (statement instanceof Select) {
                modifiedSql = processSelect((Select) statement, tenantId);
            } else if (statement instanceof Insert) {
                modifiedSql = processInsert((Insert) statement, tenantId);
            } else if (statement instanceof Update) {
                modifiedSql = processUpdate((Update) statement, tenantId);
            } else if (statement instanceof Delete) {
                modifiedSql = processDelete((Delete) statement, tenantId);
            }

            if (!originalSql.equals(modifiedSql)) {
                BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), modifiedSql,
                        boundSql.getParameterMappings(), parameter);
                for (org.apache.ibatis.mapping.ParameterMapping mapping : boundSql.getParameterMappings()) {
                    String prop = mapping.getProperty();
                    if (boundSql.hasAdditionalParameter(prop)) {
                        newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
                    }
                }
                newBoundSql.setAdditionalParameter("tenantId", tenantId);
                invocation.getArgs()[0] = copyFromMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));
            }
        } catch (JSQLParserException e) {
            log.warn("Failed to parse SQL, use original SQL: {}", originalSql, e);
        }

        return invocation.proceed();
    }

    private boolean shouldIntercept(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        String upperSql = sql.toUpperCase().trim();
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("INSERT")
                && !upperSql.startsWith("UPDATE") && !upperSql.startsWith("DELETE")) {
            return false;
        }
        for (String table : TENANT_TABLES) {
            if (sql.contains(table)) {
                return true;
            }
        }
        return false;
    }

    private String processSelect(Select select, Long tenantId) {
        if (select.getSelectBody() instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            if (plainSelect.getFromItem() instanceof Table) {
                Table table = (Table) plainSelect.getFromItem();
                if (isTenantTable(table.getName())) {
                    if (plainSelect.getWhere() != null && plainSelect.getWhere().toString().contains(TENANT_ID_COLUMN)) {
                        return select.toString();
                    }
                    EqualsTo tenantCondition = new EqualsTo();
                    tenantCondition.setLeftExpression(new Column(table, TENANT_ID_COLUMN));
                    tenantCondition.setRightExpression(new LongValue(tenantId));
                    if (plainSelect.getWhere() != null) {
                        plainSelect.setWhere(new AndExpression(plainSelect.getWhere(), tenantCondition));
                    } else {
                        plainSelect.setWhere(tenantCondition);
                    }
                }
            }
        }
        return select.toString();
    }

    private String processInsert(Insert insert, Long tenantId) {
        Table table = insert.getTable();
        if (!isTenantTable(table.getName())) {
            return insert.toString();
        }
        List<Column> columns = insert.getColumns();
        if (columns != null) {
            for (Column column : columns) {
                if (TENANT_ID_COLUMN.equalsIgnoreCase(column.getColumnName())) {
                    return insert.toString();
                }
            }
        }
        return insert.toString();
    }

    private String processUpdate(Update update, Long tenantId) {
        Table table = update.getTable();
        if (isTenantTable(table.getName())) {
            if (update.getWhere() != null && update.getWhere().toString().contains(TENANT_ID_COLUMN)) {
                return update.toString();
            }
            EqualsTo tenantCondition = new EqualsTo();
            tenantCondition.setLeftExpression(new Column(table, TENANT_ID_COLUMN));
            tenantCondition.setRightExpression(new LongValue(tenantId));
            if (update.getWhere() != null) {
                update.setWhere(new AndExpression(update.getWhere(), tenantCondition));
            } else {
                update.setWhere(tenantCondition);
            }
        }
        return update.toString();
    }

    private String processDelete(Delete delete, Long tenantId) {
        Table table = delete.getTable();
        if (isTenantTable(table.getName())) {
            if (delete.getWhere() != null && delete.getWhere().toString().contains(TENANT_ID_COLUMN)) {
                return delete.toString();
            }
            EqualsTo tenantCondition = new EqualsTo();
            tenantCondition.setLeftExpression(new Column(table, TENANT_ID_COLUMN));
            tenantCondition.setRightExpression(new LongValue(tenantId));
            if (delete.getWhere() != null) {
                delete.setWhere(new AndExpression(delete.getWhere(), tenantCondition));
            } else {
                delete.setWhere(tenantCondition);
            }
        }
        return delete.toString();
    }

    private boolean isTenantTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String name = tableName.toLowerCase();
        for (String tenantTable : TENANT_TABLES) {
            if (name.equals(tenantTable)) {
                return true;
            }
        }
        return false;
    }

    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.timeout(ms.getTimeout());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        }
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        if (ms.getResultMaps() != null && !ms.getResultMaps().isEmpty()) {
            builder.resultMaps(ms.getResultMaps());
        }
        if (ms.getResultSetType() != null) {
            builder.resultSetType(ms.getResultSetType());
        }
        if (ms.getDatabaseId() != null) {
            builder.databaseId(ms.getDatabaseId());
        }
        builder.resultOrdered(ms.isResultOrdered());
        if (ms.getLang() != null) {
            builder.lang(ms.getLang());
        }
        return builder.build();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private static class BoundSqlSqlSource implements SqlSource {
        private final BoundSql boundSql;

        BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}
