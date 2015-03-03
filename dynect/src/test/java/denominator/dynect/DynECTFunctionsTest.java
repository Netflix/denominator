package denominator.dynect;

import com.google.gson.JsonParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Map;

import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class DynECTFunctionsTest {

  @Parameterized.Parameter(0)
  public String type;
  @Parameterized.Parameter(1)
  public String inputJson;
  @Parameterized.Parameter(2)
  public Map<String, Object> expectedResult;

  @Test
  public void toRecord() {
    assertThat(ToRecord.toRData(type, new JsonParser().parse(inputJson).getAsJsonObject()))
        .isInstanceOf(expectedResult.getClass())
        .isEqualTo(expectedResult);
  }

  @Parameters
  public static Object[][] createData() {
    return new Object[][]{
        {
            "SOA",
            "{\n"
            + "  \"rname\": \"admin.denominator.io.\",\n"
            + "  \"retry\": 600,\n"
            + "  \"mname\": \"ns1.p28.dynect.net.\",\n"
            + "  \"minimum\": 60,\n"
            + "  \"refresh\": 3600,\n"
            + "  \"expire\": 604800,\n"
            + "  \"serial\": 43\n"
            + "}",
            SOAData.builder()
                .rname("admin.denominator.io.")
                .retry(600)
                .mname("ns1.p28.dynect.net.")
                .minimum(60)
                .refresh(3600)
                .expire(604800)
                .serial(43)
                .build()
        },
        {
            "NS",
            "{\n"
            + "  \"nsdname\": \"ns1.p28.dynect.net.\"\n"
            + "}",
            NSData.create("ns1.p28.dynect.net.")
        },
        {
            "A",
            "{\n"
            + "  \"address\": \"192.0.2.1\"\n"
            + "}",
            AData.create("192.0.2.1")
        },
        {
            "AAAA",
            "{\n"
            + "  \"address\": \"2001:db8::3\"\n"
            + "}",
            AAAAData.create("2001:db8::3")
        },
        {
            "CNAME",
            "{\n"
            + "  \"cname\": \"denominator.github.com\"\n"
            + "}",
            CNAMEData.create("denominator.github.com")
        },
        {
            "SPF",
            "{\n"
            + "  \"txtdata\": \"sample SPF text data\"\n"
            + "}",
            SPFData.create("sample SPF text data")
        },
        {
            "TXT",
            "{\n"
            + "  \"txtdata\": \"sample TXT text data\"\n"
            + "}",
            TXTData.create("sample TXT text data")
        },
        {
            "MX",
            "{\n"
            + "  \"preference\": 5,\n"
            + "  \"exchange\": \"helloExchange\"\n"
            + "}",
            MXData.create(5, "helloExchange")
        },
        {
            "PTR",
            "{\n"
            + "  \"ptrdname\": \"hello.ptrdname\"\n"
            + "}",
            PTRData.create("hello.ptrdname")
        },
        {
            "SRV",
            "{\n"
            + "  \"priority\": 1,\n"
            + "  \"weight\": 2,\n"
            + "  \"port\": 80,\n"
            + "  \"target\": \"helloTarget\"\n"
            + "}",
            SRVData.builder()
                .priority(1)
                .weight(2)
                .port(80)
                .target("helloTarget")
                .build()
        }
    };
  }
}

