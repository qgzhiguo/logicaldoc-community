package com.logicaldoc.core;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.logicaldoc.core.metadata.Attribute;
import com.logicaldoc.core.metadata.ExtensibleObject;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;

/**
 * Hibernate implementation of <code>PersistentObjectDAO</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 4.0
 * 
 * @param <T> Class of the implementation of a {@link PersistentObject} this DAO
 *        handles
 */
public abstract class HibernatePersistentObjectDAO<T extends PersistentObject> implements PersistentObjectDAO<T> {

	private static final String UPDATE = "update ";

	private static final String DATA_SOURCE = "DataSource";

	private static final String AND = " and (";

	private static final String ORDER_BY = "order by";

	protected Logger log = LoggerFactory.getLogger(HibernatePersistentObjectDAO.class);

	protected Class<T> entityClass;

	@Resource(name = "SessionFactory")
	protected SessionFactory sessionFactory;

	protected static final String ASPECT_STORING = "storing";

	private static final String DEFAULT_WHERE_PREAMBLE = " " + ENTITY + " where " + ENTITY + ".deleted=0 ";

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	protected HibernatePersistentObjectDAO(Class<T> entityClass) {
		super();
		this.entityClass = entityClass;
	}

	public void delete(long id, int code) throws PersistenceException {
		if (code == 0)
			throw new IllegalArgumentException("code cannot be 0");

		if (!checkStoringAspect())
			return;

		T entity = findById(id);
		if (entity == null)
			return;
		entity.setDeleted(code);
		store(entity);
	}

	public void delete(long id) throws PersistenceException {
		delete(id, PersistentObject.DELETED_CODE_DEFAULT);
	}

	public List<T> findAll() throws PersistenceException {
		return findByWhere("", "", null);
	}

	public List<T> findAll(long tenantId) throws PersistenceException {
		return findByWhere(" " + ENTITY + ".tenantId=" + tenantId, "", null);
	}

	public List<Long> findAllIds() throws PersistenceException {
		return findIdsByWhere("", "", null);
	}

	public List<Long> findAllIds(long tenantId) throws PersistenceException {
		return findIdsByWhere(" " + ENTITY + ".tenantId=" + tenantId, "", null);
	}

	@Override
	public T findById(long id, boolean initialize) throws PersistenceException {
		T entity = findById(id);
		if (initialize)
			initialize(entity);
		return entity;
	}

	@Override
	public T findById(long id) throws PersistenceException {
		T entity = null;
		try {
			entity = sessionFactory.getCurrentSession().get(entityClass, id);
			if (entity != null && entity.getDeleted() == 1)
				return null;
			return entity;
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public List<T> findByWhere(String where, String order, Integer max) throws PersistenceException {
		return findByWhere(where, (Map<String, Object>) null, order, max);
	}

	@Override
	public List<T> findByWhere(String where, Map<String, Object> parameters, String order, Integer max)
			throws PersistenceException {
		List<T> coll = new ArrayList<>();
		try {
			String sorting = StringUtils.isNotEmpty(order) && !order.toLowerCase().contains(ORDER_BY)
					? ORDER_BY + " " + order
					: order;
			String query = "from " + entityClass.getCanonicalName() + DEFAULT_WHERE_PREAMBLE
					+ (StringUtils.isNotEmpty(where) ? AND + where + ") " : " ")
					+ (StringUtils.isNotEmpty(sorting) ? sorting : " ");
			coll = findByObjectQuery(query, parameters, max);
			return coll;
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public List<T> findByObjectQuery(String query, Map<String, Object> parameters, Integer max)
			throws PersistenceException {
		List<T> coll = new ArrayList<>();
		try {
			logQuery(query);
			Query<T> queryObject = prepareQueryForObject(query, parameters, max);
			coll = queryObject.list();
			return coll;
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	protected void logQuery(String query) {
		log.debug("Execute query: {}", query);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List findByQuery(String query, Map<String, Object> parameters, Integer max) throws PersistenceException {
		List<Object> coll = new ArrayList<>();
		try {
			logQuery(query);
			Query queryObject = prepareQuery(query, parameters, max);
			coll = queryObject.list();
			return coll;
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public List<Long> findIdsByWhere(String where, String order, Integer max) throws PersistenceException {
		return findIdsByWhere(where, new HashMap<>(), order, max);
	}

	@Override
	public List<Long> findIdsByWhere(String where, Map<String, Object> parameters, String order, Integer max)
			throws PersistenceException {
		List<Long> coll = new ArrayList<>();
		try {
			String sorting = StringUtils.isNotEmpty(order) && !order.toLowerCase().contains(ORDER_BY)
					? ORDER_BY + " " + order
					: order;
			String query = "select " + ENTITY + ".id from " + entityClass.getCanonicalName() + DEFAULT_WHERE_PREAMBLE
					+ (StringUtils.isNotEmpty(where) ? AND + where + ") " : " ")
					+ (StringUtils.isNotEmpty(sorting) ? sorting : " ");
			logQuery(query);
			Query<Long> queryObject = prepareQueryForLong(query, parameters, max);
			coll = queryObject.list();
			return coll;
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Checks if the aspect for storing data is enabled
	 */
	protected boolean checkStoringAspect() {
		if (!RunLevel.current().aspectEnabled(ASPECT_STORING)) {
			log.error("Apect {} is disabled", ASPECT_STORING);
			return false;
		}
		return true;
	}

	public void store(T entity) throws PersistenceException {
		if (!checkStoringAspect())
			return;
		entity.setLastModified(new java.util.Date());

		// Save the entity
		try {
			saveOrUpdate(entity);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	protected void saveOrUpdate(Object entity) {
		// Update the attributes
		if (entity instanceof ExtensibleObject extensibleEntity) {
			try {
				for (String name : extensibleEntity.getAttributes().keySet()) {
					Attribute att = extensibleEntity.getAttribute(name);
					if (att.getMultiple() == 1 && att.getType() == Attribute.TYPE_STRING) {
						String vals = extensibleEntity.getValues(name).stream().map(Object::toString)
								.collect(Collectors.joining(","));
						att.setStringValues(vals);
					} else
						att.setStringValues(null);
				}
			} catch (Exception t) {
				// Nothing to do
			}
		}

		sessionFactory.getCurrentSession().saveOrUpdate(entity);
	}

	protected void flush() {
		try {
			sessionFactory.getCurrentSession().flush();
		} catch (Exception e) {
			// Nothing to do

		}
	}

	protected void refresh(Object entity) {
		if (entity == null)
			return;

		try {
			if (!sessionFactory.getCurrentSession().contains(entity)) {
				sessionFactory.getCurrentSession().refresh(entity);
			}
		} catch (Exception e) {
			// Nothing to do
		}
	}

	protected Object merge(Object entity) {
		try {
			return sessionFactory.getCurrentSession().merge(entity);
		} catch (Exception t) {
			log.error(t.getMessage(), t);
			return null;
		}
	}

	protected void evict(Object entity) {
		sessionFactory.getCurrentSession().evict(entity);
	}

	/**
	 * Utility method useful for preparing an Hibernate query for generic result
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	protected Query<Object[]> prepareQuery(String expression, Map<String, Object> values, Integer max) {
		Query<Object[]> queryObject = sessionFactory.getCurrentSession().createQuery(expression, Object[].class);
		applyParametersAndLimit(values, max, queryObject);
		return queryObject;
	}

	/**
	 * Utility method useful for preparing an Hibernate query for returning a
	 * long
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	protected Query<Long> prepareQueryForLong(String expression, Map<String, Object> values, Integer max) {
		Query<Long> queryObject = sessionFactory.getCurrentSession().createQuery(expression, Long.class);
		applyParametersAndLimit(values, max, queryObject);
		return queryObject;
	}

	/**
	 * Utility method useful for preparing an Hibernate query for objects of
	 * this type
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	protected Query<T> prepareQueryForObject(String expression, Map<String, Object> values, Integer max) {
		Query<T> queryObject = sessionFactory.getCurrentSession().createQuery(expression, entityClass);
		applyParametersAndLimit(values, max, queryObject);
		return queryObject;
	}

	/**
	 * Utility method useful for preparing an Hibernate query for updates
	 * 
	 * @param expression The expression for the query
	 * @param values The parameters values to be used (optional, if the query is
	 *        parametric)
	 * @param max Optional maximum number of wanted results
	 * 
	 * @return The Hibernate query
	 */
	@SuppressWarnings("rawtypes")
	protected Query prepareQueryForUpdate(String expression, Map<String, Object> values, Integer max) {
		Query queryObject = sessionFactory.getCurrentSession().createQuery(expression);
		applyParametersAndLimit(values, max, queryObject);
		return queryObject;
	}

	private void applyParametersAndLimit(Map<String, Object> parameters, Integer max, @SuppressWarnings("rawtypes")
	Query queryObject) {
		if (parameters != null)
			for (Map.Entry<String, Object> entry : parameters.entrySet())
				queryObject.setParameter(entry.getKey(), entry.getValue());

		if (max != null && max > 0)
			queryObject.setMaxResults(max);
	}

	/**
	 * Doesn't do anything by default
	 */
	@Override
	public void initialize(T entity) {
		// By default do nothing
	}

	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	@Override
	public <P> List<P> query(String sql, RowMapper<P> rowMapper, Integer maxRows) throws PersistenceException {
		return query(sql, null, rowMapper, maxRows);
	}

	@Override
	public <P> List<P> query(String sql, Map<String, Object> parameters, RowMapper<P> rowMapper, Integer maxRows)
			throws PersistenceException {

		return getCurrentSession().doReturningWork(connection -> {
			List<P> result = new ArrayList<>();
			try (NamedParameterStatement stmt = new NamedParameterStatement(connection, sql, parameters, maxRows);
					ResultSet rs = stmt.executeQuery();) {
				if (log.isDebugEnabled())
					log.debug("Run statement {} in connection {}", sql, connection);

				int i = 0;
				while (rs.next())
					result.add(rowMapper.mapRow(rs, i++));
				return result;
			} catch (Exception e) {
				throw new PersistenceException(e.getMessage(), e);
			}
		});
	}

	@Override
	public <R> List<R> queryForList(String sql, Class<R> requiredType) throws PersistenceException {
		return queryForList(sql, requiredType, null);
	}

	@Override
	public <R> List<R> queryForList(String sql, Class<R> requiredType, Integer maxRows) throws PersistenceException {
		return queryForList(sql, null, requiredType, maxRows);
	}

	@Override
	public <R> List<R> queryForList(String sql, Map<String, Object> parameters, Class<R> requiredType, Integer maxRows)
			throws PersistenceException {

		return getCurrentSession().doReturningWork(connection -> {
			List<R> result = new ArrayList<>();
			try (NamedParameterStatement stmt = new NamedParameterStatement(connection, sql, parameters, maxRows);
					ResultSet rs = stmt.executeQuery();) {
				if (log.isDebugEnabled())
					log.debug("Run statement {} in connection {}", sql, connection);

				while (rs.next()) {
					Object obj = rs.getObject(1);
					if (obj == null) {
						result.add(null);
					} else {
						if (!rs.getObject(1).getClass().equals(requiredType)) {
							log.debug("Retrieved object {} differs from attended {}", obj.getClass(), requiredType);
							if (obj instanceof Timestamp ts && requiredType.equals(Date.class)) {
								result.add(requiredType.getConstructor(Long.TYPE).newInstance(ts.getTime()));
							} else {
								result.add(rs.getObject(1, requiredType));
							}
						} else {
							result.add(rs.getObject(1, requiredType));
						}
					}
				}
				return result;
			} catch (Exception e) {
				throw new PersistenceException(e.getMessage(), e);
			}
		});
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Integer maxRows) throws PersistenceException {
		try {
			SqlRowSet rs = null;
			DataSource dataSource = (DataSource) Context.get().getBean(DATA_SOURCE);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			if (maxRows != null)
				jdbcTemplate.setMaxRows(maxRows);
			rs = jdbcTemplate.queryForRowSet(insertTopClause(sql, maxRows));
			return new SqlRowSetWrapper(rs);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Map<String, Object> parameters, Integer maxRows)
			throws PersistenceException {
		if (MapUtils.isEmpty(parameters))
			return queryForRowSet(sql, maxRows);

		try {
			SqlRowSet rs = null;
			DataSource dataSource = (DataSource) Context.get().getBean(DATA_SOURCE);
			NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
			rs = jdbcTemplate.queryForRowSet(insertTopClause(sql, maxRows), parameters);
			return new SqlRowSetWrapper(rs);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}
	
	/**
	 * Parses a SQL query and inserts the hits to the SQL processor to restrict
	 * the maximum number of returned records. The syntax varies depending on
	 * the current DBMS.
	 * 
	 * @param srcQuery The source query to parse
	 * @param maxRows Max number of rows.
	 * @return The modified qery
	 */
	private String insertTopClause(String srcQuery, Integer maxRows) {
		if (maxRows == null || maxRows.intValue() <= 0)
			return srcQuery;

		String outQuery = srcQuery;
		if (isMySQL() || isPostgreSQL()) {
			/*
			 * At the end of the query we have to insert the LIMIT clause:
			 * 
			 * SELECT column_name(s) FROM table_name WHERE condition LIMIT
			 * number;
			 */
			if (srcQuery.endsWith(";"))
				outQuery = srcQuery.substring(0, srcQuery.length() - 1);
			outQuery += " LIMIT " + maxRows;
		} else if (isSqlServer()) {
			/*
			 * After the SELECT have to put the TOP clause:
			 * 
			 * SELECT TOP number column_name(s) FROM table_name WHERE condition;
			 */
			if (srcQuery.startsWith("SELECT"))
				outQuery = outQuery.replaceFirst("SELECT", "SELECT TOP " + maxRows + " ");
			else if (srcQuery.startsWith("select"))
				outQuery = outQuery.replaceFirst("select", "select TOP " + maxRows + " ");
		} else if (isOracle()) {
			/*
			 * In the WHERE we have to put the ROWNUM condition:
			 * 
			 * SELECT column_name(s) FROM table_name WHERE ROWNUM <= number;
			 */
			if (srcQuery.contains("WHERE"))
				outQuery = outQuery.replaceFirst("WHERE", "where ROWNUM <= " + maxRows + " and ");
			if (srcQuery.contains("where"))
				outQuery = outQuery.replaceFirst("where", "where ROWNUM <= " + maxRows + " and ");
		}

		return outQuery;
	}
	
//	@Override
//	public SqlRowSet queryForRowSet(String sql, Integer maxRows) throws PersistenceException {
//		return queryForRowSet(sql, null, maxRows);
//	}
//
//	@Override
//	public SqlRowSet queryForRowSet(String sql, Map<String, Object> parameters, Integer maxRows)
//			throws PersistenceException {
//
//		return getCurrentSession().doReturningWork(new ReturningWork<SqlRowSet>() {
//
//			@Override
//			public SqlRowSet execute(Connection connection) throws SQLException {
//					@SuppressWarnings("resource")
//					NamedParameterStatement stmt = new NamedParameterStatement(connection, sql, parameters,
//							maxRows);
//					if (log.isDebugEnabled())
//						log.debug("Run statement {} in connection {}", sql, connection);
//					return new ResultSetWrappingSqlRowSet(stmt.executeQuery());
//			}
//		});
//	}

	@Override
	public int queryForInt(String sql) throws PersistenceException {
		return (int) queryForLong(sql);
	}

	@Override
	public int queryForInt(String sql, Map<String, Object> parameters) throws PersistenceException {
		return (int) queryForLong(sql, parameters);
	}

	@Override
	public long queryForLong(String sql) throws PersistenceException {
		return queryForLong(sql, null);
	}

	@Override
	public long queryForLong(String sql, Map<String, Object> parameters) throws PersistenceException {
		Long ret = queryForObject(sql, parameters, Long.class);
		return ret = ret != null ? ret.longValue() : 0L;
	}

	@Override
	public double queryForDouble(String sql) throws PersistenceException {
		return queryForDouble(sql, null);
	}

	@Override
	public double queryForDouble(String sql, Map<String, Object> parameters) throws PersistenceException {
		Double ret = queryForObject(sql, parameters, Double.class);
		return ret = ret != null ? ret.doubleValue() : 0D;
	}

	@Override
	public String queryForString(String sql) throws PersistenceException {
		return queryForObject(sql, String.class);
	}

	@Override
	public <R> R queryForObject(String sql, Class<R> requiredType) throws PersistenceException {
		return queryForObject(sql, null, requiredType);
	}

	@Override
	public <R> R queryForObject(String sql, Map<String, Object> parameters, Class<R> requiredType)
			throws PersistenceException {
		return getCurrentSession().doReturningWork(connection -> {
			try (NamedParameterStatement stmt = new NamedParameterStatement(connection, sql, parameters);
					ResultSet rs = stmt.executeQuery();) {
				if (log.isDebugEnabled())
					log.debug("Run statement {} in connection {}", sql, connection);

				if (rs.next())
					return rs.getObject(1, requiredType);
				else
					return null;
			} catch (Exception e) {
				throw new PersistenceException(e.getMessage(), e);
			}
		});
	}

	@Override
	public int jdbcUpdate(String sql, Map<String, Object> parameters) throws PersistenceException {
		if (!checkStoringAspect())
			return 0;

		return getCurrentSession().doReturningWork(connection -> {
			try (NamedParameterStatement stmt = new NamedParameterStatement(connection, sql, parameters);) {
				if (log.isDebugEnabled())
					log.debug("Run statement {} in connection {}", sql, connection);

				return stmt.executeUpdate();
			} catch (Exception e) {
				throw new PersistenceException(e.getMessage(), e);
			}
		});
	}

	@Override
	public int jdbcUpdate(String sql) throws PersistenceException {
		return jdbcUpdate(sql, null);
	}

	@Override
	public void deleteAll(Collection<T> entities, int code) throws PersistenceException {
		if (!checkStoringAspect())
			return;

		if (entities == null || entities.isEmpty())
			return;

		try {
			StringBuilder ids = new StringBuilder();
			for (T t : entities) {
				if (ids.length() > 0)
					ids.append(",");
				ids.append(Long.toString(t.getId()));
			}

			Query<T> queryObject = prepareQueryForObject(UPDATE + entityClass.getCanonicalName() + " set deleted="
					+ code + " where id in(" + ids.toString() + ")", (Map<String, Object>) null, null);
			queryObject.executeUpdate();
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public void deleteAll(Collection<T> entities) throws PersistenceException {
		deleteAll(entities, PersistentObject.DELETED_CODE_DEFAULT);
	}

	@Override
	public int bulkUpdate(String expression, Map<String, Object> parameters) throws PersistenceException {
		if (!checkStoringAspect())
			return 0;

		try {
			return prepareQueryForUpdate(UPDATE + entityClass.getCanonicalName() + " " + expression, parameters, null)
					.executeUpdate();
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	protected Connection getConnection() throws PersistenceException {
		DataSource dataSource = (DataSource) Context.get().getBean(DATA_SOURCE);
		try {
			return dataSource.getConnection();
		} catch (Exception e) {
			throw new PersistenceException(e.getMessage(), e);
		}
	}

	@Override
	public String getDbms() {
		ContextProperties config = Context.get().getProperties();
		return config.getProperty("jdbc.dbms", "mysql").toLowerCase();
	}

	@Override
	public boolean isOracle() {
		return "oracle".equals(getDbms());
	}

	protected boolean isHsql() {
		return "hsqldb".equals(getDbms());
	}

	@Override
	public boolean isMySQL() {
		return "mysql".equals(getDbms()) || isMariaDB();
	}

	protected boolean isMariaDB() {
		return "maria".equals(getDbms());
	}

	protected boolean isPostgreSQL() {
		return "postgresql".equals(getDbms());
	}

	protected boolean isSqlServer() {
		return "mssql".equals(getDbms());
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * A generic method to logically delete old records of a table, useful for
	 * some DAO implementations. This method uses JDBC directly. The processed
	 * table must provide the ld_deleted and ld_lastmodified columns. The
	 * dateColumn is the column used to validate the ttl
	 * 
	 * 
	 * @param ttl number of retention days
	 * @param tableName name of the table to process
	 * @param dateColumn
	 * 
	 * @throws PersistenceException error at database level
	 */
	protected int cleanOldRecords(int ttl, String tableName, String dateColumn) throws PersistenceException {
		int updates = 0;
		if (ttl > 0) {
			Date today = new Date();
			GregorianCalendar cal = new GregorianCalendar();
			cal.add(Calendar.DAY_OF_MONTH, -ttl);
			Date ldDate = cal.getTime();

			Map<String, Object> params = new HashMap<>();
			params.put("today", today);
			params.put("ldDate", ldDate);

			updates = jdbcUpdate("UPDATE " + tableName + " SET ld_deleted = 1, ld_lastmodified = :today"
					+ " WHERE ld_deleted = 0 AND " + dateColumn + " < :ldDate", params);

			log.info("Removed {} old rows from table {}", updates, tableName);
		}
		return updates;
	}

	/**
	 * A short cut for {@link #cleanOldRecords(int, String, String)} that passes
	 * the dateColumn="ld_date"
	 * 
	 * 
	 * @param ttl number of retention days
	 * @param tableName name of the table to process
	 * 
	 * @throws PersistenceException error at database level
	 */
	protected int cleanOldRecords(int ttl, String tableName) throws PersistenceException {
		return cleanOldRecords(ttl, tableName, "ld_date");
	}

	@Override
	public Map<String, String> getDatabaseMetadata() {
		Map<String, String> map = new HashMap<>();
		DataSource dataSource = (DataSource) Context.get().getBean(DATA_SOURCE);
		try (Connection connection = dataSource.getConnection();) {
			DatabaseMetaData meta = connection.getMetaData();
			map.put("db.product.name", meta.getDatabaseProductName());
			map.put("db.product.version", meta.getDatabaseProductVersion());
			map.put("db.minorversion", Integer.toString(meta.getDatabaseMinorVersion()));
			map.put("db.majorversion", Integer.toString(meta.getDatabaseMajorVersion()));
			map.put("db.driver.name", meta.getDriverName());
			map.put("db.driver.version", meta.getDriverVersion());
			map.put("db.driver.minorversion", Integer.toString(meta.getDriverMinorVersion()));
			map.put("db.driver.majorversion", Integer.toString(meta.getDriverMajorVersion()));
			map.put("db.jdbc.majorversion", Integer.toString(meta.getJDBCMajorVersion()));
			map.put("db.jdbc.minorversion", Integer.toString(meta.getJDBCMinorVersion()));
			map.put("db.catalog.term", meta.getCatalogTerm());
			map.put("db.catalog.separator", meta.getCatalogSeparator());
			map.put("db.schema.term", meta.getSchemaTerm());
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
		return map;
	}
}