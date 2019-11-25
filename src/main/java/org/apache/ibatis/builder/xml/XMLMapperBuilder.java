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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLMapperBuilder extends BaseBuilder {

  private final XPathParser parser;
  private final MapperBuilderAssistant builderAssistant;
  private final Map<String, XNode> sqlFragments;
  private final String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    super(configuration);
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  public void parse() {
    if (!configuration.isResourceLoaded(resource)) {// 若是没有加载resource org/apache/ibatis/binding/BoundAuthorMapper.xml
      configurationElement(parser.evalNode("/mapper"));// 解析以mapper作为标签的节点，将mapper的孩子节点进行处理，设置在configuration对应的属性字段中
      configuration.addLoadedResource(resource);// 将该路径设置到loadedResource的字段中
      bindMapperForNamespace();// 对命名空间对应的xml文件进行解析
    }
    // 解析待处理的结果集map
    parsePendingResultMaps();
    // 解析待处理的CacheRefs
    parsePendingCacheRefs();
    // 解析待处理的声明
    parsePendingStatements();
  }

  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  private void configurationElement(XNode context) {
    // context的内容如下（也就是将整个xml文件都加载到XNode上了）：
    // <mapper namespace="org.apache.ibatis.binding.BoundBlogMapper">
    //    <select resultType="int" id="selectRandom">
    //    select CAST(RANDOM()*1000000 as INTEGER) a from SYSIBM.SYSDUMMY1
    //  </select>
    //    <select resultType="org.apache.ibatis.domain.blog.Blog" id="selectBlogsFromXML">
    //    SELECT * FROM blog
    //  </select>
    //    <resultMap type="Blog" id="blogWithPosts">
    //        <id column="id" property="id"/>
    //        <result column="title" property="title"/>
    //        <association select="selectAuthorWithInlineParams" column="author_id" property="author"/>
    //        <collection select="selectPostsForBlog" column="id" property="posts"/>
    //    </resultMap>
    //    <resultMap type="Blog" id="blogUsingConstructor">
    //        <constructor>
    //            <idArg column="id" javaType="_int"/>
    //            <arg column="title" javaType="java.lang.String"/>
    //            <arg select="org.apache.ibatis.binding.BoundAuthorMapper.selectAuthor" column="author_id" javaType="org.apache.ibatis.domain.blog.Author"/>
    //            <arg select="selectPostsForBlog" column="id" javaType="java.util.List"/>
    //        </constructor>
    //    </resultMap>
    //    <resultMap type="Blog" id="blogUsingConstructorWithResultMap">
    //        <constructor>
    //            <idArg column="id" javaType="_int"/>
    //            <arg column="title" javaType="java.lang.String"/>
    //            <arg resultMap="org.apache.ibatis.binding.BoundAuthorMapper.authorResultMap" javaType="org.apache.ibatis.domain.blog.Author"/>
    //            <arg select="selectPostsForBlog" column="id" javaType="java.util.List"/>
    //        </constructor>
    //    </resultMap>
    //    <resultMap type="Blog" id="blogUsingConstructorWithResultMapAndProperties">
    //        <constructor>
    //            <idArg column="id" javaType="_int"/>
    //            <arg column="title" javaType="java.lang.String"/>
    //            <arg resultMap="org.apache.ibatis.binding.BoundAuthorMapper.authorResultMapWithProperties" javaType="org.apache.ibatis.domain.blog.Author"/>
    //            <arg select="selectPostsForBlog" column="id" javaType="java.util.List"/>
    //        </constructor>
    //    </resultMap>
    //    <resultMap type="Blog" id="blogUsingConstructorWithResultMapCollection">
    //        <constructor>
    //            <idArg column="id" javaType="_int"/>
    //            <arg column="title" javaType="java.lang.String"/>
    //            <arg resultMap="org.apache.ibatis.binding.BoundAuthorMapper.authorResultMap" javaType="org.apache.ibatis.domain.blog.Author"/>
    //            <arg resultMap="blogWithPosts" javaType="java.util.List"/>
    //        </constructor>
    //    </resultMap>
    //    <select resultMap="blogWithPosts" parameterType="int" id="selectBlogWithPostsUsingSubSelect">
    //    select * from Blog where id = #{id}
    //  </select>
    //    <select resultType="org.apache.ibatis.domain.blog.Author" parameterType="int" id="selectAuthorWithInlineParams">
    //    select * from author where id = #{id}
    //  </select>
    //    <select resultType="Post" parameterType="int" id="selectPostsForBlog">
    //    select * from Post where blog_id = #{blog_id}
    //  </select>
    //    <select resultMap="blogUsingConstructor" parameterType="int" id="selectBlogByIdUsingConstructor">
    //    select * from Blog where id = #{id}
    //  </select>
    //    <select resultMap="blogUsingConstructorWithResultMap" parameterType="int" id="selectBlogUsingConstructorWithResultMap">
    //      select b.*,
    //      	a.id as author_id,
    //      	a.username as author_username,
    //      	a.password as author_password,
    //      	a.email as author_email,
    //      	a.bio as author_bio,
    //      	a.favourite_section
    //      from Blog b join Author a
    //      on b.author_id = a.id
    //      where b.id = #{id}
    //    </select>
    //    <select resultMap="blogUsingConstructorWithResultMapAndProperties" parameterType="int" id="selectBlogUsingConstructorWithResultMapAndProperties">
    //      select b.*,
    //      	a.id as author_id,
    //      	a.username as author_username,
    //      	a.password as author_password,
    //      	a.email as author_email,
    //      	a.bio as author_bio,
    //      	a.favourite_section
    //      from Blog b join Author a
    //      on b.author_id = a.id
    //      where b.id = #{id}
    //    </select>
    //    <select resultMap="blogUsingConstructorWithResultMapCollection" parameterType="int" id="selectBlogUsingConstructorWithResultMapCollection">
    //      select b.*, p.*,
    //      	a.id as author_id,
    //      	a.username as author_username,
    //      	a.password as author_password,
    //      	a.email as author_email,
    //      	a.bio as author_bio,
    //      	a.favourite_section
    //      from Blog b
    //          join Author a on b.author_id = a.id
    //          join Post p on b.id = p.blog_id
    //      where b.id = #{id}
    //    </select>
    //</mapper>
    try {
      String namespace = context.getStringAttribute("namespace");// org.apache.ibatis.binding.BoundBlogMapper
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      builderAssistant.setCurrentNamespace(namespace);
      // 下面的5个方法都调用到了context.evalNode()方法，用于获取对应的xml文件中的相应XNode节点
      cacheRefElement(context.evalNode("cache-ref"));// 该文件没有设置cache-ref
      cacheElement(context.evalNode("cache"));// 该文件没有设置cache
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      // 以该方法为例，解析mapper下的以resultMap为标签的XNode节点，该代码没有返回数据，但是深入进去可以发现，设置到了builderAssistant.configure里的resultMap属性了
      // resultMaps = {Configuration$StrictMap@3575}  size = 10
      // 0 = {HashMap$Node@3591} "blogUsingConstructor" ->
      // 1 = {HashMap$Node@3592} "org.apache.ibatis.binding.BoundBlogMapper.blogUsingConstructorWithResultMap" ->
      // 2 = {HashMap$Node@3593} "org.apache.ibatis.binding.BoundBlogMapper.blogUsingConstructorWithResultMapCollection" ->
      // 3 = {HashMap$Node@3594} "org.apache.ibatis.binding.BoundBlogMapper.blogUsingConstructor" ->
      // 4 = {HashMap$Node@3595} "org.apache.ibatis.binding.BoundBlogMapper.blogWithPosts" ->
      // 5 = {HashMap$Node@3596} "blogUsingConstructorWithResultMap" ->
      // 6 = {HashMap$Node@3597} "blogWithPosts" ->
      // 7 = {HashMap$Node@3598} "blogUsingConstructorWithResultMapAndProperties" ->
      // 8 = {HashMap$Node@3599} "blogUsingConstructorWithResultMapCollection" ->
      // 9 = {HashMap$Node@3600} "org.apache.ibatis.binding.BoundBlogMapper.blogUsingConstructorWithResultMapAndProperties" ->
      // 其中value值没有展开，从中可以看出，全限定名和简称都复制了一遍，且value值是相同的。
      // resultMapElements的解析方法就不去看了，都是解析里面有什么属性，并且属性的值等，最后就是放在resultMap中。
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      sqlElement(context.evalNodes("/mapper/sql"));

      // 该方法则是解析xml文件中含select等标签的节点
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  private void buildStatementFromContext(List<XNode> list) {
    // 直接将所有select|insert|update|delete的节点传进来
    // 0 = {XNode@3584} "<select resultType="int" id="selectRandom">\n    select CAST(RANDOM()*1000000 as INTEGER) a from SYSIBM.SYSDUMMY1\n  </select>\n"
    //1 = {XNode@3585} "<select resultType="org.apache.ibatis.domain.blog.Blog" id="selectBlogsFromXML">\n    SELECT * FROM blog\n  </select>\n"
    //2 = {XNode@3586} "<select resultMap="blogWithPosts" parameterType="int" id="selectBlogWithPostsUsingSubSelect">\n    select * from Blog where id = #{id}\n  </select>\n"
    //3 = {XNode@3587} "<select resultType="org.apache.ibatis.domain.blog.Author" parameterType="int" id="selectAuthorWithInlineParams">\n    select * from author where id = #{id}\n  </select>\n"
    //4 = {XNode@3588} "<select resultType="Post" parameterType="int" id="selectPostsForBlog">\n    select * from Post where blog_id = #{blog_id}\n  </select>\n"
    //5 = {XNode@3589} "<select resultMap="blogUsingConstructor" parameterType="int" id="selectBlogByIdUsingConstructor">\n    select * from Blog where id = #{id}\n  </select>\n"
    //6 = {XNode@3590} "<select resultMap="blogUsingConstructorWithResultMap" parameterType="int" id="selectBlogUsingConstructorWithResultMap">\n      select b.*,\n      \ta.id as author_id,\n      \ta.username as author_username,\n      \ta.password as author_password,\n      \ta.email as author_email,\n      \ta.bio as author_bio,\n      \ta.favourite_section\n      from Blog b join Author a\n      on b.author_id = a.id\n      where b.id = #{id}\n    </select>\n"
    //7 = {XNode@3591} "<select resultMap="blogUsingConstructorWithResultMapAndProperties" parameterType="int" id="selectBlogUsingConstructorWithResultMapAndProperties">\n      select b.*,\n      \ta.id as author_id,\n      \ta.username as author_username,\n      \ta.password as author_password,\n      \ta.email as author_email,\n      \ta.bio as author_bio,\n      \ta.favourite_section\n      from Blog b join Author a\n      on b.author_id = a.id\n      where b.id = #{id}\n    </select>\n"
    //8 = {XNode@3592} "<select resultMap="blogUsingConstructorWithResultMapCollection" parameterType="int" id="selectBlogUsingConstructorWithResultMapCollection">\n      select b.*, p.*,\n      \ta.id as author_id,\n      \ta.username as author_username,\n      \ta.password as author_password,\n      \ta.email as author_email,\n      \ta.bio as author_bio,\n      \ta.favourite_section\n      from Blog b\n          join Author a on b.author_id = a.id\n          join Post p on b.id = p.blog_id\n      where b.id = #{id}\n    </select>\n"
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    // XMLStatementBuilder的parseStatementNode方法相当重要，解析select中的属性。最终放在了builderAssistant的configuration的mapperStatement中，如下:
    // mappedStatements = {Configuration$StrictMap@3645}  size = 18
    // 0 = {HashMap$Node@3663} "selectAuthorWithInlineParams" ->
    // 1 = {HashMap$Node@3664} "org.apache.ibatis.binding.BoundBlogMapper.selectBlogUsingConstructorWithResultMapAndProperties" ->
    // 2 = {HashMap$Node@3665} "selectBlogUsingConstructorWithResultMapCollection" ->
    // 3 = {HashMap$Node@3666} "org.apache.ibatis.binding.BoundBlogMapper.selectRandom" ->
    // 4 = {HashMap$Node@3667} "org.apache.ibatis.binding.BoundBlogMapper.selectBlogWithPostsUsingSubSelect" ->
    // 5 = {HashMap$Node@3668} "org.apache.ibatis.binding.BoundBlogMapper.selectBlogUsingConstructorWithResultMap" ->
    // 6 = {HashMap$Node@3669} "org.apache.ibatis.binding.BoundBlogMapper.selectBlogsFromXML" ->
    // 7 = {HashMap$Node@3670} "selectPostsForBlog" ->
    // 8 = {HashMap$Node@3671} "selectBlogWithPostsUsingSubSelect" ->
    // 9 = {HashMap$Node@3672} "org.apache.ibatis.binding.BoundBlogMapper.selectPostsForBlog" ->
    // 10 = {HashMap$Node@3673} "selectBlogUsingConstructorWithResultMapAndProperties" ->
    // 11 = {HashMap$Node@3674} "org.apache.ibatis.binding.BoundBlogMapper.selectAuthorWithInlineParams" ->
    // 12 = {HashMap$Node@3675} "org.apache.ibatis.binding.BoundBlogMapper.selectBlogByIdUsingConstructor" ->
    // 13 = {HashMap$Node@3676} "org.apache.ibatis.binding.BoundBlogMapper.selectBlogUsingConstructorWithResultMapCollection" ->
    // 14 = {HashMap$Node@3677} "selectRandom" ->
    // 15 = {HashMap$Node@3678} "selectBlogByIdUsingConstructor" ->
    // 16 = {HashMap$Node@3679} "selectBlogUsingConstructorWithResultMap" ->
    // 17 = {HashMap$Node@3680} "selectBlogsFromXML" ->
    buildStatementFromContext(list, null);
  }

  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  private void parsePendingResultMaps() {
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  private void parsePendingCacheRefs() {
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolveCacheRef();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Cache ref is still missing a resource...
        }
      }
    }
  }

  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Statement is still missing a resource...
        }
      }
    }
  }

  private void cacheRefElement(XNode context) {
    if (context != null) {
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  private void cacheElement(XNode context) {
    if (context != null) {
      String type = context.getStringAttribute("type", "PERPETUAL");
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      Properties props = context.getChildrenAsProperties();
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  private void parameterMapElement(List<XNode> list) {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  private void resultMapElements(List<XNode> list) throws Exception {
    for (XNode resultMapNode : list) {
      try {
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
      }
    }
  }

  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.emptyList(), null);
  }

  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings, Class<?> enclosingType) throws Exception {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    Class<?> typeClass = resolveClass(type);
    if (typeClass == null) {
      typeClass = inheritEnclosingType(resultMapNode, enclosingType);
    }
    Discriminator discriminator = null;
    List<ResultMapping> resultMappings = new ArrayList<>();
    resultMappings.addAll(additionalResultMappings);
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      if ("constructor".equals(resultChild.getName())) {
        processConstructorElement(resultChild, typeClass, resultMappings);
      } else if ("discriminator".equals(resultChild.getName())) {
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    String id = resultMapNode.getStringAttribute("id",
            resultMapNode.getValueBasedIdentifier());
    String extend = resultMapNode.getStringAttribute("extends");
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
    if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      String property = resultMapNode.getStringAttribute("property");
      if (property != null && enclosingType != null) {
        MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
        return metaResultType.getSetterType(property);
      }
    } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      return enclosingType;
    }
    return null;
  }

  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    Map<String, String> discriminatorMap = new HashMap<>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings, resultType));
      discriminatorMap.put(value, resultMap);
    }
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  private void sqlElement(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }

  private void sqlElement(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      id = builderAssistant.applyCurrentNamespace(id, false);
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        sqlFragments.put(id, context);
      }
    }
  }

  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      return requiredDatabaseId.equals(databaseId);
    }
    if (databaseId != null) {
      return false;
    }
    if (!this.sqlFragments.containsKey(id)) {
      return true;
    }
    // skip this fragment if there is a previous one with a not null databaseId
    XNode context = this.sqlFragments.get(id);
    return context.getStringAttribute("databaseId") == null;
  }

  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.emptyList(), resultType));
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resultSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings, Class<?> enclosingType) throws Exception {
    if ("association".equals(context.getName())
        || "collection".equals(context.getName())
        || "case".equals(context.getName())) {
      if (context.getStringAttribute("select") == null) {
        validateCollection(context, enclosingType);
        ResultMap resultMap = resultMapElement(context, resultMappings, enclosingType);
        return resultMap.getId();
      }
    }
    return null;
  }

  protected void validateCollection(XNode context, Class<?> enclosingType) {
    if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
        && context.getStringAttribute("javaType") == null) {
      MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
      String property = context.getStringAttribute("property");
      if (!metaResultType.hasSetter(property)) {
        throw new BuilderException(
          "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
      }
    }
  }

  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace(); // 获取当前命名空间
    if (namespace != null) {
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace); // 通过命名空间加载该类
      } catch (ClassNotFoundException e) {
        //ignore, bound type is not required
      }
      if (boundType != null) { // 若该类不为null
        if (!configuration.hasMapper(boundType)) { //
          // Spring may not know the real resource name so we set a flag
          // to prevent loading again this resource from the mapper interface
          // look at MapperAnnotationBuilder#loadXmlResource
          configuration.addLoadedResource("namespace:" + namespace);// 将当前命名空间设置到configuration的loadedResource的map中
          configuration.addMapper(boundType);// 将boundType设置到configuration中的mapperRegistry的knownMappers中，并对加载的文件进行解析
        }
      }
    }
  }

}
