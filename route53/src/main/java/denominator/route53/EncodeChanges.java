package denominator.route53;

import java.util.List;

import javax.inject.Inject;

import denominator.route53.Route53.ActionOnResourceRecordSet;
import feign.codec.EncodeException;
import feign.codec.Encoder;

class EncodeChanges implements Encoder.Text<List<ActionOnResourceRecordSet>> {
    @Inject
    EncodeChanges(){
    }

    @Override
    public String encode(List<ActionOnResourceRecordSet> actions) throws EncodeException {
        StringBuilder b = new StringBuilder();
        b.append("<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch>");
        b.append("<Changes>");
        for (ActionOnResourceRecordSet change : actions)
            b.append("<Change>").append("<Action>").append(change.action).append("</Action>")
                    .append(SerializeRRS.INSTANCE.apply(change.rrs)).append("</Change>");
        b.append("</Changes>");
        b.append("</ChangeBatch></ChangeResourceRecordSetsRequest>");
        return b.toString();
    }
}
