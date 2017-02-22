package denominator.ultradns.model;

import java.util.ArrayList;
import java.util.List;

public class RRSet {

    private String zoneName;
    private String ownerName;
    private String rrtype;
    private Integer ttl;
    private List<String> rdata = new ArrayList<String>();

    public RRSet(){}

    public RRSet(Integer ttl, List<String> rdata) {
        this.ttl = ttl;
        this.rdata = rdata;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getRrtype() {
        return rrtype;
    }

    public void setRrtype(String rrtype) {
        this.rrtype = rrtype;
    }

    public int getIntValueOfRRtype() {
        return Integer.parseInt(rrtype.substring(rrtype.indexOf("(") + 1,
                rrtype.indexOf(")")));
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public List<String> getRdata() {
        return rdata;
    }

    public void setRdata(List<String> rdata) {
        this.rdata = rdata;
    }
}
