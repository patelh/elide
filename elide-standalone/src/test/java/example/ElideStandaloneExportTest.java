/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static io.restassured.RestAssured.when;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneAsyncSettings;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneExportTest {
    protected ElideStandalone elide;

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneTestSettings() {

            @Override
            public ElideStandaloneAsyncSettings getAsyncProperties() {
                ElideStandaloneAsyncSettings asyncPropeties = new ElideStandaloneAsyncSettings() {
                    @Override
                    public boolean enabled() {
                        return true;
                    }

                    @Override
                    public boolean enableCleanup() {
                        return true;
                    }

                    @Override
                    public Integer getThreadSize() {
                        return 5;
                    }

                    @Override
                    public Integer getMaxRunTimeSeconds() {
                        return 1800;
                    }

                    @Override
                    public Integer getQueryCleanupDays() {
                        return 3;
                    }

                    @Override
                    public boolean enableExport() {
                        return true;
                    }
                };
                return asyncPropeties;
            }

            @Override
            public ElideStandaloneAnalyticSettings getAnalyticProperties() {
                ElideStandaloneAnalyticSettings analyticPropeties = new ElideStandaloneAnalyticSettings() {
                    @Override
                    public boolean enableDynamicModelConfig() {
                        return true;
                    }

                    @Override
                    public boolean enableAggregationDataStore() {
                        return true;
                    }

                    @Override
                    public boolean enableMetaDataStore() {
                        return true;
                    }

                    @Override
                    public String getDefaultDialect() {
                        return SQLDialectFactory.getDefaultDialect().getDialectType();
                    }

                    @Override
                    public String getDynamicConfigPath() {
                        return "src/test/resources/configs/";
                    }
                };
                return analyticPropeties;
            }
        });
        elide.start(false);
    }

    @AfterAll
    public void shutdown() throws Exception {
        elide.stop();
    }

    @Test
    public void exportNotFound() throws InterruptedException {
        int queryId = 1;
        String output = when()
                .get("/export/" + queryId)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .extract().body().asString();

        // Special Message generated by Export API
        assertEquals(true, output.contains(queryId + " Not Found"));
    }

// TODO Uncomment once AsyncExecutorService Singleton is removed. https://github.com/yahoo/elide/issues/1798
/*
    @Test
    public void testExportDynamicModel() throws InterruptedException {
        // Load Test data in Post Table
        given()
        .contentType(JSONAPI_CONTENT_TYPE)
        .accept(JSONAPI_CONTENT_TYPE)
        .body(
            datum(
                resource(
                    type("post"),
                    id("1"),
                    attributes(
                        attr("content", "This is my first post. woot."),
                        attr("date", "2019-01-01T00:00Z")
                    )
                )
            )
        )
        .post("/api/v1/post")
        .then()

        .statusCode(HttpStatus.SC_CREATED)
        .extract().body().asString();

        //Create Table Export
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"),
                                        attributes(
                                                attr("query", "{\"query\":\"{postView{ edges{node{content}}}}\",\"variables\":null}"),
                                                attr("queryType", "GRAPHQL_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/api/v1/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/api/v1/tableExport/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d");
            String outputResponse = response.jsonPath().getString("data.attributes.status");
             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {
                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql/api/v1")
                        .asString();
                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\","
                        + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"CSV\","
                        + "\"result\":{\"url\":\"https://elide.io/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";
                assertEquals(expectedResponse, responseGraphQL);
                break;
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Async Query has failed.");
                break;
            }
        }
        when()
                .get("/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }*/
}