package denominator.route53;

import java.util.List;

import denominator.route53.Route53.ActionOnResourceRecordSet;
import feign.RequestTemplate;
import feign.codec.BodyEncoder;

class EncodeChanges implements BodyEncoder {

    @Override
    public void encodeBody(Object bodyParam, RequestTemplate base) {
        @SuppressWarnings("unchecked")
        List<ActionOnResourceRecordSet> actions = List.class.cast(bodyParam);
        StringBuilder b = new StringBuilder();
        b.append("<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch>");
        b.append("<Changes>");
        for (ActionOnResourceRecordSet change : actions)
            b.append("<Change>").append("<Action>").append(change.action).append("</Action>")
                    .append(SerializeRRS.INSTANCE.apply(change.rrs)).append("</Change>");
        b.append("</Changes>");
        b.append("</ChangeBatch></ChangeResourceRecordSetsRequest>");
        base.body(b.toString());
    }
}
