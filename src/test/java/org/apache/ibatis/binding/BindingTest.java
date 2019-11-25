/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.binding;

import static com.googlecode.catchexception.apis.BDDCatchException.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javassist.util.proxy.Proxy;

import javax.sql.DataSource;

import net.sf.cglib.proxy.Factory;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Blog;
import org.apache.ibatis.domain.blog.DraftPost;
import org.apache.ibatis.domain.blog.Post;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class BindingTest {
  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setup() throws Exception {
    DataSource dataSource = BaseDataTest.createBlogDataSource();
    BaseDataTest.runScript(dataSource, BaseDataTest.BLOG_DDL);
    BaseDataTest.runScript(dataSource, BaseDataTest.BLOG_DATA);
    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    Environment environment = new Environment("Production", transactionFactory, dataSource);
    // 上面的代码是初始化数据库和条件
    // 而configuration在实例化过程中，会设置一些初始化变量，如下：
    // configuration = {Configuration@3178}
    // environment = {Environment@3177}
    //  id = "Production"
    //  transactionFactory = {JdbcTransactionFactory@3176}
    //  dataSource = {UnpooledDataSource@3175}
    // safeRowBoundsEnabled = false
    // safeResultHandlerEnabled = true
    // mapUnderscoreToCamelCase = false
    // aggressiveLazyLoading = false
    // multipleResultSetsEnabled = true
    // useGeneratedKeys = false
    // useColumnLabel = true
    // cacheEnabled = true
    // callSettersOnNulls = false
    // useActualParamName = true
    // returnInstanceForEmptyRow = false
    // logPrefix = null
    // logImpl = null
    // vfsImpl = null
    // localCacheScope = {LocalCacheScope@3179} "SESSION"
    // jdbcTypeForNull = {JdbcType@3180} "OTHER"
    // lazyLoadTriggerMethods = {HashSet@3181}  size = 4
    //  0 = "hashCode"
    //  1 = "equals"
    //  2 = "clone"
    //  3 = "toString"
    // defaultStatementTimeout = null
    // defaultFetchSize = null
    // defaultResultSetType = null
    // defaultExecutorType = {ExecutorType@3182} "SIMPLE"
    // autoMappingBehavior = {AutoMappingBehavior@3183} "PARTIAL"
    // autoMappingUnknownColumnBehavior = {AutoMappingUnknownColumnBehavior$1@3184} "NONE"
    // variables = {Properties@3185}  size = 0
    // reflectorFactory = {DefaultReflectorFactory@3186}
    // objectFactory = {DefaultObjectFactory@3187}
    // objectWrapperFactory = {DefaultObjectWrapperFactory@3188}
    // lazyLoadingEnabled = false
    // proxyFactory = {JavassistProxyFactory@3189}
    // databaseId = null
    // configurationFactory = null
    // mapperRegistry = {MapperRegistry@3190}
    // interceptorChain = {InterceptorChain@3191}
    // typeHandlerRegistry = {TypeHandlerRegistry@3192}
    //  jdbcTypeHandlerMap = {EnumMap@3221}  size = 25
    //   0 = {AbstractMap$SimpleEntry@3232} "ARRAY" -> "class java.lang.Object"
    //   1 = {AbstractMap$SimpleEntry@3233} "BIT" -> "class java.lang.Boolean"
    //   2 = {AbstractMap$SimpleEntry@3234} "TINYINT" -> "class java.lang.Byte"
    //   3 = {AbstractMap$SimpleEntry@3235} "SMALLINT" -> "class java.lang.Short"
    //   4 = {AbstractMap$SimpleEntry@3236} "INTEGER" -> "class java.lang.Integer"
    //   5 = {AbstractMap$SimpleEntry@3237} "BIGINT" -> "class java.lang.Long"
    //   6 = {AbstractMap$SimpleEntry@3238} "FLOAT" -> "class java.lang.Float"
    //   7 = {AbstractMap$SimpleEntry@3239} "REAL" -> "class java.math.BigDecimal"
    //   8 = {AbstractMap$SimpleEntry@3240} "DOUBLE" -> "class java.lang.Double"
    //   9 = {AbstractMap$SimpleEntry@3241} "NUMERIC" -> "class java.math.BigDecimal"
    //   10 = {AbstractMap$SimpleEntry@3242} "DECIMAL" -> "class java.math.BigDecimal"
    //   11 = {AbstractMap$SimpleEntry@3243} "CHAR" -> "class java.lang.String"
    //   12 = {AbstractMap$SimpleEntry@3244} "VARCHAR" -> "class java.lang.String"
    //   13 = {AbstractMap$SimpleEntry@3245} "LONGVARCHAR" -> "class java.lang.String"
    //   14 = {AbstractMap$SimpleEntry@3246} "DATE" -> "class java.util.Date"
    //   15 = {AbstractMap$SimpleEntry@3247} "TIME" -> "class java.util.Date"
    //   16 = {AbstractMap$SimpleEntry@3248} "TIMESTAMP" -> "class java.util.Date"
    //   17 = {AbstractMap$SimpleEntry@3249} "LONGVARBINARY" -> "class [B"
    //   18 = {AbstractMap$SimpleEntry@3250} "OTHER" -> "class java.lang.Object"
    //   19 = {AbstractMap$SimpleEntry@3251} "BLOB" -> "class [B"
    //   20 = {AbstractMap$SimpleEntry@3252} "CLOB" -> "class java.lang.String"
    //   21 = {AbstractMap$SimpleEntry@3253} "BOOLEAN" -> "class java.lang.Boolean"
    //   22 = {AbstractMap$SimpleEntry@3254} "NVARCHAR" -> "class java.lang.String"
    //   23 = {AbstractMap$SimpleEntry@3255} "NCHAR" -> "class java.lang.String"
    //   24 = {AbstractMap$SimpleEntry@3256} "NCLOB" -> "class java.lang.String"
    //  typeHandlerMap = {ConcurrentHashMap@3222}  size = 39
    //   0 = {ConcurrentHashMap$MapEntry@3359} "byte" -> " size = 1"
    //   1 = {ConcurrentHashMap$MapEntry@3360} "class java.lang.Object" -> " size = 3"
    //   2 = {ConcurrentHashMap$MapEntry@3361} "class java.time.YearMonth" -> " size = 1"
    //   3 = {ConcurrentHashMap$MapEntry@3362} "class java.time.Instant" -> " size = 1"
    //   4 = {ConcurrentHashMap$MapEntry@3363} "class java.math.BigDecimal" -> " size = 1"
    //   5 = {ConcurrentHashMap$MapEntry@3364} "class java.lang.Long" -> " size = 1"
    //   6 = {ConcurrentHashMap$MapEntry@3365} "class java.lang.Double" -> " size = 1"
    //   7 = {ConcurrentHashMap$MapEntry@3366} "class java.time.OffsetTime" -> " size = 1"
    //   8 = {ConcurrentHashMap$MapEntry@3367} "class java.time.LocalDate" -> " size = 1"
    //   9 = {ConcurrentHashMap$MapEntry@3368} "class java.time.Month" -> " size = 1"
    //   10 = {ConcurrentHashMap$MapEntry@3369} "class java.time.ZonedDateTime" -> " size = 1"
    //   11 = {ConcurrentHashMap$MapEntry@3370} "char" -> " size = 1"
    //   12 = {ConcurrentHashMap$MapEntry@3371} "class java.lang.Short" -> " size = 1"
    //   13 = {ConcurrentHashMap$MapEntry@3372} "class [Ljava.lang.Byte;" -> " size = 3"
    //   14 = {ConcurrentHashMap$MapEntry@3373} "class java.sql.Timestamp" -> " size = 1"
    //   15 = {ConcurrentHashMap$MapEntry@3374} "class java.time.LocalTime" -> " size = 1"
    //   16 = {ConcurrentHashMap$MapEntry@3375} "class java.lang.Integer" -> " size = 1"
    //   17 = {ConcurrentHashMap$MapEntry@3376} "int" -> " size = 1"
    //   18 = {ConcurrentHashMap$MapEntry@3377} "class java.sql.Time" -> " size = 1"
    //   19 = {ConcurrentHashMap$MapEntry@3378} "class java.lang.Boolean" -> " size = 1"
    //   20 = {ConcurrentHashMap$MapEntry@3379} "class java.time.OffsetDateTime" -> " size = 1"
    //   21 = {ConcurrentHashMap$MapEntry@3380} "short" -> " size = 1"
    //   22 = {ConcurrentHashMap$MapEntry@3381} "class java.time.LocalDateTime" -> " size = 1"
    //   23 = {ConcurrentHashMap$MapEntry@3382} "class java.math.BigInteger" -> " size = 1"
    //   24 = {ConcurrentHashMap$MapEntry@3383} "class java.util.Date" -> " size = 3"
    //   25 = {ConcurrentHashMap$MapEntry@3384} "class java.sql.Date" -> " size = 1"
    //   26 = {ConcurrentHashMap$MapEntry@3385} "class java.lang.Byte" -> " size = 1"
    //   27 = {ConcurrentHashMap$MapEntry@3386} "long" -> " size = 1"
    //   28 = {ConcurrentHashMap$MapEntry@3387} "class java.lang.Float" -> " size = 1"
    //   29 = {ConcurrentHashMap$MapEntry@3388} "class java.time.Year" -> " size = 1"
    //   30 = {ConcurrentHashMap$MapEntry@3389} "double" -> " size = 1"
    //   31 = {ConcurrentHashMap$MapEntry@3390} "class java.io.Reader" -> " size = 1"
    //   32 = {ConcurrentHashMap$MapEntry@3391} "class java.lang.Character" -> " size = 1"
    //   33 = {ConcurrentHashMap$MapEntry@3392} "float" -> " size = 1"
    //   34 = {ConcurrentHashMap$MapEntry@3393} "class java.lang.String" -> " size = 9"
    //   35 = {ConcurrentHashMap$MapEntry@3394} "class [B" -> " size = 3"
    //   36 = {ConcurrentHashMap$MapEntry@3395} "class java.time.chrono.JapaneseDate" -> " size = 1"
    //   37 = {ConcurrentHashMap$MapEntry@3396} "boolean" -> " size = 1"
    //   38 = {ConcurrentHashMap$MapEntry@3397} "class java.io.InputStream" -> " size = 1"
    //  unknownTypeHandler = {UnknownTypeHandler@3223} "class java.lang.Object"
    //  allTypeHandlersMap = {HashMap@3224}  size = 40
    //   0 = {HashMap$Node@3500} "class org.apache.ibatis.type.BigDecimalTypeHandler" -> "class java.math.BigDecimal"
    //   1 = {HashMap$Node@3501} "class org.apache.ibatis.type.InstantTypeHandler" -> "class java.time.Instant"
    //   2 = {HashMap$Node@3502} "class org.apache.ibatis.type.SqlDateTypeHandler" -> "class java.sql.Date"
    //   3 = {HashMap$Node@3503} "class org.apache.ibatis.type.FloatTypeHandler" -> "class java.lang.Float"
    //   4 = {HashMap$Node@3504} "class org.apache.ibatis.type.ByteObjectArrayTypeHandler" -> "class [Ljava.lang.Byte;"
    //   5 = {HashMap$Node@3505} "class org.apache.ibatis.type.UnknownTypeHandler" -> "class java.lang.Object"
    //   6 = {HashMap$Node@3506} "class org.apache.ibatis.type.IntegerTypeHandler" -> "class java.lang.Integer"
    //   7 = {HashMap$Node@3507} "class org.apache.ibatis.type.LocalDateTimeTypeHandler" -> "class java.time.LocalDateTime"
    //   8 = {HashMap$Node@3508} "class org.apache.ibatis.type.LocalTimeTypeHandler" -> "class java.time.LocalTime"
    //   9 = {HashMap$Node@3509} "class org.apache.ibatis.type.ByteTypeHandler" -> "class java.lang.Byte"
    //   10 = {HashMap$Node@3510} "class org.apache.ibatis.type.NClobTypeHandler" -> "class java.lang.String"
    //   11 = {HashMap$Node@3511} "class org.apache.ibatis.type.DateTypeHandler" -> "class java.util.Date"
    //   12 = {HashMap$Node@3512} "class org.apache.ibatis.type.YearMonthTypeHandler" -> "class java.time.YearMonth"
    //   13 = {HashMap$Node@3513} "class org.apache.ibatis.type.BigIntegerTypeHandler" -> "class java.math.BigInteger"
    //   14 = {HashMap$Node@3514} "class org.apache.ibatis.type.BlobInputStreamTypeHandler" -> "class java.io.InputStream"
    //   15 = {HashMap$Node@3515} "class org.apache.ibatis.type.ClobTypeHandler" -> "class java.lang.String"
    //   16 = {HashMap$Node@3516} "class org.apache.ibatis.type.LongTypeHandler" -> "class java.lang.Long"
    //   17 = {HashMap$Node@3517} "class org.apache.ibatis.type.ByteArrayTypeHandler" -> "class [B"
    //   18 = {HashMap$Node@3518} "class org.apache.ibatis.type.SqlTimeTypeHandler" -> "class java.sql.Time"
    //   19 = {HashMap$Node@3519} "class org.apache.ibatis.type.MonthTypeHandler" -> "class java.time.Month"
    //   20 = {HashMap$Node@3520} "class org.apache.ibatis.type.YearTypeHandler" -> "class java.time.Year"
    //   21 = {HashMap$Node@3521} "class org.apache.ibatis.type.BlobByteObjectArrayTypeHandler" -> "class [Ljava.lang.Byte;"
    //   22 = {HashMap$Node@3522} "class org.apache.ibatis.type.ShortTypeHandler" -> "class java.lang.Short"
    //   23 = {HashMap$Node@3523} "class org.apache.ibatis.type.ZonedDateTimeTypeHandler" -> "class java.time.ZonedDateTime"
    //   24 = {HashMap$Node@3524} "class org.apache.ibatis.type.CharacterTypeHandler" -> "class java.lang.Character"
    //   25 = {HashMap$Node@3525} "class org.apache.ibatis.type.NStringTypeHandler" -> "class java.lang.String"
    //   26 = {HashMap$Node@3526} "class org.apache.ibatis.type.DoubleTypeHandler" -> "class java.lang.Double"
    //   27 = {HashMap$Node@3527} "class org.apache.ibatis.type.ClobReaderTypeHandler" -> "class java.io.Reader"
    //   28 = {HashMap$Node@3528} "class org.apache.ibatis.type.ArrayTypeHandler" -> "class java.lang.Object"
    //   29 = {HashMap$Node@3529} "class org.apache.ibatis.type.StringTypeHandler" -> "class java.lang.String"
    //   30 = {HashMap$Node@3530} "class org.apache.ibatis.type.SqlxmlTypeHandler" -> "class java.lang.String"
    //   31 = {HashMap$Node@3531} "class org.apache.ibatis.type.OffsetDateTimeTypeHandler" -> "class java.time.OffsetDateTime"
    //   32 = {HashMap$Node@3532} "class org.apache.ibatis.type.LocalDateTypeHandler" -> "class java.time.LocalDate"
    //   33 = {HashMap$Node@3533} "class org.apache.ibatis.type.BooleanTypeHandler" -> "class java.lang.Boolean"
    //   34 = {HashMap$Node@3534} "class org.apache.ibatis.type.DateOnlyTypeHandler" -> "class java.util.Date"
    //   35 = {HashMap$Node@3535} "class org.apache.ibatis.type.BlobTypeHandler" -> "class [B"
    //   36 = {HashMap$Node@3536} "class org.apache.ibatis.type.OffsetTimeTypeHandler" -> "class java.time.OffsetTime"
    //   37 = {HashMap$Node@3537} "class org.apache.ibatis.type.JapaneseDateTypeHandler" -> "class java.time.chrono.JapaneseDate"
    //   38 = {HashMap$Node@3538} "class org.apache.ibatis.type.TimeOnlyTypeHandler" -> "class java.util.Date"
    //   39 = {HashMap$Node@3539} "class org.apache.ibatis.type.SqlTimestampTypeHandler" -> "class java.sql.Timestamp"
    //  defaultEnumTypeHandler = {Class@3225} "class org.apache.ibatis.type.EnumTypeHandler"
    // typeAliasRegistry = {TypeAliasRegistry@3193}
    //  typeAliases = {HashMap@3659}  size = 72
    //   0 = {HashMap$Node@3662} "date" -> "class java.util.Date"
    //   1 = {HashMap$Node@3663} "_boolean" -> "boolean"
    //   2 = {HashMap$Node@3664} "cglib" -> "class org.apache.ibatis.executor.loader.cglib.CglibProxyFactory"
    //   3 = {HashMap$Node@3665} "_byte[]" -> "class [B"
    //   4 = {HashMap$Node@3666} "_int[]" -> "class [I"
    //   5 = {HashMap$Node@3667} "object[]" -> "class [Ljava.lang.Object;"
    //   6 = {HashMap$Node@3668} "decimal[]" -> "class [Ljava.math.BigDecimal;"
    //   7 = {HashMap$Node@3669} "integer" -> "class java.lang.Integer"
    //   8 = {HashMap$Node@3670} "float" -> "class java.lang.Float"
    //   9 = {HashMap$Node@3671} "perpetual" -> "class org.apache.ibatis.cache.impl.PerpetualCache"
    //   10 = {HashMap$Node@3672} "_byte" -> "byte"
    //   11 = {HashMap$Node@3673} "iterator" -> "interface java.util.Iterator"
    //   12 = {HashMap$Node@3674} "biginteger[]" -> "class [Ljava.math.BigInteger;"
    //   13 = {HashMap$Node@3675} "xml" -> "class org.apache.ibatis.scripting.xmltags.XMLLanguageDriver"
    //   14 = {HashMap$Node@3676} "_double" -> "double"
    //   15 = {HashMap$Node@3677} "_int" -> "int"
    //   16 = {HashMap$Node@3678} "hashmap" -> "class java.util.HashMap"
    //   17 = {HashMap$Node@3679} "_float[]" -> "class [F"
    //   18 = {HashMap$Node@3680} "soft" -> "class org.apache.ibatis.cache.decorators.SoftCache"
    //   19 = {HashMap$Node@3681} "javassist" -> "class org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory"
    //   20 = {HashMap$Node@3682} "date[]" -> "class [Ljava.util.Date;"
    //   21 = {HashMap$Node@3683} "bigdecimal[]" -> "class [Ljava.math.BigDecimal;"
    //   22 = {HashMap$Node@3684} "slf4j" -> "class org.apache.ibatis.logging.slf4j.Slf4jImpl"
    //   23 = {HashMap$Node@3685} "byte" -> "class java.lang.Byte"
    //   24 = {HashMap$Node@3686} "double" -> "class java.lang.Double"
    //   25 = {HashMap$Node@3687} "resultset" -> "interface java.sql.ResultSet"
    //   26 = {HashMap$Node@3688} "raw" -> "class org.apache.ibatis.scripting.defaults.RawLanguageDriver"
    //   27 = {HashMap$Node@3689} "collection" -> "interface java.util.Collection"
    //   28 = {HashMap$Node@3690} "list" -> "interface java.util.List"
    //   29 = {HashMap$Node@3691} "lru" -> "class org.apache.ibatis.cache.decorators.LruCache"
    //   30 = {HashMap$Node@3692} "_float" -> "float"
    //   31 = {HashMap$Node@3693} "_long" -> "long"
    //   32 = {HashMap$Node@3694} "_integer" -> "int"
    //   33 = {HashMap$Node@3695} "_integer[]" -> "class [I"
    //   34 = {HashMap$Node@3696} "boolean[]" -> "class [Ljava.lang.Boolean;"
    //   35 = {HashMap$Node@3697} "decimal" -> "class java.math.BigDecimal"
    //   36 = {HashMap$Node@3698} "_double[]" -> "class [D"
    //   37 = {HashMap$Node@3699} "object" -> "class java.lang.Object"
    //   38 = {HashMap$Node@3700} "biginteger" -> "class java.math.BigInteger"
    //   39 = {HashMap$Node@3701} "string" -> "class java.lang.String"
    //   40 = {HashMap$Node@3702} "long[]" -> "class [Ljava.lang.Long;"
    //   41 = {HashMap$Node@3703} "jdbc" -> "class org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory"
    //   42 = {HashMap$Node@3704} "long" -> "class java.lang.Long"
    //   43 = {HashMap$Node@3705} "weak" -> "class org.apache.ibatis.cache.decorators.WeakCache"
    //   44 = {HashMap$Node@3706} "no_logging" -> "class org.apache.ibatis.logging.nologging.NoLoggingImpl"
    //   45 = {HashMap$Node@3707} "unpooled" -> "class org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory"
    //   46 = {HashMap$Node@3708} "pooled" -> "class org.apache.ibatis.datasource.pooled.PooledDataSourceFactory"
    //   47 = {HashMap$Node@3709} "db_vendor" -> "class org.apache.ibatis.mapping.VendorDatabaseIdProvider"
    //   48 = {HashMap$Node@3710} "managed" -> "class org.apache.ibatis.transaction.managed.ManagedTransactionFactory"
    //   49 = {HashMap$Node@3711} "commons_logging" -> "class org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl"
    //   50 = {HashMap$Node@3712} "_short[]" -> "class [S"
    //   51 = {HashMap$Node@3713} "_short" -> "short"
    //   52 = {HashMap$Node@3714} "map" -> "interface java.util.Map"
    //   53 = {HashMap$Node@3715} "log4j" -> "class org.apache.ibatis.logging.log4j.Log4jImpl"
    //   54 = {HashMap$Node@3716} "jdk_logging" -> "class org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl"
    //   55 = {HashMap$Node@3717} "fifo" -> "class org.apache.ibatis.cache.decorators.FifoCache"
    //   56 = {HashMap$Node@3718} "bigdecimal" -> "class java.math.BigDecimal"
    //   57 = {HashMap$Node@3719} "short[]" -> "class [Ljava.lang.Short;"
    //   58 = {HashMap$Node@3720} "int[]" -> "class [Ljava.lang.Integer;"
    //   59 = {HashMap$Node@3721} "arraylist" -> "class java.util.ArrayList"
    //   60 = {HashMap$Node@3722} "int" -> "class java.lang.Integer"
    //   61 = {HashMap$Node@3723} "float[]" -> "class [Ljava.lang.Float;"
    //   62 = {HashMap$Node@3724} "log4j2" -> "class org.apache.ibatis.logging.log4j2.Log4j2Impl"
    //   63 = {HashMap$Node@3725} "byte[]" -> "class [Ljava.lang.Byte;"
    //   64 = {HashMap$Node@3726} "boolean" -> "class java.lang.Boolean"
    //   65 = {HashMap$Node@3727} "stdout_logging" -> "class org.apache.ibatis.logging.stdout.StdOutImpl"
    //   66 = {HashMap$Node@3728} "double[]" -> "class [Ljava.lang.Double;"
    //   67 = {HashMap$Node@3729} "_long[]" -> "class [J"
    //   68 = {HashMap$Node@3730} "jndi" -> "class org.apache.ibatis.datasource.jndi.JndiDataSourceFactory"
    //   69 = {HashMap$Node@3731} "short" -> "class java.lang.Short"
    //   70 = {HashMap$Node@3732} "_boolean[]" -> "class [Z"
    //   71 = {HashMap$Node@3733} "integer[]" -> "class [Ljava.lang.Integer;"
    // languageRegistry = {LanguageDriverRegistry@3194}
    //  LANGUAGE_DRIVER_MAP = {HashMap@3905}  size = 2
    //   0 = {HashMap$Node@3909} "class org.apache.ibatis.scripting.xmltags.XMLLanguageDriver" ->
    //   1 = {HashMap$Node@3910} "class org.apache.ibatis.scripting.defaults.RawLanguageDriver" ->
    //  defaultDriverClass = {Class@3173} "class org.apache.ibatis.scripting.xmltags.XMLLanguageDriver"
    // mappedStatements = {Configuration$StrictMap@3195}  size = 0
    // caches = {Configuration$StrictMap@3196}  size = 0
    // resultMaps = {Configuration$StrictMap@3197}  size = 0
    // parameterMaps = {Configuration$StrictMap@3198}  size = 0
    // keyGenerators = {Configuration$StrictMap@3199}  size = 0
    // loadedResources = {HashSet@3200}  size = 0
    // sqlFragments = {Configuration$StrictMap@3201}  size = 0
    // incompleteStatements = {LinkedList@3202}  size = 0
    // incompleteCacheRefs = {LinkedList@3203}  size = 0
    // incompleteResultMaps = {LinkedList@3204}  size = 0
    // incompleteMethods = {LinkedList@3205}  size = 0
    // cacheRefMap = {HashMap@3206}  size = 0
    Configuration configuration = new Configuration(environment);
    configuration.setLazyLoadingEnabled(true);
    configuration.setUseActualParamName(false); // to test legacy style reference (#{0} #{1})
    // 注册Blog的别名，就是看下Blog有没有设置别名的注解，没有则用默认的方式注册
    configuration.getTypeAliasRegistry().registerAlias(Blog.class);
    configuration.getTypeAliasRegistry().registerAlias(Post.class);
    configuration.getTypeAliasRegistry().registerAlias(Author.class);

    // 将与xml映射的接口文件放到mapperRegistry中。从此可看出，实体是放到了typeAlias的map中，而映射类放到了mapperRegistry中
    configuration.addMapper(BoundBlogMapper.class);
    configuration.addMapper(BoundAuthorMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  @Test
  void shouldSelectBlogWithPostsUsingSubSelect() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog b = mapper.selectBlogWithPostsUsingSubSelect(1);
      assertEquals(1, b.getId());
      assertNotNull(b.getAuthor());
      assertEquals(101, b.getAuthor().getId());
      assertEquals("jim", b.getAuthor().getUsername());
      assertEquals("********", b.getAuthor().getPassword());
      assertEquals(2, b.getPosts().size());
    }
  }

  @Test
  void shouldFindPostsInList() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      List<Post> posts = mapper.findPostsInList(new ArrayList<Integer>() {{
        add(1);
        add(3);
        add(5);
      }});
      assertEquals(3, posts.size());
      session.rollback();
    }
  }

  @Test
  void shouldFindPostsInArray() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Integer[] params = new Integer[]{1, 3, 5};
      List<Post> posts = mapper.findPostsInArray(params);
      assertEquals(3, posts.size());
      session.rollback();
    }
  }

  @Test
  void shouldFindThreeSpecificPosts() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      List<Post> posts = mapper.findThreeSpecificPosts(1, new RowBounds(1, 1), 3, 5);
      assertEquals(1, posts.size());
      assertEquals(3, posts.get(0).getId());
      session.rollback();
    }
  }

  @Test
  void shouldInsertAuthorWithSelectKey() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
      int rows = mapper.insertAuthor(author);
      assertEquals(1, rows);
      session.rollback();
    }
  }

  @Test
  void verifyErrorMessageFromSelectKey() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      try {
        BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
        Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
        when(() -> mapper.insertAuthorInvalidSelectKey(author));
        then(caughtException()).isInstanceOf(PersistenceException.class).hasMessageContaining(
            "### The error may exist in org/apache/ibatis/binding/BoundAuthorMapper.xml" + System.lineSeparator() +
                "### The error may involve org.apache.ibatis.binding.BoundAuthorMapper.insertAuthorInvalidSelectKey!selectKey" + System.lineSeparator() +
                "### The error occurred while executing a query");
      } finally {
        session.rollback();
      }
    }
  }

  @Test
  void verifyErrorMessageFromInsertAfterSelectKey() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      try {
        BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
        Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
        when(() -> mapper.insertAuthorInvalidInsert(author));
        then(caughtException()).isInstanceOf(PersistenceException.class).hasMessageContaining(
            "### The error may exist in org/apache/ibatis/binding/BoundAuthorMapper.xml" + System.lineSeparator() +
                "### The error may involve org.apache.ibatis.binding.BoundAuthorMapper.insertAuthorInvalidInsert" + System.lineSeparator() +
                "### The error occurred while executing an update");
      } finally {
        session.rollback();
      }
    }
  }

  @Test
  void shouldInsertAuthorWithSelectKeyAndDynamicParams() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
      int rows = mapper.insertAuthorDynamic(author);
      assertEquals(1, rows);
      assertNotEquals(-1, author.getId()); // id must be autogenerated
      Author author2 = mapper.selectAuthor(author.getId());
      assertNotNull(author2);
      assertEquals(author.getEmail(), author2.getEmail());
      session.rollback();
    }
  }

  @Test
  void shouldSelectRandom() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Integer x = mapper.selectRandom();
      assertNotNull(x);
    }
  }

  @Test
  void shouldExecuteBoundSelectListOfBlogsStatement() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Blog> blogs = mapper.selectBlogs();
      assertEquals(2, blogs.size());
    }
  }

  @Test
  void shouldExecuteBoundSelectMapOfBlogsById() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Map<Integer,Blog> blogs = mapper.selectBlogsAsMapById();
      assertEquals(2, blogs.size());
      for(Map.Entry<Integer,Blog> blogEntry : blogs.entrySet()) {
        assertEquals(blogEntry.getKey(), (Integer) blogEntry.getValue().getId());
      }
    }
  }

  @Test
  void shouldExecuteMultipleBoundSelectOfBlogsByIdInWithProvidedResultHandlerBetweenSessions() {
    final DefaultResultHandler handler = new DefaultResultHandler();
    try (SqlSession session = sqlSessionFactory.openSession()) {
      session.select("selectBlogsAsMapById", handler);
    }

    final DefaultResultHandler moreHandler = new DefaultResultHandler();
    try (SqlSession session = sqlSessionFactory.openSession()) {
      session.select("selectBlogsAsMapById", moreHandler);
    }
    assertEquals(2, handler.getResultList().size());
    assertEquals(2, moreHandler.getResultList().size());
  }

  @Test
  void shouldExecuteMultipleBoundSelectOfBlogsByIdInWithProvidedResultHandlerInSameSession() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      final DefaultResultHandler handler = new DefaultResultHandler();
      session.select("selectBlogsAsMapById", handler);

      final DefaultResultHandler moreHandler = new DefaultResultHandler();
      session.select("selectBlogsAsMapById", moreHandler);

      assertEquals(2, handler.getResultList().size());
      assertEquals(2, moreHandler.getResultList().size());
    }
  }

  @Test
  void shouldExecuteMultipleBoundSelectMapOfBlogsByIdInSameSessionWithoutClearingLocalCache() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Map<Integer,Blog> blogs = mapper.selectBlogsAsMapById();
      Map<Integer,Blog> moreBlogs = mapper.selectBlogsAsMapById();
      assertEquals(2, blogs.size());
      assertEquals(2, moreBlogs.size());
      for(Map.Entry<Integer,Blog> blogEntry : blogs.entrySet()) {
        assertEquals(blogEntry.getKey(), (Integer) blogEntry.getValue().getId());
      }
      for(Map.Entry<Integer,Blog> blogEntry : moreBlogs.entrySet()) {
        assertEquals(blogEntry.getKey(), (Integer) blogEntry.getValue().getId());
      }
    }
  }

  @Test
  void shouldExecuteMultipleBoundSelectMapOfBlogsByIdBetweenTwoSessionsWithGlobalCacheEnabled() {
    Map<Integer,Blog> blogs;
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      blogs = mapper.selectBlogsAsMapById();
    }
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Map<Integer,Blog> moreBlogs = mapper.selectBlogsAsMapById();
      assertEquals(2, blogs.size());
      assertEquals(2, moreBlogs.size());
      for(Map.Entry<Integer,Blog> blogEntry : blogs.entrySet()) {
        assertEquals(blogEntry.getKey(), (Integer) blogEntry.getValue().getId());
      }
      for(Map.Entry<Integer,Blog> blogEntry : moreBlogs.entrySet()) {
        assertEquals(blogEntry.getKey(), (Integer) blogEntry.getValue().getId());
      }
    }
  }

  @Test
  void shouldSelectListOfBlogsUsingXMLConfig() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Blog> blogs = mapper.selectBlogsFromXML();
      assertEquals(2, blogs.size());
    }
  }

  @Test
  void shouldExecuteBoundSelectListOfBlogsStatementUsingProvider() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Blog> blogs = mapper.selectBlogsUsingProvider();
      assertEquals(2, blogs.size());
    }
  }

  @Test
  void shouldExecuteBoundSelectListOfBlogsAsMaps() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Map<String,Object>> blogs = mapper.selectBlogsAsMaps();
      assertEquals(2, blogs.size());
    }
  }

  @Test
  void shouldSelectListOfPostsLike() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Post> posts = mapper.selectPostsLike(new RowBounds(1,1),"%a%");
      assertEquals(1, posts.size());
    }
  }

  @Test
  void shouldSelectListOfPostsLikeTwoParameters() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Post> posts = mapper.selectPostsLikeSubjectAndBody(new RowBounds(1,1),"%a%","%a%");
      assertEquals(1, posts.size());
    }
  }

  @Test
  void shouldExecuteBoundSelectOneBlogStatement() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlog(1);
      assertEquals(1, blog.getId());
      assertEquals("Jim Business", blog.getTitle());
    }
  }

  @Test
  void shouldExecuteBoundSelectOneBlogStatementWithConstructor() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogUsingConstructor(1);
      assertEquals(1, blog.getId());
      assertEquals("Jim Business", blog.getTitle());
      assertNotNull(blog.getAuthor(), "author should not be null");
      List<Post> posts = blog.getPosts();
      assertTrue(posts != null && !posts.isEmpty(), "posts should not be empty");
    }
  }

  @Test
  void shouldExecuteBoundSelectBlogUsingConstructorWithResultMap() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogUsingConstructorWithResultMap(1);
      assertEquals(1, blog.getId());
      assertEquals("Jim Business", blog.getTitle());
      assertNotNull(blog.getAuthor(), "author should not be null");
      List<Post> posts = blog.getPosts();
      assertTrue(posts != null && !posts.isEmpty(), "posts should not be empty");
    }
  }

  @Test
  void shouldExecuteBoundSelectBlogUsingConstructorWithResultMapAndProperties() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogUsingConstructorWithResultMapAndProperties(1);
      assertEquals(1, blog.getId());
      assertEquals("Jim Business", blog.getTitle());
      assertNotNull(blog.getAuthor(), "author should not be null");
      Author author = blog.getAuthor();
      assertEquals(101, author.getId());
      assertEquals("jim@ibatis.apache.org", author.getEmail());
      assertEquals("jim", author.getUsername());
      assertEquals(Section.NEWS, author.getFavouriteSection());
      List<Post> posts = blog.getPosts();
      assertNotNull(posts, "posts should not be empty");
      assertEquals(2, posts.size());
    }
  }

  @Disabled
  @Test // issue #480 and #101
  void shouldExecuteBoundSelectBlogUsingConstructorWithResultMapCollection() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogUsingConstructorWithResultMapCollection(1);
      assertEquals(1, blog.getId());
      assertEquals("Jim Business", blog.getTitle());
      assertNotNull(blog.getAuthor(), "author should not be null");
      List<Post> posts = blog.getPosts();
      assertTrue(posts != null && !posts.isEmpty(), "posts should not be empty");
    }
  }

  @Test
  void shouldExecuteBoundSelectOneBlogStatementWithConstructorUsingXMLConfig() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogByIdUsingConstructor(1);
      assertEquals(1, blog.getId());
      assertEquals("Jim Business", blog.getTitle());
      assertNotNull(blog.getAuthor(), "author should not be null");
      List<Post> posts = blog.getPosts();
      assertTrue(posts != null && !posts.isEmpty(), "posts should not be empty");
    }
  }

  @Test
  void shouldSelectOneBlogAsMap() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Map<String,Object> blog = mapper.selectBlogAsMap(new HashMap<String, Object>() {
        {
          put("id", 1);
        }
      });
      assertEquals(1, blog.get("ID"));
      assertEquals("Jim Business", blog.get("TITLE"));
    }
  }

  @Test
  void shouldSelectOneAuthor() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = mapper.selectAuthor(101);
      assertEquals(101, author.getId());
      assertEquals("jim", author.getUsername());
      assertEquals("********", author.getPassword());
      assertEquals("jim@ibatis.apache.org", author.getEmail());
      assertEquals("", author.getBio());
    }
  }

  @Test
  void shouldSelectOneAuthorFromCache() {
    Author author1 = selectOneAuthor();
    Author author2 = selectOneAuthor();
    assertSame(author1, author2, "Same (cached) instance should be returned unless rollback is called.");
  }

  private Author selectOneAuthor() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      return mapper.selectAuthor(101);
    }
  }

  @Test
  void shouldSelectOneAuthorByConstructor() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = mapper.selectAuthorConstructor(101);
      assertEquals(101, author.getId());
      assertEquals("jim", author.getUsername());
      assertEquals("********", author.getPassword());
      assertEquals("jim@ibatis.apache.org", author.getEmail());
      assertEquals("", author.getBio());
    }
  }

  @Test
  void shouldSelectDraftTypedPosts() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Post> posts = mapper.selectPosts();
      assertEquals(5, posts.size());
      assertTrue(posts.get(0) instanceof DraftPost);
      assertFalse(posts.get(1) instanceof DraftPost);
      assertTrue(posts.get(2) instanceof DraftPost);
      assertFalse(posts.get(3) instanceof DraftPost);
      assertFalse(posts.get(4) instanceof DraftPost);
    }
  }

  @Test
  void shouldSelectDraftTypedPostsWithResultMap() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Post> posts = mapper.selectPostsWithResultMap();
      assertEquals(5, posts.size());
      assertTrue(posts.get(0) instanceof DraftPost);
      assertFalse(posts.get(1) instanceof DraftPost);
      assertTrue(posts.get(2) instanceof DraftPost);
      assertFalse(posts.get(3) instanceof DraftPost);
      assertFalse(posts.get(4) instanceof DraftPost);
    }
  }

  @Test
  void shouldReturnANotNullToString() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      assertNotNull(mapper.toString());
    }
  }

  @Test
  void shouldReturnANotNullHashCode() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      assertNotNull(mapper.hashCode());
    }
  }

  @Test
  void shouldCompareTwoMappers() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      BoundBlogMapper mapper2 = session.getMapper(BoundBlogMapper.class);
      assertNotEquals(mapper, mapper2);
    }
  }

  @Test
  void shouldFailWhenSelectingOneBlogWithNonExistentParam() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      assertThrows(Exception.class, () -> mapper.selectBlogByNonExistentParam(1));
    }
  }

  @Test
  void shouldFailWhenSelectingOneBlogWithNullParam() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      assertThrows(Exception.class, () -> mapper.selectBlogByNullParam(null));
    }
  }

  @Test // Decided that maps are dynamic so no existent params do not fail
  void shouldFailWhenSelectingOneBlogWithNonExistentNestedParam() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      mapper.selectBlogByNonExistentNestedParam(1, Collections.<String, Object>emptyMap());
    }
  }

  @Test
  void shouldSelectBlogWithDefault30ParamNames() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogByDefault30ParamNames(1, "Jim Business");
      assertNotNull(blog);
    }
  }

  @Test
  void shouldSelectBlogWithDefault31ParamNames() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogByDefault31ParamNames(1, "Jim Business");
      assertNotNull(blog);
    }
  }

  @Test
  void shouldSelectBlogWithAParamNamedValue() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Blog blog = mapper.selectBlogWithAParamNamedValue("id", 1, "Jim Business");
      assertNotNull(blog);
    }
  }

  @Test
  void shouldCacheMapperMethod() throws Exception {
    try (SqlSession session = sqlSessionFactory.openSession()) {

      // Create another mapper instance with a method cache we can test against:
      final MapperProxyFactory<BoundBlogMapper> mapperProxyFactory = new MapperProxyFactory<BoundBlogMapper>(BoundBlogMapper.class);
      assertEquals(BoundBlogMapper.class, mapperProxyFactory.getMapperInterface());
      final BoundBlogMapper mapper = mapperProxyFactory.newInstance(session);
      assertNotSame(mapper, mapperProxyFactory.newInstance(session));
      assertTrue(mapperProxyFactory.getMethodCache().isEmpty());

      // Mapper methods we will call later:
      final Method selectBlog = BoundBlogMapper.class.getMethod("selectBlog", Integer.TYPE);
      final Method selectBlogByIdUsingConstructor = BoundBlogMapper.class.getMethod("selectBlogByIdUsingConstructor", Integer.TYPE);

      // Call mapper method and verify it is cached:
      mapper.selectBlog(1);
      assertEquals(1, mapperProxyFactory.getMethodCache().size());
      assertTrue(mapperProxyFactory.getMethodCache().containsKey(selectBlog));
      final MapperMethod cachedSelectBlog = mapperProxyFactory.getMethodCache().get(selectBlog);

      // Call mapper method again and verify the cache is unchanged:
      session.clearCache();
      mapper.selectBlog(1);
      assertEquals(1, mapperProxyFactory.getMethodCache().size());
      assertSame(cachedSelectBlog, mapperProxyFactory.getMethodCache().get(selectBlog));

      // Call another mapper method and verify that it shows up in the cache as well:
      session.clearCache();
      mapper.selectBlogByIdUsingConstructor(1);
      assertEquals(2, mapperProxyFactory.getMethodCache().size());
      assertSame(cachedSelectBlog, mapperProxyFactory.getMethodCache().get(selectBlog));
      assertTrue(mapperProxyFactory.getMethodCache().containsKey(selectBlogByIdUsingConstructor));
    }
  }

  @Test
  void shouldGetBlogsWithAuthorsAndPosts() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Blog> blogs = mapper.selectBlogsWithAutorAndPosts();
      assertEquals(2, blogs.size());
      assertTrue(blogs.get(0) instanceof Proxy);
      assertEquals(101, blogs.get(0).getAuthor().getId());
      assertEquals(1, blogs.get(0).getPosts().size());
      assertEquals(1, blogs.get(0).getPosts().get(0).getId());
      assertTrue(blogs.get(1) instanceof Proxy);
      assertEquals(102, blogs.get(1).getAuthor().getId());
      assertEquals(1, blogs.get(1).getPosts().size());
      assertEquals(2, blogs.get(1).getPosts().get(0).getId());
    }
  }

  @Test
  void shouldGetBlogsWithAuthorsAndPostsEagerly() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      List<Blog> blogs = mapper.selectBlogsWithAutorAndPostsEagerly();
      assertEquals(2, blogs.size());
      assertFalse(blogs.get(0) instanceof Factory);
      assertEquals(101, blogs.get(0).getAuthor().getId());
      assertEquals(1, blogs.get(0).getPosts().size());
      assertEquals(1, blogs.get(0).getPosts().get(0).getId());
      assertFalse(blogs.get(1) instanceof Factory);
      assertEquals(102, blogs.get(1).getAuthor().getId());
      assertEquals(1, blogs.get(1).getPosts().size());
      assertEquals(2, blogs.get(1).getPosts().get(0).getId());
    }
  }

  @Test
  void executeWithResultHandlerAndRowBounds() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      final DefaultResultHandler handler = new DefaultResultHandler();
      mapper.collectRangeBlogs(handler, new RowBounds(1, 1));

      assertEquals(1, handler.getResultList().size());
      Blog blog = (Blog) handler.getResultList().get(0);
      assertEquals(2, blog.getId());
    }
  }

  @Test
  void executeWithMapKeyAndRowBounds() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      Map<Integer, Blog> blogs = mapper.selectRangeBlogsAsMapById(new RowBounds(1, 1));

      assertEquals(1, blogs.size());
      Blog blog = blogs.get(2);
      assertEquals(2, blog.getId());
    }
  }

  @Test
  void executeWithCursorAndRowBounds() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundBlogMapper mapper = session.getMapper(BoundBlogMapper.class);
      try (Cursor<Blog> blogs = mapper.openRangeBlogs(new RowBounds(1, 1)) ) {
        Iterator<Blog> blogIterator = blogs.iterator();
        Blog blog = blogIterator.next();
        assertEquals(2, blog.getId());
        assertFalse(blogIterator.hasNext());
      }
    } catch (IOException e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  void registeredMappers() {
    Collection<Class<?>> mapperClasses = sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers();
    assertEquals(2, mapperClasses.size());
    assertTrue(mapperClasses.contains(BoundBlogMapper.class));
    assertTrue(mapperClasses.contains(BoundAuthorMapper.class));
  }

  @Test
  void shouldMapPropertiesUsingRepeatableAnnotation() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
      mapper.insertAuthor(author);
      Author author2 = mapper.selectAuthorMapToPropertiesUsingRepeatable(author.getId());
      assertNotNull(author2);
      assertEquals(author.getId(), author2.getId());
      assertEquals(author.getUsername(), author2.getUsername());
      assertEquals(author.getPassword(), author2.getPassword());
      assertEquals(author.getBio(), author2.getBio());
      assertEquals(author.getEmail(), author2.getEmail());
      session.rollback();
    }
  }

  @Test
  void shouldMapConstructorUsingRepeatableAnnotation() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
      mapper.insertAuthor(author);
      Author author2 = mapper.selectAuthorMapToConstructorUsingRepeatable(author.getId());
      assertNotNull(author2);
      assertEquals(author.getId(), author2.getId());
      assertEquals(author.getUsername(), author2.getUsername());
      assertEquals(author.getPassword(), author2.getPassword());
      assertEquals(author.getBio(), author2.getBio());
      assertEquals(author.getEmail(), author2.getEmail());
      assertEquals(author.getFavouriteSection(), author2.getFavouriteSection());
      session.rollback();
    }
  }

  @Test
  void shouldMapUsingSingleRepeatableAnnotation() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
      mapper.insertAuthor(author);
      Author author2 = mapper.selectAuthorUsingSingleRepeatable(author.getId());
      assertNotNull(author2);
      assertEquals(author.getId(), author2.getId());
      assertEquals(author.getUsername(), author2.getUsername());
      assertNull(author2.getPassword());
      assertNull(author2.getBio());
      assertNull(author2.getEmail());
      assertNull(author2.getFavouriteSection());
      session.rollback();
    }
  }

  @Test
  void shouldMapWhenSpecifyBothArgAndConstructorArgs() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
      mapper.insertAuthor(author);
      Author author2 = mapper.selectAuthorUsingBothArgAndConstructorArgs(author.getId());
      assertNotNull(author2);
      assertEquals(author.getId(), author2.getId());
      assertEquals(author.getUsername(), author2.getUsername());
      assertEquals(author.getPassword(), author2.getPassword());
      assertEquals(author.getBio(), author2.getBio());
      assertEquals(author.getEmail(), author2.getEmail());
      assertEquals(author.getFavouriteSection(), author2.getFavouriteSection());
      session.rollback();
    }
  }

  @Test
  void shouldMapWhenSpecifyBothResultAndResults() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoundAuthorMapper mapper = session.getMapper(BoundAuthorMapper.class);
      Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
      mapper.insertAuthor(author);
      Author author2 = mapper.selectAuthorUsingBothResultAndResults(author.getId());
      assertNotNull(author2);
      assertEquals(author.getId(), author2.getId());
      assertEquals(author.getUsername(), author2.getUsername());
      assertNull(author2.getPassword());
      assertNull(author2.getBio());
      assertNull(author2.getEmail());
      assertNull(author2.getFavouriteSection());
      session.rollback();
    }
  }

}

