package denominator.route53;

import denominator.Live.UseTestGraph;
import denominator.profile.WeightedReadOnlyLiveTest;

@UseTestGraph(Route53TestGraph.class)
public class Route53WeightedReadOnlyLiveTest extends WeightedReadOnlyLiveTest {

}
