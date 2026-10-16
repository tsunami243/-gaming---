package org.example.bigevent.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Checks real rendered mapper SQL; does not claim database execution. */
class PublicArticleSqlBoundaryTest {
    private Configuration configuration() throws Exception {
        var configuration = new Configuration();
        String resource = "org/example/bigevent/mapper/ArticleMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
    @Test void publicStatementsRequireBothPublicationFields() throws Exception {
        var configuration = configuration();
        Map<String,Object> params = new HashMap<>();
        params.put("id",7); params.put("ids",List.of(7,8)); params.put("limit",5); params.put("tag","test");
        for (String key : List.of("state","categoryId","userId","gameCode","contentType")) params.put(key,null);
        for (String method : List.of("list","searchByTag","detailPublished","selectCarouselList",
                "selectPublishedByIds","selectHotIdsFromDb","selectAllHeat","listPublishedByFilters")) {
            String sql = configuration.getMappedStatement(ArticleMapper.class.getName()+"."+method)
                    .getBoundSql(params).getSql().replaceAll("\\s+", "");
            assertTrue(sql.contains("state='已发布'"),method+": "+sql);
            assertTrue(sql.contains("workflow_status='PUBLISHED'"),method+": "+sql);
        }
    }
    @Test void draftParameterCannotRemovePublicWorkflowBoundary() throws Exception {
        var params = new HashMap<String,Object>();
        params.put("state","草稿"); params.put("categoryId",null); params.put("userId",null);
        String sql = configuration().getMappedStatement(ArticleMapper.class.getName()+".list")
                .getBoundSql(params).getSql().replaceAll("\\s+", "");
        assertTrue(sql.contains("state='已发布'andworkflow_status='PUBLISHED'"));
    }
}
