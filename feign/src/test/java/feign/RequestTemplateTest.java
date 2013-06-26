package feign;

import static feign.RequestTemplate.expand;
import static javax.ws.rs.HttpMethod.GET;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

import feign.RequestTemplate;

public class RequestTemplateTest {
    @Test
    public void expandNotUrlEncoded() {
        for (String val : ImmutableList.of("apples", "sp ace", "unic???de", "qu?stion"))
            assertEquals(expand("/users/{user}", ImmutableMap.of("user", val)), "/users/" + val);
    }

    @Test
    public void expandMultipleParams() {
        assertEquals(expand("/users/{user}/{repo}", ImmutableMap.of("user", "unic???de", "repo", "foo")),
                "/users/unic???de/foo");
    }

    @Test
    public void expandParamKeyHyphen() {
        assertEquals(expand("/{user-dir}", ImmutableMap.of("user-dir", "foo")), "/foo");
    }

    @Test
    public void expandMissingParamProceeds() {
        assertEquals(expand("/{user-dir}", ImmutableMap.of("user_dir", "foo")), "/{user-dir}");
    }

    @Test
    public void resolveTemplateWithParameterizedPathSkipsEncodingSlash() {

        RequestTemplate template = new RequestTemplate().method(GET)
                .append("{zoneId}");

        assertEquals(template.toString(), ""//
                + "GET {zoneId} HTTP/1.1\n");

        template.resolve(ImmutableMap.of("zoneId", "/hostedzone/Z1PA6795UKMFR9"));

        assertEquals(template.toString(), ""//
                + "GET /hostedzone/Z1PA6795UKMFR9 HTTP/1.1\n");

        template.insert(0, "https://route53.amazonaws.com/2012-12-12");

        assertEquals(template.request().toString(), ""//
                + "GET https://route53.amazonaws.com/2012-12-12/hostedzone/Z1PA6795UKMFR9 HTTP/1.1\n");
    }

    @Test
    public void resolveTemplateWithBaseAndParameterizedQuery() {
        RequestTemplate template = new RequestTemplate().method(GET)
                .append("/?Action=DescribeRegions").query("RegionName.1", "{region}");

        assertEquals(template.queries(),
                ImmutableListMultimap.of("Action", "DescribeRegions", "RegionName.1", "{region}"));
        assertEquals(template.toString(), ""//
                + "GET /?Action=DescribeRegions&RegionName.1={region} HTTP/1.1\n");

        template.resolve(ImmutableMap.of("region", "eu-west-1"));
        assertEquals(template.queries(),
                ImmutableListMultimap.of("Action", "DescribeRegions", "RegionName.1", "eu-west-1"));

        assertEquals(template.toString(), ""//
                + "GET /?Action=DescribeRegions&RegionName.1=eu-west-1 HTTP/1.1\n");

        template.insert(0, "https://iam.amazonaws.com");

        assertEquals(template.request().toString(), ""//
                + "GET https://iam.amazonaws.com/?Action=DescribeRegions&RegionName.1=eu-west-1 HTTP/1.1\n");
    }
}
